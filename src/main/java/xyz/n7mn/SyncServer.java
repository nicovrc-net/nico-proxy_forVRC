package xyz.n7mn;

import com.google.gson.Gson;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.data.QueueData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
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
            System.out.println("[Info] "+Master.replaceAll("-:","Port ")+"で 同期サーバー待機開始");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Timer timer = new Timer();
        int[] count = new int[]{-1};
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                HashMap<String, String> temp = new HashMap<>(QueueList);
                if (count[0] != temp.size()){
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] 現在のキュー数 : " + temp.size());
                    count[0] = temp.size();
                }
                // すでに有効期限が切れていて見れないものは削除

                temp.forEach((id, url)->{
                    try {
                        OkHttpClient build = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .build();
                        Response response = build.newCall(request).execute();

                        if (response.code() == 403 || response.code() == 404){
                            QueueList.remove(id);
                            System.out.println("[Info] キューから " + id + " を削除");
                        }
                        response.close();
                    } catch (Exception e) {
                        //System.out.println(e.getMessage());
                    }
                });

                System.gc();
            }
        };

        timer.scheduleAtFixedRate(task, 0L, 1000L);

        while (true) {
            try {
                System.gc();
                Socket socket = svSock.accept();
                new Thread(() -> {
                    try {
                        InputStream in = socket.getInputStream();
                        OutputStream out = socket.getOutputStream();
                        byte[] data = new byte[1000000];
                        int readSize = in.read(data);
                        String requestText = new String(Arrays.copyOf(data, readSize));

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

                            if (!split[1].isEmpty()) {
                                QueueList.put(split[0], split[1]);
                                System.out.println("[Info] キューに "+split[0]+" を追加");
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

                    System.gc();
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
