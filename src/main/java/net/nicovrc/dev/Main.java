package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.JsonElement;
import net.nicovrc.dev.data.*;
import net.nicovrc.dev.http.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Timer proxyCheckTimer = new Timer();
    private static final Timer logWriteTimer = new Timer();
    private static final Timer cacheRemoveTimer = new Timer();

    private static final Pattern matcher_Json = Pattern.compile("<meta name=\"server-response\" content=\"\\{(.+)}\" />");

    public static void main(String[] args) {

        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Connection,Host");

        // config
        String config = """
# ----------------------------
#
# 基本設定
#
# ----------------------------
# 受付ポート (HTTP)
Port: 25252
# ログをRedisに書き出すときはtrue
LogToRedis: false
# (Redis使わない場合)ログの保存先
LogFileFolderPass: "./log"
# ツイキャスAPIキー (https://twitcasting.tv/developerapp.php から取得可能)
TwitcastingClientID: ""
TwitcastingClientSecret: ""
# Discord Webhook URL (設定しない場合は空欄)
DiscordWebhookURL: ""
# ----------------------------
#
# Redis設定
#
# ----------------------------
# Redisサーバー
RedisServer: "127.0.0.1"
# Redisサーバーのポート
RedisPort: 6379
# Redis AUTHパスワード
# パスワードがない場合は以下の通りに設定してください
RedisPass: ""
# ----------------------------
#
# Proxy設定
#
# ----------------------------
# 動画取得プロキシ (ニコ動が見れればどこでも可)
VideoProxy:
  - "127.0.0.1:3128"
# 日本国内判定されるプロキシ (TVer、Abemaなど日本国内のみしか使えないサイトへ使う用)
JPProxy:
  - "127.0.0.1:3128"
# ----------------------------
#
# ニコニコアカウント設定
# ※センシティブ設定になっている動画を見れるようにはなりますが何があっても自己責任です
#
# ----------------------------
# Cookie user_sessionの値
NicoNico_user_session: ""
# Cookie user_session_secureの値
NicoNico_user_session_secure: ""
                    """;

        File file1 = new File("./config.yml");
        if (!file1.exists()){
            file1 = null;
            try {
                FileWriter file = new FileWriter("./config.yml");
                PrintWriter pw = new PrintWriter(new BufferedWriter(file));
                pw.print(config);
                pw.close();
                file.close();
                pw = null;
                file = null;
            } catch (Exception e){
                //e.printStackTrace();
            }

            boolean isError = true;
            for (String arg : args) {
                if (arg.equals("--default-config-mode")) {
                    isError = false;
                }
            }

            if (isError){
                System.out.println("[Info] config.ymlを設定してください。");
                // 終了処理
                try {
                    proxyCheckTimer.cancel();
                    logWriteTimer.cancel();
                    cacheRemoveTimer.cancel();
                } catch (Exception e){
                    // e.printStackTrace();
                }
                return;
            }
        }
        file1 = null;
        // 設定読み込み
        String FolderPass = "";
        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            FolderPass = yamlMapping.string("LogFileFolderPass");
        } catch (Exception e){
            // e.printStackTrace();
            FolderPass = "";
        }

        // ログフォルダ作成
        File file = new File(FolderPass);
        if (!FolderPass.isEmpty() && !file.exists()){
            boolean mkdir = file.mkdir();
        }

        // エラー動画
        file = new File("./error-video");
        if (!file.exists()){
            if (file.mkdir()) {
                URI uri;
                HttpRequest request;
                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
                try {
                    uri = new URI("https://r2.7mi.site/vrc/nico/error_404.mp4");
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();

                    HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (send.statusCode() >= 400) {
                        send = null;
                        uri = null;
                        request = null;
                        client.close();
                        client = null;
                    } else {
                        FileOutputStream stream = new FileOutputStream("./error-video/error_404.mp4");
                        stream.write(send.body());
                        stream.close();
                        stream = null;
                    }

                    uri = new URI("https://r2.7mi.site/vrc/nico/error_000.mp4");
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (send.statusCode() >= 400) {
                        send = null;
                        uri = null;
                        request = null;
                        client.close();
                        client = null;
                    } else {
                        FileOutputStream stream = new FileOutputStream("./error-video/error_000.mp4");
                        stream.write(send.body());
                        stream.close();
                        stream = null;
                    }

                    uri = new URI("https://r2.7mi.site/vrc/nico/error_404_2.mp4");
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (send.statusCode() >= 400) {
                        send = null;
                        uri = null;
                        request = null;
                        client.close();
                        client = null;
                    } else {
                        FileOutputStream stream = new FileOutputStream("./error-video/error_404_2.mp4");
                        stream.write(send.body());
                        stream.close();
                        stream = null;
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                } finally {
                    uri = null;
                    request = null;
                    client.close();
                    client = null;
                }
            }
        }

        // プロキシチェック
        proxyCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                try {
                    YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();

                    YamlSequence proxy1 = yamlMapping.yamlSequence("VideoProxy");
                    YamlSequence proxy2 = yamlMapping.yamlSequence("JPProxy");

                    Thread.ofVirtual().start(()->{
                        long count = Function.ProxyList.size();
                        try {
                            if (proxy1 != null){
                                List<String> proxyList = new ArrayList<>();

                                for (int i = 0; i < proxy1.size(); i++){

                                    String[] s = proxy1.string(i).split(":");

                                    try (HttpClient client = HttpClient.newBuilder()
                                            .version(HttpClient.Version.HTTP_2)
                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                                            .build()) {

                                        HttpRequest request = HttpRequest.newBuilder()
                                                .uri(new URI("https://www.nicovideo.jp/watch/sm9"))
                                                .headers("User-Agent", Function.UserAgent)
                                                .GET()
                                                .build();

                                        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                                        if (send.statusCode() < 400){
                                            Matcher matcher = matcher_Json.matcher(send.body());
                                            if (matcher.find()){
                                                JsonElement json = Function.gson.fromJson("{"+matcher.group(1).replaceAll("&quot;", "\"")+"}", JsonElement.class);

                                                if (json != null){

                                                    if (json.getAsJsonObject().has("data") && json.getAsJsonObject().getAsJsonObject("data").has("response")){
                                                        String nicosid = json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("client").getAsJsonObject().get("nicosid").getAsString();
                                                        String accessRightKey = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").get("accessRightKey").getAsString();
                                                        String trackId = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("client").get("watchTrackId").getAsString();

                                                        request = HttpRequest.newBuilder()
                                                                .uri(new URI("https://nvapi.nicovideo.jp/v1/watch/sm9/access-rights/hls?actionTrackId="+trackId))
                                                                .headers("Access-Control-Request-Headers", "content-type,x-access-right-key,x-frontend-id,x-frontend-version,x-niconico-language,x-request-with")
                                                                .headers("X-Access-Right-Key", accessRightKey)
                                                                .headers("X-Frontend-Id", "6")
                                                                .headers("X-Frontend-Version", "0")
                                                                .headers("X-Niconico-Language", "ja-jp")
                                                                .headers("X-Request-With", "nicovideo")
                                                                .headers("Cookie", "nicosid="+nicosid)
                                                                .headers("User-Agent", Function.UserAgent)
                                                                .POST(HttpRequest.BodyPublishers.ofString("{\"outputs\":[[\"video-h264-360p\",\"audio-aac-128kbps\"],[\"video-h264-360p-lowest\",\"audio-aac-128kbps\"]]}"))
                                                                .build();

                                                        send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                                                        if (send.statusCode() < 400){
                                                            proxyList.add(proxy1.string(i));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e){
                                        // e.printStackTrace();
                                    }
                                }

                                Function.ProxyList.clear();
                                Function.ProxyList.addAll(proxyList);
                            }
                        } catch (Exception e){
                            //e.printStackTrace();
                        }
                        if (count != Function.ProxyList.size()){
                            System.out.println("[Info] VideoProxy: "+ Function.ProxyList.size() + "件");
                        }
                    });

                    Thread.ofVirtual().start(()->{
                        long count = Function.JP_ProxyList.size();
                        try {
                            if (proxy2 != null){
                                List<String> proxyList = new ArrayList<>();

                                for (int i = 0; i < proxy2.size(); i++){

                                    String[] s = proxy2.string(i).split(":");

                                    try (HttpClient client = HttpClient.newBuilder()
                                            .version(HttpClient.Version.HTTP_2)
                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                                            .build()){
                                        HttpRequest request = HttpRequest.newBuilder()
                                                .uri(new URI("https://www.nicovideo.jp/watch/so38016254"))
                                                .headers("User-Agent", Function.UserAgent)
                                                .GET()
                                                .build();

                                        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                                        if (send.statusCode() < 400){
                                            Matcher matcher = matcher_Json.matcher(send.body());
                                            if (matcher.find()){
                                                JsonElement json = Function.gson.fromJson("{"+matcher.group(1).replaceAll("&quot;", "\"")+"}", JsonElement.class);

                                                if (json != null){

                                                    if (json.getAsJsonObject().has("data") && json.getAsJsonObject().getAsJsonObject("data").has("response")){
                                                        String nicosid = json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("client").getAsJsonObject().get("nicosid").getAsString();
                                                        String accessRightKey = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").get("accessRightKey").getAsString();
                                                        String trackId = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("client").get("watchTrackId").getAsString();

                                                        request = HttpRequest.newBuilder()
                                                                .uri(new URI("https://nvapi.nicovideo.jp/v1/watch/so38016254/access-rights/hls?actionTrackId="+trackId))
                                                                .headers("Access-Control-Request-Headers", "content-type,x-access-right-key,x-frontend-id,x-frontend-version,x-niconico-language,x-request-with")
                                                                .headers("X-Access-Right-Key", accessRightKey)
                                                                .headers("X-Frontend-Id", "6")
                                                                .headers("X-Frontend-Version", "0")
                                                                .headers("X-Niconico-Language", "ja-jp")
                                                                .headers("X-Request-With", "nicovideo")
                                                                .headers("Cookie", "nicosid="+nicosid)
                                                                .headers("User-Agent", Function.UserAgent)
                                                                .POST(HttpRequest.BodyPublishers.ofString("{\"outputs\":[[\"video-h264-720p\",\"audio-aac-192kbps\"]],\"heartbeat\":{\"method\":\"guest\",\"params\":{\"eventType\":\"start\",\"eventOccurredAt\":\"2025-03-05T21:05:38+09:00\",\"watchMilliseconds\":0,\"endCount\":0,\"additionalParameters\":{\"___pc_v\":1,\"os\":\"\",\"os_version\":\"\",\"nicosid\":\"1741103555.1678032013\",\"referer\":\"\",\"query_parameters\":{},\"is_ad_block\":false,\"has_playlist\":false,\"___abw\":null,\"abw_show\":false,\"abw_closed\":false,\"abw_seen_at\":null,\"viewing_source\":\"\",\"viewing_source_detail\":{},\"playback_rate\":\"\",\"use_flip\":false,\"quality\":[],\"auto_quality\":[],\"loop_count\":0,\"suspend_count\":0,\"load_failed\":false,\"error_description\":[],\"end_position_milliseconds\":null,\"performance\":{\"watch_access_start\":1741176338952,\"watch_access_finish\":null,\"video_loading_start\":1741176338959,\"video_loading_finish\":null,\"video_play_start\":null,\"end_context\":{\"ad_playing\":false,\"video_playing\":false,\"is_suspending\":false}}}}}}"))
                                                                .build();

                                                        send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                                                        if (send.statusCode() < 400){
                                                            proxyList.add(proxy2.string(i));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e){
                                        // e.printStackTrace();
                                    }
                                }

                                Function.JP_ProxyList.clear();
                                Function.JP_ProxyList.addAll(proxyList);
                            }
                        } catch (Exception e){
                            //e.printStackTrace();
                        }
                        if (count != Function.JP_ProxyList.size()){
                            System.out.println("[Info] JP ProxyList: "+ Function.JP_ProxyList.size() + "件");
                        }
                    });

                } catch (Exception e){
                    // e.printStackTrace();
                }

            }
        }, 0L, 60000L);

        // ログ、Webhook書き出し
        logWriteTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                WriteLog();
                SendWebhook();
            }
        }, 0L, 60000L);

        // キャッシュ掃除
        cacheRemoveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Thread.ofVirtual().start(()->{
                    HashMap<String, CacheData> map = new HashMap<>(Function.CacheList);

                    long time = new Date().getTime();
                    map.forEach((url, data)->{

                        if (data.isSet() && time - data.getCacheDate() >= 86400000L) {
                            Function.CacheList.remove(url);
                        }


                        boolean isFound = true;
                        try (HttpClient client = data.getProxy() == null || data.getProxy().split(":").length != 2 ? HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofSeconds(5))
                                .build()
                                :
                                HttpClient.newBuilder()
                                        .version(HttpClient.Version.HTTP_2)
                                        .followRedirects(HttpClient.Redirect.NORMAL)
                                        .connectTimeout(Duration.ofSeconds(5))
                                        .proxy(ProxySelector.of(new InetSocketAddress(data.getProxy().split(":")[0], Integer.parseInt(data.getProxy().split(":")[1]))))
                                        .build()
                        ) {

                            HttpRequest request = null;
                            if (data.getCookieText() == null && data.getRefererText() == null){
                                request = HttpRequest.newBuilder()
                                        .uri(new URI(url))
                                        .headers("User-Agent", Function.UserAgent)
                                        .GET()
                                        .build();
                            } else if (data.getCookieText() != null && data.getRefererText() == null){
                                request = HttpRequest.newBuilder()
                                        .uri(new URI(url))
                                        .headers("User-Agent", Function.UserAgent)
                                        .headers("Cookie", data.getCookieText())
                                        .GET()
                                        .build();
                            } else if (data.getCookieText() == null && data.getRefererText() != null){
                                request = HttpRequest.newBuilder()
                                        .uri(new URI(url))
                                        .headers("User-Agent", Function.UserAgent)
                                        .headers("Referer", data.getRefererText())
                                        .GET()
                                        .build();
                            } else if (data.getCookieText() != null && data.getRefererText() != null){
                                request = HttpRequest.newBuilder()
                                        .uri(new URI(url))
                                        .headers("User-Agent", Function.UserAgent)
                                        .headers("Cookie", data.getCookieText())
                                        .headers("Referer", data.getRefererText())
                                        .GET()
                                        .build();
                            }

                            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                            if (send.statusCode() >= 300 && send.statusCode() <= 199){
                                isFound = false;
                            }

                        } catch (Exception e){
                            //e.printStackTrace();
                        }

                        if (!isFound){
                            Function.CacheList.remove(url);
                        }

                    });

                    map.clear();
                    map = null;
                });
            }
        }, 0L, 60000L);

        // HTTP受付
        Function.httpServiceList.add(new NicoVRCWebAPI());
        Function.httpServiceList.add(new GetURL());
        Function.httpServiceList.add(new GetURL_dummy());
        Function.httpServiceList.add(new GetURL_dummy2()); // Quest/Pico用
        Function.httpServiceList.add(new GetURL_old1()); // v2互換用、様子見て削除
        Function.httpServiceList.add(new GetURL_old2()); // v2互換用、様子見て削除
        Function.httpServiceList.add(new GetVideo());

        TCPServer tcpServer = new TCPServer();
        tcpServer.start();
        try {
            tcpServer.join();
        } catch (Exception e){
            // e.printStackTrace();
        }

        // 終了処理
        proxyCheckTimer.cancel();
        logWriteTimer.cancel();
        cacheRemoveTimer.cancel();
        WriteLog();
        SendWebhook();
        System.out.println("[Info] 終了します...");

        File file2 = new File("./stop_lock.txt");
        if (file2.exists()){
            file2.deleteOnExit();
        }
        File file3 = new File("./stop.txt");
        if (file3.exists()){
            file3.deleteOnExit();
        }
    }

    private static void WriteLog(){

        if (!Function.GetURLAccessLog.isEmpty()){
            Thread.ofVirtual().start(()->{
                System.out.println("[Info] ログ書き出し開始");

                int[] count = {0};
                try {
                    final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();

                    if (yamlMapping.string("LogToRedis").toLowerCase(Locale.ROOT).equals("true")){
                        String redisServer = yamlMapping.string("RedisServer");
                        String redisPass = yamlMapping.string("RedisPass");
                        int redisPort = yamlMapping.integer("RedisPort");

                        try (JedisPool jedisPool = new JedisPool(redisServer, redisPort);
                             Jedis jedis = jedisPool.getResource()){

                            if (!redisPass.isEmpty()){
                                jedis.auth(redisPass);
                            }

                            HashMap<String, LogData> temp = new HashMap<>(Function.GetURLAccessLog);
                            Function.GetURLAccessLog.clear();

                            temp.forEach((id, value)->{
                                try {
                                    jedis.set("nicovrc:access_log:"+id, Function.gson.toJson(value));
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                    Function.GetURLAccessLog.put(id, value);
                                }
                            });
                            count[0] = temp.size();
                            temp.clear();
                            temp = null;

                            HashMap<String, String> temp2 = new HashMap<>(Function.APIAccessLog);
                            Function.APIAccessLog.clear();

                            temp2.forEach((id, value)->{
                                try {
                                    jedis.set("nicovrc:api_log:"+id, value);
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                    Function.APIAccessLog.put(id, value);
                                }
                            });
                            count[0] = count[0] + temp2.size();
                            temp2.clear();
                            temp2 = null;

                        } catch (Exception e){
                            // e.printStackTrace();
                        }
                        redisServer = null;
                        redisPass = null;
                    } else {

                        File file = new File("./log");

                        if (!file.exists()){
                            file.mkdir();
                        }
                        file = null;

                        HashMap<String, LogData> temp = new HashMap<>(Function.GetURLAccessLog);
                        Function.GetURLAccessLog.clear();

                        temp.forEach((id, value)->{
                            try {
                                File file2 = new File("./log/" + id + ".txt");
                                if (!file2.exists()){
                                    PrintWriter writer = new PrintWriter(file2);
                                    writer.print(Function.gson.toJson(value));
                                    writer.close();
                                    writer = null;
                                } else if (file2.length() == 0){
                                    file2.delete();
                                    PrintWriter writer = new PrintWriter(file2);
                                    writer.print(Function.gson.toJson(value));
                                    writer.close();
                                    writer = null;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Function.GetURLAccessLog.put(id, value);
                            }
                        });
                        count[0] = temp.size();
                        temp.clear();
                        temp = null;

                        HashMap<String, String> temp2 = new HashMap<>(Function.APIAccessLog);
                        Function.APIAccessLog.clear();

                        temp2.forEach((id, value)->{
                            try {
                                File file2 = new File("./log/api_log_" + id + ".txt");
                                if (!file2.exists()){
                                    PrintWriter writer = new PrintWriter(file2);
                                    writer.print(value);
                                    writer.close();
                                    writer = null;
                                } else if (file2.length() == 0){
                                    file2.delete();
                                    PrintWriter writer = new PrintWriter(file2);
                                    writer.print(value);
                                    writer.close();
                                    writer = null;
                                }
                            } catch (Exception e) {
                                //e.printStackTrace();
                                Function.APIAccessLog.put(id, value);
                            }
                        });

                        count[0] = count[0] + temp2.size();
                        temp2.clear();
                        temp2 = null;

                    }
                } catch (Exception e){
                    // e.printStackTrace();
                }


                System.out.println("[Info] ログ書き出し完了 ("+count[0]+"件)");
            });
        }

    }

    private static void SendWebhook(){
        if (!Function.WebhookData.isEmpty()){
            Thread.ofVirtual().start(()->{
                int[] count = {0};

                String WebhookURL = "";
                try {
                    final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                    WebhookURL = yamlMapping.string("DiscordWebhookURL");
                } catch (Exception e){
                    WebhookURL = "";
                }

                if (WebhookURL.isEmpty()){
                    return;
                }

                final List<String> proxyList = new ArrayList<>();
                proxyList.addAll(Function.ProxyList);
                proxyList.addAll(Function.JP_ProxyList);

                final HashMap<String, WebhookData> temp = new HashMap<>(Function.WebhookData);
                Function.WebhookData.clear();

                System.out.println("[Info] Webhook送信開始");
                final String finalWebhookURL = WebhookURL;
                temp.forEach((id, data) -> {

                    int i = new SecureRandom().nextInt(0, proxyList.size());
                    String proxy = null;
                    if (!proxyList.isEmpty()){
                        proxy = proxyList.get(i);
                    }

                    SendWebhookData webhookData = new SendWebhookData();
                    webhookData.setUsername("nico-proxy_forVRC (Ver " + Function.Version + ")");
                    webhookData.setAvatar_url("https://r2.7mi.site/vrc/nico/nc296562.png");
                    webhookData.setContent("利用ログ");
                    WebhookEmbeds[] embeds = {new WebhookEmbeds()};
                    embeds[0].setTitle(data.getAPIURI() == null || data.getAPIURI().isEmpty() ? "変換URL利用ログ" : "API利用ログ");
                    embeds[0].setDescription(Function.sdf.format(data.getDate()));
                    WebhookFields[] fields = {new WebhookFields(), new WebhookFields(), new WebhookFields()};
                    fields[0].setName("リクエストURL");
                    fields[0].setValue(data.getURL() == null || data.getURL().isEmpty() ? data.getAPIURI() : data.getURL());
                    fields[1].setName("処理結果");
                    fields[1].setValue(data.getResult() == null ? "(なし)" : data.getResult());
                    fields[2].setName("HTTP Request");
                    fields[2].setValue("```\n"+data.getHTTPRequest()+"\n```");
                    embeds[0].setFields(fields);
                    webhookData.setEmbeds(embeds);

                    //System.out.println(Function.gson.toJson(webhookData));

                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NORMAL)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()){

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(finalWebhookURL))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Content-Type", "application/json; charset=UTF-8")
                                .POST(HttpRequest.BodyPublishers.ofString(Function.gson.toJson(webhookData)))
                                .build();
                        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        send = null;
                        request = null;
                        //System.out.println(send.body());

                        count[0]++;
                    } catch (Exception e){
                        // e.printStackTrace();
                    }
                    embeds = null;
                    webhookData = null;
                });
                System.out.println("[Info] Webhook送信完了 ("+count[0]+"件)");

            });
        }
    }
}
