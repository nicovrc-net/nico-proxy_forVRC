package xyz.n7mn;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.data.SyncData;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SyncServer extends Thread {
    private final String Master;
    private final ConcurrentHashMap<String, String> QueueList;

    public SyncServer(String master, ConcurrentHashMap<String, String> queueList){
        this.Master = master;
        this.QueueList = queueList;
    }

    @Override
    public void run() {
        // 定期的な掃除
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final HashMap<String, String> temp = new HashMap<>(QueueList);
                temp.forEach((requestUrl, resultUrl)->{
                    if (resultUrl.equals("preadd")){
                        return;
                    }

                    try {
                        OkHttpClient build = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(resultUrl)
                                .build();
                        Response response = build.newCall(request).execute();
                        if (response.code() >= 200 && response.code() <= 299){
                            response.close();
                            return;
                        }
                        delQueue(new SyncData(requestUrl, resultUrl));
                        System.out.println("[Info] "+requestUrl+"を自動削除 (キュー数 : "+QueueList.size()+")");
                        response.close();
                    } catch (Exception e){
                        // e.printStackTrace();
                    }
                });
            }
        }, 0L, 5000L);


        if (Master.split(":")[0].equals("-")){
            // UDPで受付
            System.out.println("[Info] UDP Port "+Integer.parseInt(Master.split(":")[1])+"で キューサーバー待機開始");
            while (true){
                try {
                    //System.out.println("Debug");
                    DatagramSocket sock = new DatagramSocket(Integer.parseInt(Master.split(":")[1]));

                    byte[] data = new byte[1000000];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    sock.receive(packet);
                    String s = new String(Arrays.copyOf(packet.getData(), packet.getLength()));
                    //System.out.println("受信 : "+s);
                    InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());
                    SyncData syncData = new Gson().fromJson(s, SyncData.class);
                    //System.out.println(syncData.getRequestURL() + " / " + syncData.getResultURL());
                    if (syncData.getRequestURL() == null) {
                        sock.close();
                        continue;
                    }

                    if (syncData.getResultURL() != null){
                        if (syncData.getResultURL().isEmpty()){
                            // 削除処理
                            delQueue(syncData);
                            byte[] bytes = "{\"ok\"}".getBytes(StandardCharsets.UTF_8);
                            System.out.println("[Info] "+syncData.getRequestURL()+"を削除しました。 (キュー数 : "+QueueList.size()+")");
                            sock.send(new DatagramPacket(bytes, bytes.length, address));
                            sock.close();
                            continue;
                        }
                        // 登録処理
                        //System.out.println("[Debug] " + syncData.getRequestURL() + " / " + syncData.getResultURL());
                        setQueue(syncData);
                        if (!syncData.getResultURL().equals("preadd")){
                            System.out.println("[Info] "+syncData.getRequestURL()+"を追加しました。 (キュー数 : "+QueueList.size()+")");
                        }

                        byte[] bytes = "{\"ok\"}".getBytes(StandardCharsets.UTF_8);
                        sock.send(new DatagramPacket(bytes, bytes.length, address));
                    } else {
                        // 取得処理
                        String queue = getQueue(syncData);
                        if (queue == null){
                            queue = "null";
                        }
                        byte[] bytes = queue.getBytes(StandardCharsets.UTF_8);
                        //System.out.println(packet.getPort());
                        //System.out.println("[Debug] " + new String(bytes) + "を送信します");
                        sock.send(new DatagramPacket(bytes, bytes.length, address));
                        //System.out.println("[Debug] " + new String(bytes) + "を送信しました");
                    }
                    sock.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            //System.out.println("[Error] キューサーバー 異常終了 再起動してください。");
        }
    }


    private void setQueue(SyncData data){
        if (QueueList.get(data.getRequestURL()) != null && !QueueList.get(data.getRequestURL()).equals("preadd")){
            return;
        }

        QueueList.put(data.getRequestURL(), data.getResultURL());
    }

    private String getQueue(SyncData data){
        return QueueList.get(data.getRequestURL());
    }

    private void delQueue(SyncData data){
        QueueList.remove(data.getRequestURL());
    }
}