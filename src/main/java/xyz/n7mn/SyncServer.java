package xyz.n7mn;

import com.google.gson.Gson;
import xyz.n7mn.data.QueueData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyncServer extends Thread {
    private final String Master;
    private final HashMap<String, String> QueueList;

    public SyncServer(String master, HashMap<String, String> queueList){
        this.Master = master;
        this.QueueList = queueList;
    }

    @Override
    public void run() {
        ServerSocket svSock = null;
        try {
            svSock = new ServerSocket(Integer.parseInt(Master.split(":")[1]));
            System.out.println(Master.replaceAll("-:","Port ")+"で 同期サーバー待機開始");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                System.gc();
                Socket socket = svSock.accept();
                new Thread(() -> {
                    try {
                        InputStream in = socket.getInputStream();
                        OutputStream out = socket.getOutputStream();
                        byte[] tempText = in.readAllBytes();
                        String requestText = new String(tempText);

                        if (requestText.equals("{\"queue\":\"getList\"}")) {
                            QueueData[] temp = new QueueData[QueueList.size()];

                            AtomicInteger i = new AtomicInteger();
                            QueueList.forEach((id, url) -> {
                                temp[i.get()] = new QueueData(id, url);
                                i.getAndIncrement();
                            });

                            out.write(new Gson().toJson(temp).getBytes(StandardCharsets.UTF_8));

                            out.flush();
                            in.close();
                            socket.close();
                            return;
                        }

                        Matcher matcher = Pattern.compile("\\{\"queue\":\"(.*)\"\\}").matcher(requestText);
                        if (matcher.find()) {
                            String[] split = matcher.group(1).split(",");

                            if (split[1].length() != 0) {
                                QueueList.put(split[0], split[1]);
                            } else {
                                QueueList.remove(split[0]);
                            }

                            out.write("{\"queue\": \"ok\"}".getBytes(StandardCharsets.UTF_8));
                        } else {
                            out.write("{}}".getBytes(StandardCharsets.UTF_8));
                        }
                        out.flush();
                        in.close();
                        socket.close();

                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            socket.getOutputStream().write("{\"queue\":\"error\"}".getBytes(StandardCharsets.UTF_8));
                            socket.getOutputStream().flush();
                            socket.close();
                        } catch (IOException ex) {
                            // ex.printStackTrace();
                        }
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
