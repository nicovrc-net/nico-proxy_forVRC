package net.nicovrc.dev;

import net.nicovrc.dev.http.NicoVRCHTTP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TCPServer extends Thread {

    private final int HTTPPort;
    private final boolean[] temp = {true};
    private final List<NicoVRCHTTP> httpServiceList;

    private final Timer stopTimer = new Timer();

    public TCPServer(List<NicoVRCHTTP> list){
        this.HTTPPort = 25252;
        this.httpServiceList = list;

        // 停止監視
        stopTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File file = new File("./stop.txt");
                File file2 = new File("./stop_lock.txt");
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
                        stream.write(new byte[0]);
                        stream.close();
                        socket.close();
                        stopTimer.cancel();
                        System.out.println("[Info] 終了処理を完了しました。");
                    }
                } catch (Exception e){
                    // e.printStackTrace();
                }

                file.deleteOnExit();
                file2.deleteOnExit();
            }
        }, 0L, 1000L);
    }

    @Override
    public void run() {
        ServerSocket svSock;
        try {
            svSock = new ServerSocket(HTTPPort);
        } catch (IOException e) {
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

                        final String httpRequest = Function.getHTTPRequest(sock);
                        //System.out.println(httpRequest);
                        if (httpRequest == null) {
                            in.close();
                            out.close();
                            sock.close();

                            in = null;
                            out = null;
                            return;
                        }

                        String HTTPVersion = Function.getHTTPVersion(httpRequest);
                        String Method = Function.getMethod(httpRequest);
                        if (Method == null) {

                            Function.sendHTTPRequest(sock, HTTPVersion, 405, "text/plain; charset=utf-8", "Not Support Method".getBytes(StandardCharsets.UTF_8), false);

                            in.close();
                            out.close();
                            sock.close();


                            in = null;
                            out = null;
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
                            return;
                        }

                        String URI = Function.getURI(httpRequest);
                        //System.out.println("[Debug] " + URI);

                        // それぞれの処理へ飛ぶ
                        boolean[] isFound = {false};
                        for (NicoVRCHTTP vrchttp : httpServiceList) {
                            if (URI.startsWith(vrchttp.getStartURI())) {
                                vrchttp.setURL(URI);
                                vrchttp.setHTTPRequest(httpRequest);
                                vrchttp.setHTTPSocket(sock);
                                Thread sub_thread = Thread.ofVirtual().start((Runnable) vrchttp);
                                try {
                                    sub_thread.join();
                                } catch (Exception e){
                                    // e.printStackTrace();
                                }
                                isFound[0] = true;
                            }
                        }

                        if (!isFound[0]) {
                            Function.sendHTTPRequest(sock, null, 400, "text/plain; charset=utf-8", "Bad Request".getBytes(StandardCharsets.UTF_8), isHead);

                            in.close();
                            out.close();
                            sock.close();


                            in = null;
                            out = null;
                            return;
                        }

                        in.close();
                        out.close();
                        sock.close();
                        in = null;
                        out = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                try {
                    thread.join();
                } catch (Exception e){
                    // e.printStackTrace();
                }
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
    }
}
