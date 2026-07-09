package net.nicovrc.dev;

import net.nicovrc.dev.api.NicoVRCAPI;
import net.nicovrc.dev.http.*;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
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

                final File file = new File("./stop.txt");
                final File file2 = new File("./stop_lock.txt");

                Thread.ofVirtual().start(()->{
                    if (!file.exists()){
                        return;
                    }

                    if (file2.exists()){
                        return;
                    }

                    try {
                        if (file2.createNewFile()){
                            System.out.println("[Info] 終了処理を開始します。");
                            Socket socket = new Socket("127.0.0.1", Function.config_httpPort);
                            OutputStream stream = socket.getOutputStream();
                            stream.write(("stop-"+stopCode).getBytes(StandardCharsets.UTF_8));
                            stream.close();
                            socket.close();
                            Function.checkTimer.cancel();
                            //System.out.println("[Info] 終了処理を完了しました。");
                        }
                    } catch (Exception e){
                        // e.printStackTrace();
                    } finally {
                        Function.checkTimer.cancel();
                        System.out.println("[Info] 終了処理を完了しました。");
                    }

                    file.delete();
                    file2.delete();
                });
/*
                Thread.ofVirtual().start(()->{
                    try {
                        if (!temp[0]){
                            file.createNewFile();
                            Function.checkTimer.cancel();
                            return;
                        }

                        try {
                            Socket socket = new Socket("127.0.0.1", HTTPPort);
                            OutputStream stream = socket.getOutputStream();
                            stream.write(("server-check_"+Function.Version).getBytes(StandardCharsets.UTF_8));
                            socket.close();
                        } catch (Exception e){
                            file.createNewFile();
                            Function.checkTimer.cancel();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                });*/
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
                    ByteBuffer buf = ByteBuffer.allocate(2048);
                    ch.read(buf, buf, new CompletionHandler<>() {
                        public void completed(Integer n, ByteBuffer b) {
                            if (n == -1) {
                                close(ch);
                                return;
                            }
                            b.flip();
                            //System.out.println(new String(b.array(), StandardCharsets.UTF_8));
                            final String httpRequest = Function.getHTTPRequest(b);

                            System.out.println(httpRequest);

                            if (httpRequest.isEmpty()) {
                                close(ch);
                                return;
                            }

                            if (httpRequest.equals("stop-"+stopCode)) {
                                System.out.println("stop-"+stopCode);
                                close(ch);
                                try {
                                    server.close();
                                    System.out.println(stopCode);
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }
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
                                httpHeader = Function.createHTTPHeader(httpVersion, 405, Function.contentType_textPlain, null, "*", httpBody, null, false, -1, -1, -1);
                                Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                                return;
                            }

                            final String URI = Function.getURI(httpRequest);
                            if (URI == null) {
                                //System.out.println("[Debug] HTTPRequest送信");
                                httpBody = Function.content_BadGateway;
                                httpHeader = Function.createHTTPHeader(httpVersion, 502, Function.contentType_textPlain, null, "*", httpBody, null, false, -1, -1, -1);

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
                                        httpHeader = Function.createHTTPHeader(httpVersion, 200, Function.contentType_json, null, "*", httpBody, null, false, -1, -1, -1);
                                        Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                                        break;
                                    }
                                }

                                if (httpHeader == null) {
                                    httpBody = Function.content_errorAPINotFound;
                                    httpHeader = Function.createHTTPHeader(httpVersion, 404, Function.contentType_textPlain, null, "*", httpBody, null, false, -1, -1, -1);
                                    Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
                                }

                                return;
                            }

                            if (VideoMatchFlag) {
                                getVideo.setHTTPRequest(httpRequest);
                                getVideo.setURL(URI);
                                getVideo.setHTTPSocket(ch);

                                getVideo.run();
                                return;
                            }

                            if (UrlMatchFlag) {
                                getURL.setHTTPRequest(httpRequest);
                                getURL.setURL(URI);
                                getURL.setHTTPSocket(ch);

                                getURL.run();
                                return;
                            }

                            httpBody = Function.content_NotFound;
                            httpHeader = Function.createHTTPHeader(httpVersion, 404, Function.contentType_textPlain, null, "*", httpBody, null, false, -1, -1, -1);

                            Function.sendHTTPData(ch, Function.createSendHTTPData(httpHeader, httpBody));
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
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Function.checkTimer.cancel();
    }
}
