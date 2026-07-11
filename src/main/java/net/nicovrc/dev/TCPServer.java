package net.nicovrc.dev;

import net.nicovrc.dev.api.NicoVRCAPI;
import net.nicovrc.dev.http.*;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPServer extends Thread {

    private final HttpClient client;

    private final GetURL getURL = new GetURL();
    private final GetVideo getVideo = new GetVideo();

    private final static Pattern matcher_uri = Pattern.compile("(url=|vi=|dummy=|dummy\\.m3u8|/proxy)");
    private MessageDigest sha3_256;
    private final String stopCode;


    public TCPServer(HttpClient client){
        String str = null;
        try {
            this.sha3_256 = MessageDigest.getInstance("SHA3-256");
            byte[] digest = sha3_256.digest(Base64.getEncoder().encode(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            str = String.format("%040x", new BigInteger(1, digest));
        } catch (Exception e){
            str = null;
        }
        stopCode = str;

        this.client = client;
        getURL.setHTTPClient(client);
        getURL.setProxy(null);
        getVideo.setHTTPClient(client);
        getVideo.setProxy(null);

        // 停止監視 & 死活監視
        Function.checkTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                Thread.ofVirtual().start(()->{
                    if (!Function.isFoundFile("./stop.txt")) {
                        return;
                    }

                    if (Function.isFoundFile("./stop_lock.txt")) {
                        return;
                    }

                    try {
                        if (Function.writeFile("./stop_lock.txt", Function.zeroByte)){
                            System.out.println("[Info] 終了処理を開始します。");
                            AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
                            client.connect(new InetSocketAddress("127.0.0.1",Function.config_httpPort), null, new CompletionHandler<Void,Void>(){
                                public void completed(Void v, Void a){
                                    ByteBuffer b = ByteBuffer.wrap(("stop-"+stopCode).getBytes(StandardCharsets.UTF_8));
                                    client.write(b, null, new CompletionHandler<Integer,Void>(){
                                        public void completed(Integer r, Void a){

                                        }
                                        public void failed(Throwable e, Void a){
                                            //e.printStackTrace();
                                        }
                                    });
                                }
                                public void failed(Throwable e, Void a){ e.printStackTrace(); }
                            });
                            Function.checkTimer.cancel();
                            System.out.println("[Info] 終了処理を完了しました。");
                        }
                    } catch (Exception e){
                        // e.printStackTrace();
                    } finally {
                        Function.checkTimer.cancel();
                        System.out.println("[Info] 終了処理を完了しました。");
                    }

                    Function.deleteFile("./stop.txt");
                    //file2.delete();
                });
            }
        }, 1000L, 1000L);
    }

    @Override
    public void run() {
        System.out.println("[Info] TCP Port " + Function.config_httpPort + "で 処理受付用HTTPサーバー待機開始");
        System.out.println("[Info] VRC動画プレーヤーからはhttp://(サーバーIP):"+Function.config_httpPort+"/?url=(URL)");

        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open()
                .bind(new InetSocketAddress(Function.config_httpPort))) {
            server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                public void completed(AsynchronousSocketChannel ch, Void att) {
                    server.accept(null, this);

                    ByteBuffer buf = ByteBuffer.allocate(4096);
                    ch.read(buf, buf, new CompletionHandler<>() {
                        public void completed(Integer n, ByteBuffer b) {
                            if (n == -1) {
                                close(ch);
                                return;
                            }
                            b.flip();
                            //System.out.println(new String(b.array(), StandardCharsets.UTF_8));
                            final String httpRequest = Function.getHTTPRequest(b);

                            //System.out.println(httpRequest);

                            if (httpRequest.isEmpty()) {
                                close(ch);
                                return;
                            }

                            final String httpVersion = Function.getHTTPVersion(httpRequest);
                            final String httpMethod = Function.getMethod(httpRequest);

                            final boolean isGET = httpMethod != null && httpMethod.equals("GET");
                            final boolean isPOST = httpMethod != null && httpMethod.equals("POST");
                            final boolean isHead = httpMethod != null && httpMethod.equals("HEAD");

                            String httpHeader = null;
                            byte[] httpBody = null;

                            if (!isGET && !isPOST && !isHead) {
                                //System.out.println("[Debug] HTTPRequest送信");
                                httpBody = Function.content_MethodNotAllowed;
                                httpHeader = Function.createHTTPHeader(httpVersion, 405, Function.contentType_textPlain, null, "*", httpBody, null);
                                Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                                return;
                            }

                            final String URI = Function.getURI(httpRequest);
                            if (URI == null) {
                                //System.out.println("[Debug] HTTPRequest送信");
                                httpBody = Function.content_BadGateway;
                                httpHeader = Function.createHTTPHeader(httpVersion, 502, Function.contentType_textPlain, null, "*", httpBody, null);

                                Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                                return;
                            }

                            final Matcher matcher = matcher_uri.matcher(URI);
                            final boolean ApiMatchFlag = URI.startsWith("/api/");
                            final boolean UrlMatchFlag = matcher.find();
                            final boolean VideoMatchFlag = URI.startsWith("/https");

                            if (ApiMatchFlag) {
                                for (NicoVRCAPI api : Function.APIList) {
                                    if (URI.startsWith(api.getURI())) {
                                        try {
                                            httpBody = api.Run(httpRequest, client).getBytes(StandardCharsets.UTF_8);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        httpHeader = Function.createHTTPHeader(httpVersion, 200, Function.contentType_json, null, "*", httpBody, null);
                                        Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                                        close(ch);
                                        break;
                                    }
                                }

                                if (httpHeader == null) {
                                    httpBody = Function.content_errorAPINotFound;
                                    httpHeader = Function.createHTTPHeader(httpVersion, 404, Function.contentType_textPlain, null, "*", httpBody, null);
                                    Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                                }

                                close(ch);
                                return;
                            }

                            if (VideoMatchFlag) {
                                getVideo.setHTTPRequest(httpRequest);
                                getVideo.setURL(URI);
                                getVideo.setHTTPSocket(ch);

                                getVideo.run();
                                close(ch);
                                return;
                            }

                            if (UrlMatchFlag) {
                                getURL.setHTTPRequest(httpRequest);
                                getURL.setURL(URI);
                                getURL.setHTTPSocket(ch);

                                getURL.run();
                                close(ch);
                                return;
                            }

                            httpBody = Function.content_NotFound;
                            httpHeader = Function.createHTTPHeader(httpVersion, 404, Function.contentType_textPlain, null, "*", httpBody, null);

                            Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                            close(ch);
                        }

                        public void failed(Throwable e, ByteBuffer b) {
                            close(ch);
                        }
                    });
                }

                public void failed(Throwable e, Void att) {
                    e.printStackTrace();
                }

                void close(AsynchronousSocketChannel c) {
                    try {
                        c.close();
                    } catch (Exception ignored) {
                    }
                }
            });

            while (true) {
                if (Function.isFoundFile("./stop_lock.txt")){
                    Function.deleteFile("./stop_lock.txt");
                    break;
                }
                try {
                    Thread.sleep(100L);
                } catch (Exception ignored) {
                    //ignored.printStackTrace();
                }
            }
            //Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Function.checkTimer.cancel();
    }
}
