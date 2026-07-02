package net.nicovrc.dev;

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
                            stream.write(Function.zeroByte);
                            stream.close();
                            socket.close();
                        } catch (Exception e){
                            file.createNewFile();
                            Function.checkTimer.cancel();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                });
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
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        if (!sock.isConnected()){
                            sock.close();
                            return;
                        }

                        if (sock.isClosed()){
                            sock.close();
                            return;
                        }

                        String httpRequest = Function.getHTTPRequest(sock);
                        //System.out.println(httpRequest);
                        if (httpRequest == null) {
                            sock.close();
                            return;
                        }

                        Thread.ofVirtual().start(() -> {
                            try {
                                Thread.sleep(6000L);

                                if (!sock.isClosed()){
                                    String request = Function.getHTTPRequest(sock);
                                    String httpVersion = Function.getHTTPVersion(request);
                                    String Method = Function.getMethod(request);

                                    if (httpVersion != null){
                                        Function.sendHTTPRequest(sock, httpVersion, 503, textPlain, null, null, "".getBytes(StandardCharsets.UTF_8), Method != null && Method.equalsIgnoreCase("head"), null);
                                    }
                                    sock.close();
                                }
                            } catch (Exception e){
                                //e.printStackTrace();
                            }
                        });

                        //System.out.println(httpRequest);

                        String HTTPVersion = Function.getHTTPVersion(httpRequest);
                        String Method = Function.getMethod(httpRequest);

                        if (Method == null) {

                            byte[] bytes = err405;
                            Function.sendHTTPRequest(sock, HTTPVersion, 405, textPlain, null, null, err405, false, null);
                            sock.close();

                            HTTPVersion = null;
                            httpRequest = null;
                            return;
                        }

                        final boolean isHead = Method.equals("HEAD");
                        if (HTTPVersion == null) {
                            Function.sendHTTPRequest(sock, null, 400, textPlain, null, null, err400, isHead, null);
                            sock.close();

                            httpRequest = null;
                            Method = null;
                            return;
                        }

                        String URI = Function.getURI(httpRequest);
                        //System.out.println("[Debug] " + URI);

                        // それぞれの処理へ飛ぶ
                        String[] split = URI.split("\\?");

                        String s = "";
                        //System.out.println("debug0 : " + split.length);
                        if (split.length == 1){
                            s = "/" + split[0].split("/")[1] + "/";
                            //System.out.println("debug1 : "+s);
                        }
                        if (split.length >= 2){
                            s = split[0];
                            //System.out.println("debug2 : "+s);
                            if (split[0].startsWith("/api")) {
                                s = "/" + split[0].split("/")[1] + "/";
                            } else if (split[0].startsWith("/dummy")){
                                s = "/dummy.m3u8";
                            } else if (split[1].startsWith("url") || URI.matches(".*&url=.*")){
                                s = s + "?url=";
                            } else if (split[1].startsWith("dummy")){
                                s = s + "?dummy=";
                            } else if (split[1].startsWith("vi")){
                                s = s + "?vi=";
                            } else if (split[0].startsWith("/proxy")){
                                s = s + "?";
                            } else if (split[0].startsWith("http") || split[0].startsWith("/http")){
                                s = "/https/";
                            }
                        }

                        //System.out.println(s);
                        NicoVRCHTTP vrchttp = httpService.get(s.substring(0, Math.min(s.length(), 15)));
                        if (vrchttp != null){
                            // Proxy
                            String p = null;
                            String[] st = null;
                            if (!Function.ProxyList.isEmpty()){
                                int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
                                p = Function.ProxyList.get(i);
                                st = p.split(":");
                            }


                            final String url = URI.split("\\?")[0];
                            final Matcher matcher_normal = Function.NicoID1.matcher(url);
                            final Matcher matcher_short_video = Function.NicoID_short.matcher(url);
                            final Matcher matcher_short = Function.NicoID2.matcher(url);
                            final Matcher matcher_cas = Function.NicoID3.matcher(url);
                            final Matcher matcher_idOnly = Function.NicoID4.matcher(url);

                            final boolean isNormal = matcher_normal.find();
                            final boolean isShortVideo = matcher_short_video.find();
                            final boolean isShort = matcher_short.find();
                            final boolean isCas = matcher_cas.find();
                            final boolean isID = matcher_idOnly.find();

                            String id = "";

                            if (isID){
                                id = matcher_idOnly.group(1);
                            } else if (isNormal){
                                id = matcher_normal.group(3);
                            } else if (isShortVideo){
                                id = matcher_short_video.group(2);
                            } else if (isShort) {
                                id = matcher_short.group(2);
                            }

                            if (isID || isNormal || isShort || isShortVideo){
                                if (id.startsWith("lv") || id.startsWith("so")){
                                    if (!Function.JP_ProxyList.isEmpty()){
                                        int i = Function.JP_ProxyList.size() > 1 ? new SecureRandom().nextInt(0, Function.JP_ProxyList.size()) : 0;
                                        p = Function.JP_ProxyList.get(i);
                                        st = p.split(":");
                                    }
                                }
                            }

                            if (isCas){
                                if (!Function.JP_ProxyList.isEmpty()){
                                    int i = Function.JP_ProxyList.size() > 1 ? new SecureRandom().nextInt(0, Function.JP_ProxyList.size()) : 0;
                                    p = Function.JP_ProxyList.get(i);
                                    st = p.split(":");
                                }
                            }

                            vrchttp.setURL(URI);
                            vrchttp.setHTTPRequest(httpRequest);
                            vrchttp.setHTTPSocket(sock);
                            vrchttp.setHTTPClient(client);
                            Thread start = Thread.ofVirtual().start((Runnable) vrchttp);
                        } else {
                            Function.sendHTTPRequest(sock, null, 400, textPlain, null, null, err400, isHead, null);

                            sock.close();

                            URI = null;
                            httpRequest = null;
                            return;
                        }

                        //sock.close();
                        URI = null;
                        httpRequest = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                try {
                    final Thread finalThread = thread;
                    Thread.ofVirtual().start(()->{
                        try {
                            Thread.sleep(60000L);
                            while (sock.isConnected()){
                                Thread.sleep(1000L);
                            }
                            if (finalThread.isAlive()){
                                finalThread.interrupt();
                            }
                        } catch (Exception e){
                            // e.printStackTrace();
                        }
                    });
                    //thread.join();
                } catch (Exception e){
                    e.printStackTrace();
                }
                //thread = null;
                //sock.close();
            } catch (Exception e) {
                e.printStackTrace();
                temp[0] = false;
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
