package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.api.*;
import net.nicovrc.dev.data.*;
import net.nicovrc.dev.server.HTTPServer;
import net.nicovrc.dev.server.UDPServer;
import okhttp3.OkHttpClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final String DefaultConfig = """
# 受付ポート (HTTP/UDP共通)
Port: 25252
# ログをRedisに書き出すときはtrue
LogToRedis: false

# HTTPで受付する場合はtrue
HTTPServer: true
# UDPで受付する場合はtrue
UDPServer: false

# HTTPサーバー/UDPサーバーの受付ログをDiscordへWebhookで配信するかどうか
DiscordWebhook: false
# DiscordのWebhookのURL
DiscordWebhookURL: ""

# ログを強制的に書き出すときの合言葉
# 普通に漏れると危ないので厳重管理すること。
WriteLogPass: 'c41f30a58e09fa1a6618ed06d16f035e98821420bb0b6d70598be5df87f37725'

# 他に処理鯖がある場合はそのリストを「IP:受付ポート」形式で記載する
# (HTTP通信用を1つ、処理鯖(UDP通信)はn個という想定)
ServerList:
    - "127.0.0.1:25252"

# ツイキャスの設定
# https://twitcasting.tv/developer.phpでAPIキーを取得してください
ClientID: ""
ClientSecret: ""

# bilibili変換システム
BiliBiliSystemIP: "127.0.0.1"

# ニコ動domand鯖の変換システム
NicoVideoSystem: "127.0.0.1"

# Redisの設定(LogToRedisをtrue)にしていない場合は設定不要
# RedisサーバーIP
RedisServer: 127.0.0.1
# Redisサーバーポート
RedisPort: 6379
# Redis AUTHパスワード
# パスワードがない場合は以下の通りに設定してください
RedisPass: ""
""";

    private static final String DefaultProxy = """
# 動画取得用 (ニコ動が見れればどこでも可)
VideoProxy:
  - "127.0.0.1:3128"
# 公式動画、生放送用 (ニコ動が見れる国内IPならどこでも可)
OfficialProxy:
  - "127.0.0.1:3128"
""";

    private static final OkHttpClient HttpClient = new OkHttpClient();

    private static final List<ProxyData> MainProxyList = new ArrayList<>();
    private static final List<ProxyData> JPProxyList = new ArrayList<>();


    private static final ConcurrentHashMap<String, CacheData> CacheList = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ServerData> ServerList = new ConcurrentHashMap<>();

    private static final Timer ProxyCheckTimer = new Timer();
    private static final Timer ServerCheckTimer = new Timer();
    private static final Timer CacheCheckTimer = new Timer();

    private static final CacheAPI cacheAPI = new CacheAPI(CacheList, HttpClient);
    private static final ProxyAPI proxyAPI = new ProxyAPI(MainProxyList, JPProxyList);
    private static final ServerAPI serverAPI = new ServerAPI(ServerList);
    private static final JinnnaiSystemURL_API jinnnaiAPI = new JinnnaiSystemURL_API();

    private static Boolean isStop = false;

    public static void main(String[] args) {
        ProxyCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                proxyAPI.ListRefresh();
            }
        }, 0L, 60000L);

        ServerCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                serverAPI.ListRefresh();
            }
        }, 0L, 60000L);

        CacheCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cacheAPI.ListRefresh();

                UDPPacket check = new UDPPacket();
                check.setRequestURL("get_cache");
                Gson gson = new Gson();

                // 処理鯖から改めてキャッシュをゲットしてくる
                ServerList.forEach((ID, ServerData)-> {
                    String ServerIP = ServerData.getIP();
                    int ServerPort = ServerData.getPort();

                    DatagramSocket udp_sock = null;
                    try {
                        udp_sock = new DatagramSocket();

                        byte[] textByte = gson.toJson(check).getBytes(StandardCharsets.UTF_8);
                        DatagramPacket udp_packet = new DatagramPacket(textByte, textByte.length, new InetSocketAddress(ServerIP, ServerPort));
                        udp_sock.send(udp_packet);

                        byte[] temp1 = new byte[104857600];
                        DatagramPacket udp_packet2 = new DatagramPacket(temp1, temp1.length);
                        udp_sock.setSoTimeout(100);
                        udp_sock.receive(udp_packet2);

                        String result = new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength()));
                        //System.out.println("受信 : " + result);
                        //OutputJson json = new Gson().fromJson(result, OutputJson.class);
                        JsonElement json = gson.fromJson(result, JsonElement.class);

                        json.getAsJsonObject().asMap().forEach((i, value) -> {
                            if (CacheList.get(i) == null){
                                CacheList.put(i, new CacheData(value.getAsJsonObject().get("ExpiryDate").getAsLong(), value.getAsJsonObject().get("CacheUrl").getAsString()));
                            }
                        });

                        udp_sock.close();
                    } catch (Exception e) {
                        //e.printStackTrace();
                        try {
                            if (udp_sock != null) {
                                udp_sock.close();
                            }
                        } catch (Exception ex) {
                            //ex.printStackTrace();
                        }
                    }
                });
            }
        }, 0L, 15000L);

        if (!new File("./config.yml").exists()){
            try {
                FileWriter file = new FileWriter("./config.yml");
                PrintWriter pw = new PrintWriter(new BufferedWriter(file));
                pw.print(DefaultConfig);
                pw.close();
                file.close();
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        }

        if (!new File("./config-proxy.yml").exists()){
            try {
                FileWriter file = new FileWriter("./config-proxy.yml");
                PrintWriter pw = new PrintWriter(new BufferedWriter(file));
                pw.print(DefaultProxy);
                pw.close();
                file.close();
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        }

        int port = 25252;
        boolean isHTTP = false;
        boolean isUDP = false;

        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            isHTTP = yamlMapping.string("HTTPServer").toLowerCase(Locale.ROOT).equals("true");
            isUDP = yamlMapping.string("UDPServer").toLowerCase(Locale.ROOT).equals("true");
            port = yamlMapping.integer("Port");
        } catch (Exception e){
            // e.printStackTrace();
        }

        if (isHTTP){
            new HTTPServer(cacheAPI, proxyAPI, serverAPI, jinnnaiAPI, HttpClient, port, isStop).start();
        }

        if (isUDP){
            new UDPServer(cacheAPI, proxyAPI, serverAPI, jinnnaiAPI, HttpClient, port, isStop).start();
        }

        // 処理鯖があったらキャッシュリストを構築する
        try {
            Thread.sleep(1000L);
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            final YamlSequence list = yamlMapping.yamlSequence("ServerList");

            if (list != null){
                //System.out.println("a");
                UDPPacket check = new UDPPacket();
                check.setRequestURL("get_cache");
                Gson gson = new Gson();

                for (int x = 0; x < list.size(); x++){
                    String[] split = list.string(x).split(":");
                    String s_ip = split[0];
                    int s_port = Integer.parseInt(split[1]);

                    DatagramSocket udp_sock = null;
                    try {
                        udp_sock = new DatagramSocket();

                        byte[] textByte = gson.toJson(check).getBytes(StandardCharsets.UTF_8);
                        DatagramPacket udp_packet = new DatagramPacket(textByte, textByte.length,new InetSocketAddress(s_ip, s_port));
                        udp_sock.send(udp_packet);

                        byte[] temp1 = new byte[104857600];
                        DatagramPacket udp_packet2 = new DatagramPacket(temp1, temp1.length);
                        udp_sock.setSoTimeout(100);
                        udp_sock.receive(udp_packet2);

                        String result = new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength()));
                        //System.out.println("受信 : " + result);
                        //OutputJson json = new Gson().fromJson(result, OutputJson.class);
                        JsonElement json = gson.fromJson(result, JsonElement.class);
                        HashMap<String, CacheData> temp = new HashMap<>();
                        json.getAsJsonObject().asMap().forEach((i, value) -> temp.put(i, new CacheData(value.getAsJsonObject().get("ExpiryDate").getAsLong(), value.getAsJsonObject().get("CacheUrl").getAsString())));
                        CacheList.putAll(temp);

                        udp_sock.close();
                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            if (udp_sock != null){
                                udp_sock.close();
                            }
                        } catch (Exception ex){
                            //ex.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e){
            //e.printStackTrace();
        }

    }
}
