package xyz.n7mn.data;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingHTTPServer extends Thread{
    private final int PingHTTPPort;

    public PingHTTPServer(int pingHTTPPort) {
        this.PingHTTPPort = pingHTTPPort;
    }

    @Override
    public void run() {
        ServerSocket svSock = null;
        try {
            svSock = new ServerSocket(PingHTTPPort);
            System.out.println("[Info] Port "+PingHTTPPort+"で 死活監視用HTTPサーバー待機開始");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        while (true) {
            try {
                Socket socket1 = svSock.accept();
                InputStream inputStream = socket1.getInputStream();
                OutputStream outputStream = socket1.getOutputStream();

                byte[] data = new byte[1000000];
                int readSize = inputStream.read(data);
                if (readSize == 0){
                    outputStream.write(("HTTP/1.1 400 Bad Request\r\n" +
                            "date: " + new Date() + "\r\n" +
                            "content-type: text/plain\r\n\r\n" +
                            "400\r\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    socket1.close();
                    return;
                }
                data = Arrays.copyOf(data, readSize);

                String text = new String(data, StandardCharsets.UTF_8);
                Matcher matcher1 = Pattern.compile("GET").matcher(text);
                Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);

                String httpVersion = "1." + (matcher2.find() ? matcher2.group(1) : "1");

                String response;
                if (!matcher1.find()) {
                    response = "HTTP/1." + httpVersion + " 400 Bad Request\r\n" +
                            "date: " + new Date() + "\r\n" +
                            "content-type: text/plain\r\n\r\n" +
                            "400\r\n";

                } else {
                    response = "HTTP/1." + httpVersion + " 200 OK\r\n" +
                            "date: " + new Date() + "\r\n" +
                            "Content-type: text/plain; charset=UTF-8\r\n\r\n" +
                            "ok";
                }

                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                inputStream.close();
                outputStream.close();

                socket1.close();

                System.gc();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
