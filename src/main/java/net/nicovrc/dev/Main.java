package net.nicovrc.dev;

import net.nicovrc.dev.api.CacheAPI;
import net.nicovrc.dev.api.JinnnaiSystemURL_API;
import net.nicovrc.dev.api.ProxyAPI;
import net.nicovrc.dev.api.ServerAPI;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.data.ProxyData;
import net.nicovrc.dev.data.ServerData;
import net.nicovrc.dev.server.HTTPServer;
import okhttp3.OkHttpClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
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
        }, 0L, 15000L);

        CacheCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cacheAPI.ListRefresh();
            }
        }, 0L, 5000L);

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

        new HTTPServer(cacheAPI, proxyAPI, serverAPI, jinnnaiAPI, HttpClient, 25252).start();
    }
}
