package net.nicovrc.dev;

import net.nicovrc.dev.Service.Vimeo;
import net.nicovrc.dev.api.GetSupportList;
import net.nicovrc.dev.api.Test;
import net.nicovrc.dev.http.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class TCPServer extends Thread {

    private final int HTTPPort;
    private final boolean[] temp = {true};


    private final HashMap<String, NicoVRCHTTP> httpService = new HashMap<>();
    private final HttpClient client;

    private final String textPlain = "text/plain; charset=utf-8";
    private final byte[] err400 = "Bad Request".getBytes(StandardCharsets.UTF_8);
    private final byte[] err405 = "Not Support Method".getBytes(StandardCharsets.UTF_8);

    public TCPServer(HttpClient client){
        final GetURL getURL = new GetURL();
        final GetURL_dummy getURLDummy = new GetURL_dummy();
        final GetURL_dummy2 getURLDummy2 = new GetURL_dummy2();
        final GetURL_old1 getURLOld1 = new GetURL_old1();
        final GetURL_old2 getURLOld2 = new GetURL_old2();
        final GetVideo getVideo = new GetVideo();
        final NicoVRCWebAPI nicoVRCWebAPI = new NicoVRCWebAPI();

        this.client = client;

        httpService.put(getURL.getStartURI().substring(0, Math.min(getURL.getStartURI().length(), 15)), getURL);
        httpService.put(getURLDummy.getStartURI().substring(0, Math.min(getURLDummy.getStartURI().length(), 15)), getURLDummy);
        httpService.put(getURLDummy2.getStartURI().substring(0, Math.min(getURLDummy2.getStartURI().length(), 15)), getURLDummy2);
        httpService.put(getURLOld1.getStartURI().substring(0, Math.min(getURLOld1.getStartURI().length(), 15)), getURLOld1);
        httpService.put(getURLOld2.getStartURI().substring(0, Math.min(getURLOld2.getStartURI().length(), 15)), getURLOld2);
        httpService.put(getVideo.getStartURI().substring(0, Math.min(getVideo.getStartURI().length(), 15)), getVideo);
        httpService.put(nicoVRCWebAPI.getStartURI().substring(0, Math.min(nicoVRCWebAPI.getStartURI().length(), 15)), nicoVRCWebAPI);

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
                            Function.sendHTTPRequest(sock, httpVersion, 405, textPlain, null, "*", Function.contentMethodNotAllowed, false, null);
                            sock.close();

                            return;
                        }

                        final String URI = Function.getURI(httpRequest);

                        if (URI == null) {
                            //System.out.println("[Debug] HTTPRequest送信");
                            Function.sendHTTPRequest(sock, httpVersion, 502, textPlain, null, "*", Function.contentBadGateway, isHead, null);
                            sock.close();

                            return;
                        }

                        final boolean ApiMatchFlag = URI.startsWith("/api/");
                        final boolean UrlMatchFlag = URI.startsWith("/?url=") || URI.matches(".*url=.*") || URI.startsWith("/proxy") || URI.matches(".*vi=.*");
                        final boolean VideoMatchFlag = URI.startsWith("/https");

                        //System.out.println("AAAB : " + URI);

                        if (ApiMatchFlag){
                            //System.out.println("AAAC");
                            Test test = new Test();
                            GetSupportList support = new GetSupportList();
                            if (URI.startsWith(test.getURI())){
                                Function.sendHTTPRequest(sock, httpVersion, 200, "application/json; charset=utf-8", null, "*", test.Run(httpRequest, client).getBytes(StandardCharsets.UTF_8), isHead, null);
                            }
                            if (URI.startsWith(support.getURI())){
                                Function.sendHTTPRequest(sock, httpVersion, 200, "application/json; charset=utf-8", null, "*", support.Run(httpRequest, client).getBytes(StandardCharsets.UTF_8), isHead, null);
                            }
                            sock.close();
                            return;
                        }

                        if (VideoMatchFlag){
                            //System.out.println("AAAC-3");
                            GetVideo getVideo = new GetVideo();
                            getVideo.setHTTPClient(client);
                            getVideo.setHTTPRequest(httpRequest);
                            getVideo.setURL(URI);
                            getVideo.setHTTPSocket(sock);
                            getVideo.run();
                            sock.close();
                            return;
                        }

                        if (UrlMatchFlag){
                            //System.out.println("AAAC-2");
                            GetURL getURL = new GetURL();
                            getURL.setHTTPClient(client);
                            getURL.setHTTPRequest(httpRequest);
                            getURL.setURL(URI);
                            getURL.setHTTPSocket(sock);
                            getURL.run();
                            sock.close();
                            return;
                        }

                        Function.sendHTTPRequest(sock, httpVersion, 404, textPlain, null, "*", Function.contentNotFound, isHead, null);
                        sock.close();


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
