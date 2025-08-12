package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import net.nicovrc.dev.http.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class TCPServer extends Thread {

    private final int HTTPPort;
    private final boolean[] temp = {true};

    private final Timer checkTimer = new Timer();

    private final HashMap<String, NicoVRCHTTP> httpService = new HashMap<>();

    public TCPServer(){
        final GetURL getURL = new GetURL();
        final GetURL_dummy getURLDummy = new GetURL_dummy();
        final GetURL_dummy2 getURLDummy2 = new GetURL_dummy2();
        final GetURL_old1 getURLOld1 = new GetURL_old1();
        final GetURL_old2 getURLOld2 = new GetURL_old2();
        final GetVideo getVideo = new GetVideo();
        final NicoVRCWebAPI nicoVRCWebAPI = new NicoVRCWebAPI();

        httpService.put(getURL.getStartURI().substring(0, 5), getURL);
        httpService.put(getURLDummy.getStartURI().substring(0, 5), getURLDummy);
        httpService.put(getURLDummy2.getStartURI().substring(0, 5), getURLDummy2);
        httpService.put(getURLOld1.getStartURI().substring(0, 5), getURLOld1);
        httpService.put(getURLOld2.getStartURI().substring(0, 5), getURLOld2);
        httpService.put(getVideo.getStartURI().substring(0, 5), getVideo);
        httpService.put(nicoVRCWebAPI.getStartURI().substring(0, 5), nicoVRCWebAPI);

        int tempPort;

        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            tempPort = yamlMapping.integer("Port");
        } catch (Exception e){
            tempPort = 25252;
        }

        this.HTTPPort = tempPort;

        // 停止監視 & 死活監視
        checkTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                Thread.ofVirtual().start(()->{
                    File file = new File("./stop.txt");
                    File file2 = new File("./stop_lock.txt");
                    if (!file.exists()){
                        file = null;
                        file2 = null;
                        return;
                    }

                    if (file2.exists()){
                        file = null;
                        file2 = null;
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
                            checkTimer.cancel();
                            System.out.println("[Info] 終了処理を完了しました。");
                        }
                    } catch (Exception e){
                        // e.printStackTrace();
                    } finally {
                        temp[0] = false;
                        checkTimer.cancel();
                        System.out.println("[Info] 終了処理を完了しました。");
                    }

                    file.delete();
                    file2.delete();
                    file = null;
                    file2 = null;
                });

                Thread.ofVirtual().start(()->{
                    try {
                        File file = new File("./stop.txt");
                        if (!temp[0]){
                            file.createNewFile();
                            checkTimer.cancel();
                            file = null;
                            return;
                        }

                        try {
                            Socket socket = new Socket("127.0.0.1", HTTPPort);
                            OutputStream stream = socket.getOutputStream();
                            stream.write(Function.zeroByte);
                            stream.close();
                            socket.close();
                            file = null;
                        } catch (Exception e){
                            file.createNewFile();
                            file = null;
                            checkTimer.cancel();
                        }
                    } catch (Exception e){
                        // e.printStackTrace();
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
            checkTimer.cancel();
            throw new RuntimeException(e);
        }

        System.out.println("[Info] TCP Port " + HTTPPort + "で 処理受付用HTTPサーバー待機開始");
        while (temp[0]) {
            try {
                final Socket sock = svSock.accept();
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        InputStream in = sock.getInputStream();
                        OutputStream out = sock.getOutputStream();

                        String httpRequest = Function.getHTTPRequest(sock);
                        //System.out.println(httpRequest);
                        if (httpRequest == null) {
                            in.close();
                            out.close();
                            sock.close();

                            in = null;
                            out = null;
                            return;
                        }

                        //System.out.println(httpRequest);

                        String HTTPVersion = Function.getHTTPVersion(httpRequest);
                        String Method = Function.getMethod(httpRequest);
                        if (Method == null) {

                            Function.sendHTTPRequest(sock, HTTPVersion, 405, "text/plain; charset=utf-8", "Not Support Method".getBytes(StandardCharsets.UTF_8), false);

                            in.close();
                            out.close();
                            sock.close();


                            in = null;
                            out = null;
                            HTTPVersion = null;
                            httpRequest = null;
                            return;
                        }

                        final boolean isHead = Method.equals("HEAD");
                        if (HTTPVersion == null) {
                            Function.sendHTTPRequest(sock, null, 400, "text/plain; charset=utf-8", "Bad Request".getBytes(StandardCharsets.UTF_8), isHead);

                            in.close();
                            out.close();
                            sock.close();


                            in = null;
                            out = null;
                            httpRequest = null;
                            Method = null;
                            return;
                        }

                        String URI = Function.getURI(httpRequest);
                        //System.out.println("[Debug] " + URI);

                        // それぞれの処理へ飛ぶ
                        boolean isFound = false;
                        NicoVRCHTTP vrchttp = httpService.get(URI.substring(0, Math.min(URI.length(), 5)));
                        if (vrchttp != null){
                            isFound = true;
                            vrchttp.setURL(URI);
                            vrchttp.setHTTPRequest(httpRequest);
                            vrchttp.setHTTPSocket(sock);
                            Thread start = Thread.ofVirtual().start((Runnable) vrchttp);
                            try {
                                start.join();
                            } catch (Exception e){
                                // e.printStackTrace();
                            }
                        }

                        if (!isFound) {
                            Function.sendHTTPRequest(sock, null, 400, "text/plain; charset=utf-8", "Bad Request".getBytes(StandardCharsets.UTF_8), isHead);

                            in.close();
                            out.close();
                            sock.close();


                            in = null;
                            out = null;
                            URI = null;
                            httpRequest = null;
                            return;
                        }

                        in.close();
                        out.close();
                        sock.close();
                        in = null;
                        out = null;
                        URI = null;
                        httpRequest = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                try {
                    thread.join();
                } catch (Exception e){
                    // e.printStackTrace();
                }
                thread = null;
                sock.close();
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
        checkTimer.cancel();
    }
}
