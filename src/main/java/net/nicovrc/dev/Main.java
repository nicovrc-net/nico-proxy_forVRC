package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.JsonElement;
import net.nicovrc.dev.api.*;
import net.nicovrc.dev.data.*;
import redis.clients.jedis.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

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
# Redisにキャッシュするかどうか
CacheToRedis: false
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
# Redisの接続にSSL/TLSを使うか
RedisSSL: false
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
# Cookie nicosidの値
NicoNico_nicosid: ""
# Cookie user_sessionの値
NicoNico_user_session: ""
                    """;

        System.out.println("nico-proxy_forVRC Ver "+ Function.Version);

        if (!Function.isFoundFile("./config.yml")){
            Function.writeFile("./config.yml", config, StandardCharsets.UTF_8);

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
                    //proxyCheckTimer.cancel();
                    Function.mainTimer.cancel();
                    Function.checkTimer.cancel();
                } catch (Exception e){
                    // e.printStackTrace();
                }
                return;
            }
        }

        if (Function.getBrotliPath().isEmpty()){
            System.out.println("[Info] brotliの実行ファイルを設置してください。");
            Function.mainTimer.cancel();
            Function.checkTimer.cancel();

            return;
        }

        // API設定
        Function.APIList.add(new AddCache());
        Function.APIList.add(new GetCacheList());
        Function.APIList.add(new GetSupportList());
        Function.APIList.add(new GetVersion());
        Function.APIList.add(new GetVideoInfo());
        Function.APIList.add(new Test());

        // 設定読み込み
        String redisServer;
        String redisPass;
        int redisPort;
        boolean redisTLS = false;

        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            Function.config_httpPort = yamlMapping.integer("Port");

            Function.config_FolderPass = yamlMapping.string("LogFileFolderPass");

            Function.config_user_session = yamlMapping.string("NicoNico_user_session");
            Function.config_nicosid = yamlMapping.string("NicoNico_nicosid");


            Function.config_twitcast_ClientId = yamlMapping.string("TwitcastingClientID");
            Function.config_twitcast_ClientSecret = yamlMapping.string("TwitcastingClientSecret");

            Function.config_CacheToRedis = yamlMapping.string("CacheToRedis").equals("true");

            Function.DiscordWebhookURL = yamlMapping.string("DiscordWebhookURL");

            redisServer = yamlMapping.string("RedisServer");
            redisPass = yamlMapping.string("RedisPass");
            redisPort = yamlMapping.integer("RedisPort");
            redisTLS = yamlMapping.string("RedisSSL").equals("true");

        } catch (Exception e){
            // e.printStackTrace();
            Function.config_httpPort = 25252;

            Function.config_FolderPass = "";

            Function.config_user_session = null;
            Function.config_nicosid = null;

            Function.config_twitcast_ClientId = null;
            Function.config_twitcast_ClientSecret = null;

            Function.config_CacheToRedis = false;

            Function.DiscordWebhookURL = null;

            redisServer = "";
            redisPass = "";
            redisPort = 6379;

        }

        // ログフォルダ作成
        Function.createFolder(Function.config_FolderPass);

        JedisClientConfig redis_config = null;
        if (redisTLS) {

            SslOptions options = SslOptions.defaults();

            redis_config = redisPass != null && redisPass.isEmpty() ? DefaultJedisClientConfig.builder()
                    .sslOptions(options)
                    .build() : DefaultJedisClientConfig.builder()
                    .sslOptions(options)
                    .password(redisPass)
                    .build();
        } else {

            redis_config = redisPass != null && redisPass.isEmpty() ? DefaultJedisClientConfig.builder()
                    .build() : DefaultJedisClientConfig.builder()
                    .password(redisPass)
                    .build();

        }

        final RedisClient jedis;
        try (HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build()
        ){
            jedis = RedisClient.builder()
                    .hostAndPort(new HostAndPort(redisServer, redisPort))
                    .clientConfig(redis_config)
                    .build();
            Function.redisClient = jedis;

            // エラー動画
            if (!Function.isFoundFolder("./error-video")){
                if (Function.createFolder("./error-video")) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI("https://r2.7mi.site/vrc/nico/error_404.mp4"))
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();

                    HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (send.statusCode() >= 400) {
                        send = null;
                        request = null;
                    } else {
                        Function.writeFile("./error-video/error_404.mp4", send.body());
                    }
                    request = HttpRequest.newBuilder()
                            .uri(new URI("https://r2.7mi.site/vrc/nico/error_000.mp4"))
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (send.statusCode() >= 400) {
                        send = null;
                        request = null;
                        //client.close();
                    } else {
                        Function.writeFile("./error-video/error_000.mp4", send.body());
                    }

                    request = HttpRequest.newBuilder()
                            .uri(new URI("https://r2.7mi.site/vrc/nico/error_404_2.mp4"))
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (send.statusCode() >= 400) {
                        send = null;
                        request = null;
                    } else {
                        Function.writeFile("./error-video/error_404_2.mp4", send.body());
                    }
                }
            }

            if (Function.isFoundFile("./error-video/error_000.mp4")){
                Function.content_errorVideo_others = Function.getFileByBinary("./error-video/error_000.mp4");
            }
            if (Function.isFoundFile("./error-video/error_404.mp4")){
                Function.content_errorVideo_site = Function.getFileByBinary("./error-video/error_404.mp4");
            }
            if (Function.isFoundFile("./error-video/error_404_2.mp4")){
                Function.content_errorVideo_endLive = Function.getFileByBinary("./error-video/error_404_2.mp4");
            }
            // ログ、Webhook書き出し & キャッシュ削除
            Function.mainTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    if (!Function.config_CacheToRedis){
                        Thread.ofVirtual().start(()->{
                            HashMap<String, CacheData> map = Function.getCacheList();

                            long time = new Date().getTime();
                            map.forEach((url, data)->{

                                if (Function.CacheWaitList.get(url) == null){
                                    if (time - data.getCacheDate() >= 86400000L) {
                                        Function.deleteCache(url);
                                    }
                                }

                            });

                            map.clear();
                            map = null;
                        });
                    }

                    WriteLog(jedis);
                    try {
                        SendWebhook(client);
                    } catch (Exception e){
                        //e.printStackTrace();
                    }
                }
            }, 0L, 60000L);


            // プロキシチェック
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

                                try (HttpClient client2 = HttpClient.newBuilder()
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

                                    HttpResponse<String> send = client2.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

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

                                                    send = client2.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

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

                                try (HttpClient client2 = HttpClient.newBuilder()
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

                                    HttpResponse<String> send = client2.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

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

                                                    send = client2.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

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

            // HTTP受付
            new TCPServer(client).start(Function.config_httpPort);
            while (true) {
                if (Function.isFoundFile("./stop_lock.txt")){
                    Function.deleteFile("./stop_lock.txt");
                    break;
                }
                try {
                    Thread.sleep(100L);
                } catch (Exception ignored) {
                    //ignored.printStackTrace();
                }
            }

            // 終了処理
            //proxyCheckTimer.cancel();
            Function.mainTimer.cancel();
            Function.checkTimer.cancel();
            WriteLog(jedis);
            SendWebhook(client);
            jedis.close();
            System.out.println("[Info] 終了します...");

            if (Function.isFoundFile("./stop_lock.txt")){
                Function.deleteFile("./stop_lock.txt");
            }
            if (Function.isFoundFile("./stop.txt")){
                Function.deleteFile("./stop.txt");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (Function.redisClient != null){
                Function.redisClient.close();
            }
        }
    }

    private static void WriteLog(RedisClient jedis){

        if (!Function.GetURLAccessLog.isEmpty()){
            Thread.ofVirtual().start(()->{
                System.out.println("[Info] ログ書き出し開始");

                int[] count = {0};
                try {
                    if (jedis != null){
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
                    } else {

                        if (!Function.isFoundFolder("./log")){
                            Function.createFolder("./log");
                        }

                        HashMap<String, LogData> temp = new HashMap<>(Function.GetURLAccessLog);
                        Function.GetURLAccessLog.clear();

                        temp.forEach((id, value)->{
                            try {
                                if (!Function.isFoundFile("./log/"+id+".txt")){
                                    Function.writeFile("./log/" + id + ".txt", Function.gson.toJson(value), StandardCharsets.UTF_8);
                                } else if (Function.getFileByText("./log"+id+".txt", StandardCharsets.UTF_8).isEmpty()){
                                    Function.deleteFile("./log"+id+".txt");
                                    Function.writeFile("./log/" + id + ".txt", Function.gson.toJson(value), StandardCharsets.UTF_8);
                                } else {
                                    Function.GetURLAccessLog.put(id, value);
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
                                if (!Function.isFoundFile("./log/api_log_" + id + ".txt")){
                                    Function.writeFile("./log/api_log_" + id + ".txt", value, StandardCharsets.UTF_8);
                                } else if (Function.getFileByText("./log/api_log_"+id+".txt", StandardCharsets.UTF_8).isEmpty()){
                                    Function.deleteFile("./log/api_log_"+id+".txt");
                                    Function.writeFile("./log/api_log_"+id+".txt", value, StandardCharsets.UTF_8);
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

    private static void SendWebhook(HttpClient client) throws Exception {
        if (!Function.WebhookData.isEmpty()){
            Thread.ofVirtual().start(()-> {
                int[] count = {0};



                if (Function.DiscordWebhookURL == null || Function.DiscordWebhookURL.isEmpty()) {
                    return;
                }

                try {
                    final HashMap<String, WebhookData> temp = new HashMap<>(Function.WebhookData);
                    Function.WebhookData.clear();

                    System.out.println("[Info] Webhook送信開始");
                    final String finalWebhookURL = Function.DiscordWebhookURL;
                    temp.forEach((id, data) -> {
                        try {
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
                            fields[2].setValue("```\n" + data.getHTTPRequest() + "\n```");
                            embeds[0].setFields(fields);
                            webhookData.setEmbeds(embeds);

                            //System.out.println(Function.gson.toJson(webhookData));

                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(new URI(finalWebhookURL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Content-Type", "application/json; charset=UTF-8")
                                    .POST(HttpRequest.BodyPublishers.ofString(Function.gson.toJson(webhookData)))
                                    .build();
                            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                            request = null;
                            //System.out.println(send.body());
                            send = null;

                            count[0]++;
                            embeds = null;
                            webhookData = null;
                        } catch (Exception e){
                            throw new RuntimeException(e);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("[Info] Webhook送信完了 ("+count[0]+"件)");

            });
        }
    }
}
