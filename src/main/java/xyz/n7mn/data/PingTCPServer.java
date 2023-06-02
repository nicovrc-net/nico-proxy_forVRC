package xyz.n7mn.data;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PingTCPServer extends Thread{

    private final int PingPort;
    public PingTCPServer(int pingPort){
        this.PingPort = pingPort;
    }

    @Override
    public void run() {
        ServerSocket svSock = null;
        try {
            svSock = new ServerSocket(PingPort);
            System.out.println("Port "+PingPort+"で 死活監視用TCPサーバー待機開始");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                Socket socket = svSock.accept();
                OutputStream stream = socket.getOutputStream();

                stream.write("{status: \"OK\"}".getBytes(StandardCharsets.UTF_8));
                stream.flush();

                stream.close();
                System.gc();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
