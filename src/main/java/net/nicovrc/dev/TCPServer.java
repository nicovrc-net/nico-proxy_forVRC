package net.nicovrc.dev;

import net.nicovrc.dev.api.NicoVRCAPI;
import net.nicovrc.dev.http.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPServer extends Thread {

    private final int HTTPPort;
    private final boolean[] temp = {true};

    private final HttpClient client;

    private final GetURL getURL = new GetURL();
    private final GetVideo getVideo = new GetVideo();

    private static Pattern matcher_uri = Pattern.compile("(url=|vi=)");


    public TCPServer(HttpClient client){
        this.client = client;
        this.HTTPPort = Function.config_httpPort;

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
                            temp[0] = false;
                            Socket socket = new Socket("127.0.0.1", HTTPPort);
                            OutputStream stream = socket.getOutputStream();
                            stream.write(Function.zeroByte);
                            stream.close();
                            socket.close();
                            Function.checkTimer.cancel();
                            System.out.println("[Info] 終了処理を完了しました。");
                        }
                    } catch (Exception e){
                        // e.printStackTrace();
                    } finally {
                        temp[0] = false;
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
        ServerSocket svSock;
        try {
            svSock = new ServerSocket(HTTPPort);
        } catch (IOException e) {
            temp[0] = false;
            Function.checkTimer.cancel();
            throw new RuntimeException(e);
        }

        System.out.println("[Info] TCP Port " + HTTPPort + "で 処理受付用HTTPサーバー待機開始");
        System.out.println("[Info] VRC動画プレーヤーからはhttp://(サーバーIP):"+HTTPPort+"/?url=(URL)");

        while (temp[0]) {
            try {
                final Socket sock = svSock.accept();
                Thread.ofVirtual().start(() -> {
                    try {
                        //System.out.println("A");

                        if (!sock.isConnected()) {
                            return;
                        }

                        if (sock.isClosed()) {
                            return;
                        }
                        //System.out.println("B");

                        final String httpRequest = Function.getHTTPRequest(sock);
                        //System.out.println(httpRequest);
                        //System.out.println("C");

                        if (httpRequest == null) {
                            return;
                        }
                        //System.out.println("D");


                        final String httpVersion = Function.getHTTPVersion(httpRequest);
                        final String httpMethod = Function.getMethod(httpRequest);

                        final boolean isGET = httpMethod != null && httpMethod.equals("GET");
                        final boolean isPOST = httpMethod != null && httpMethod.equals("POST");
                        final boolean isHead = httpMethod != null && httpMethod.equals("HEAD");

                        //System.out.println("AAAA");

                        if (!isGET && !isPOST && !isHead) {
                            //System.out.println("[Debug] HTTPRequest送信");
                            Function.sendHTTPRequest(sock, httpVersion, 405, Function.contentType_textPlain, null, "*", Function.content_MethodNotAllowed, false, null);
                            //sock.close();

                            return;
                        }

                        final String URI = Function.getURI(httpRequest);

                        if (URI == null) {
                            //System.out.println("[Debug] HTTPRequest送信");
                            Function.sendHTTPRequest(sock, httpVersion, 502, Function.contentType_textPlain, null, "*", Function.content_BadGateway, isHead, null);
                            //sock.close();

                            return;
                        }

                        final Matcher matcher = matcher_uri.matcher(URI);
                        final boolean ApiMatchFlag = URI.startsWith("/api/");
                        final boolean UrlMatchFlag = URI.startsWith("/?url=") || URI.startsWith("/proxy") || matcher.find();
                        final boolean VideoMatchFlag = URI.startsWith("/https");

                        //System.out.println("AAAB : " + URI);

                        if (ApiMatchFlag){
                            //System.out.println("AAAC");

                            for (NicoVRCAPI api : Function.APIList) {
                                if (URI.startsWith(api.getURI())){
                                    Function.sendHTTPRequest(sock, httpVersion, 200, Function.contentType_json, null, "*", api.Run(httpRequest, client).getBytes(StandardCharsets.UTF_8), isHead, null);
                                    break;
                                }
                            }

                            Function.sendHTTPRequest(sock, httpVersion, 404, Function.contentType_textPlain, null, null, Function.content_errorAPINotFound, isHead, null);
                            //sock.close();
                            return;
                        }

                        if (VideoMatchFlag){
                            //System.out.println("AAAC-3");
                            getVideo.setHTTPClient(client);
                            getVideo.setHTTPRequest(httpRequest);
                            getVideo.setURL(URI);
                            getVideo.setHTTPSocket(sock);
                            getVideo.run();
                            //sock.close();
                            return;
                        }

                        if (UrlMatchFlag){
                            //System.out.println("AAAC-2");
                            getURL.setHTTPClient(client);
                            getURL.setHTTPRequest(httpRequest);
                            getURL.setURL(URI);
                            getURL.setHTTPSocket(sock);
                            getURL.run();
                            //sock.close();
                            return;
                        }

                        Function.sendHTTPRequest(sock, httpVersion, 404, Function.contentType_textPlain, null, "*", Function.content_NotFound, isHead, null);
                        //sock.close();


                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                Function.checkTimer.cancel();
                break;
            }
        }
        try {
            svSock.close();
            svSock = null;
        } catch (Exception e){
            // e.printStackTrace();
        }
        Function.checkTimer.cancel();
    }
}
