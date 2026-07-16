package net.nicovrc.dev;

import net.nicovrc.dev.api.NicoVRCAPI;
import net.nicovrc.dev.data.HttpHeader;
import net.nicovrc.dev.http.GetURL;
import net.nicovrc.dev.http.GetVideo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Nio2EchoServer {

    private final AsynchronousServerSocketChannel asyncChannel;
    private final HttpClient client;

    private final GetURL getURL = new GetURL();
    private final GetVideo getVideo = new GetVideo();

    private final static Pattern matcher_uri = Pattern.compile("(url=|vi=|dummy=|dummy\\.m3u8|/proxy)");
    private final static Pattern matcher_http_range1 = Pattern.compile("[r|R]ange: bytes=(\\d+)-(\\d+)");
    private final static Pattern matcher_http_range2 = Pattern.compile("[r|R]ange: bytes=(\\d+)-");


    public Nio2EchoServer(HttpClient client) {
        try {
            this.asyncChannel = AsynchronousServerSocketChannel.open();
            this.client = client;

            getURL.setHTTPClient(client);
            getURL.setProxy(null);
            getVideo.setHTTPClient(client);
            getVideo.setProxy(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(int port) {
        try {
            asyncChannel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        asyncChannel.accept(null, acceptHandler);
    }

    private final CompletionHandler<AsynchronousSocketChannel, Void> acceptHandler = new CompletionHandler<>() {

        @Override
        public void completed(AsynchronousSocketChannel asyncSocketChannel, Void attachment) {

            // accept the next connection
            asyncChannel.accept(null, this);

            // handle this connection
            Context ctx = new Context(asyncSocketChannel, ByteBuffer.allocate(1024));
            asyncSocketChannel.read(ctx.buffer, ctx, requestHandler);
        }

        @Override
        public void failed(Throwable e, Void attachment) {
            e.printStackTrace();
        }
    };

    private final CompletionHandler<Integer, Context> requestHandler = new CompletionHandler<>() {
        @Override
        public void completed(Integer result, Context ctx) {
            if (result == -1) {
                ctx.close();
                return;
            }
            //System.out.println("read " + result + " bytes.");
            ctx.buffer.flip();

            final String httpRequest = Function.getHTTPRequest(ctx.buffer);

            //System.out.println(httpRequest);

            if (httpRequest.isEmpty()) {
                ctx.close();
                return;
            }

            final AsynchronousSocketChannel ch = ctx.asyncSocketChannel;

            final String httpVersion = Function.getHTTPVersion(httpRequest);
            final String httpMethod = Function.getMethod(httpRequest);

            final boolean isGET = httpMethod != null && httpMethod.equals("GET");
            final boolean isPOST = httpMethod != null && httpMethod.equals("POST");
            final boolean isHead = httpMethod != null && httpMethod.equals("HEAD");

            if (!isGET && !isPOST && !isHead) {
                //System.out.println("[Debug] HTTPRequest送信");
                Function.sendHttpData(ch, new HttpHeader(httpVersion, 405, Function.contentType_textPlain, null, "*", Function.content_MethodNotAllowed, null));
                return;
            }
            //System.out.println("debug0");

            final String URI = Function.getURI(httpRequest);
            if (URI == null) {
                //System.out.println("[Debug] HTTPRequest送信");
                Function.sendHttpData(ch, new HttpHeader(httpVersion, 502, Function.contentType_textPlain, null, "*", Function.content_BadGateway, null));
                //System.out.println(httpRequest);
                return;
            }

            final Matcher matcher = matcher_uri.matcher(URI);
            final boolean ApiMatchFlag = URI.startsWith("/api/");
            final boolean UrlMatchFlag = matcher.find();
            final boolean VideoMatchFlag = URI.startsWith("/video");

            Matcher matcher_range1 = matcher_http_range1.matcher(httpRequest);
            Matcher matcher_range2 = matcher_http_range2.matcher(httpRequest);
            final boolean RangeVideoFlag = matcher_range1.find();
            final boolean RangeVideoFullFlag = matcher_range2.find();

            if (ApiMatchFlag) {
                byte[] httpBody = null;
                for (NicoVRCAPI api : Function.APIList) {
                    if (URI.startsWith(api.getURI())) {
                        try {
                            httpBody = api.Run(httpRequest, client).getBytes(StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_json, null, "*", httpBody, null));
                        System.out.println("[API (" + Function.sdf.format(new Date()) + ")] " + URI + " ---> OK");
                        Function.APIAccessLog.put(UUID.randomUUID().toString(), httpRequest);
                        break;
                    }
                }

                if (httpBody == null) {
                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 404, Function.contentType_textPlain, null, "*", Function.content_errorAPINotFound, null));
                    System.out.println("[API (" + Function.sdf.format(new Date()) + ")] " + URI + " ---> Error");
                    Function.APIAccessLog.put(UUID.randomUUID().toString(), httpRequest);
                }

                ctx.close();
                return;
            }

            System.out.println(URI);
            if (VideoMatchFlag || (RangeVideoFlag && !matcher_range1.group(1).equals("0"))) {
                //System.out.println("debug1");
                getVideo.setHTTPRequest(httpRequest);
                getVideo.setURL(URI);
                getVideo.setHTTPSocket(ch);

                getVideo.run();
                ctx.close();
                return;
            }

            if (UrlMatchFlag || RangeVideoFlag || RangeVideoFullFlag) {
                //System.out.println("debug2");
                getURL.setHTTPRequest(httpRequest);
                getURL.setURL(URI);
                getURL.setHTTPSocket(ch);

                getURL.run();
                ctx.close();
                return;
            }

            Function.sendHttpData(ch, new HttpHeader(httpVersion, 404, Function.contentType_textPlain, null, "*", Function.content_NotFound, null));

            //ctx.asyncSocketChannel.write(ctx.buffer, ctx, responseHandler);
        }

        @Override
        public void failed(Throwable e, Context ctx) {
            e.printStackTrace();
            ctx.close();
        }
    };

    private final CompletionHandler<Integer, Context> responseHandler = new CompletionHandler<>() {
        @Override
        public void completed(Integer result, Context ctx) {
            //System.out.println("write " + result + " bytes.");
            boolean hasRemaining = ctx.buffer.hasRemaining();
            ctx.buffer.compact();
            if (hasRemaining) {
                ctx.asyncSocketChannel.write(ctx.buffer, ctx, responseHandler);
            } else {
                ctx.asyncSocketChannel.read(ctx.buffer, ctx, requestHandler);
            }
        }

        @Override
        public void failed(Throwable e, Context ctx) {
            e.printStackTrace();
            ctx.close();
        }
    };

    record Context(AsynchronousSocketChannel asyncSocketChannel, ByteBuffer buffer) {
        public void close() {
                try {
                    asyncSocketChannel.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

}