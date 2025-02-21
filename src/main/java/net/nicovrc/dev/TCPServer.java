package net.nicovrc.dev;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TCPServer extends Thread {

    private final int HTTPPort;
    private final boolean[] temp = {true};

    public TCPServer(){
        this.HTTPPort = 25252;
    }

    @Override
    public void run() {
        ServerSocket svSock = null;
        try {
            svSock = new ServerSocket(HTTPPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("[Info] TCP Port " + HTTPPort + "で 処理受付用HTTPサーバー待機開始");
        while (temp[0]) {
            try {
                final Socket sock = svSock.accept();
                Thread.ofVirtual().start(() -> {
                    try {
                        final InputStream in = sock.getInputStream();
                        final OutputStream out = sock.getOutputStream();

                        StringBuffer sb = new StringBuffer();
                        byte[] data = new byte[1024];
                        int readSize = in.read(data);

                        if (readSize <= 0) {
                            in.close();
                            out.close();
                            sock.close();
                            return;
                        }
                        data = Arrays.copyOf(data, readSize);
                        sb.append(new String(data, StandardCharsets.UTF_8));
                    } catch (Exception e) {

                    }
                });
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
