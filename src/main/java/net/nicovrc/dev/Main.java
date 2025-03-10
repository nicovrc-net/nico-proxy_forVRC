package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Service.Result.NicoNicoVideo;
import net.nicovrc.dev.Service.Result.TikTokResult;
import net.nicovrc.dev.http.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
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

    private static final List<NicoVRCHTTP> httpServiceList = new ArrayList<>();
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

                                    HttpClient client = HttpClient.newBuilder()
                                            .version(HttpClient.Version.HTTP_2)
                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                                            .build();

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

                                    client.close();
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

                                    HttpClient client = HttpClient.newBuilder()
                                            .version(HttpClient.Version.HTTP_2)
                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                                            .build();

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

                                    client.close();
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

        // ログ書き出し
        logWriteTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                WriteLog();
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

                        JsonElement json = Function.gson.fromJson(data.getResultJson(), JsonElement.class);

                        if (time - data.getCacheDate() >= 86400000L) {
                            Function.CacheList.remove(url);
                        } else if (!json.getAsJsonObject().has("VideoURL") && !json.getAsJsonObject().has("LiveURL") && !json.getAsJsonObject().has("AudioURL")){
                            Function.CacheList.remove(url);
                        } else if (data.isSet()) {
                            // Proxy
                            String Proxy = "";
                            if (!Function.JP_ProxyList.isEmpty()){
                                int i = new SecureRandom().nextInt(0, Function.JP_ProxyList.size());
                                Proxy = Function.JP_ProxyList.get(i);
                            }
                            String[] s = Proxy.split(":");

                            HttpClient client = HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NORMAL)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                                    .build();

                            try {
                                if (data.getServiceAPI().getServiceName().equals("ニコニコ")){

                                    NicoNicoVideo video = Function.gson.fromJson(data.getResultJson(), NicoNicoVideo.class);

                                    final StringBuilder sb = new StringBuilder();
                                    if (video.getVideoAccessCookie() != null && !video.getVideoAccessCookie().isEmpty()){
                                        video.getVideoAccessCookie().forEach((name, cookie) -> {
                                            sb.append(name).append("=").append(cookie).append(";");
                                        });
                                    }

                                    HttpRequest request = HttpRequest.newBuilder()
                                            .uri(new URI(video.getVideoURL() != null ? video.getVideoURL() : video.getLiveURL()))
                                            .headers("User-Agent", Function.UserAgent)
                                            .headers("Cookie", sb.isEmpty() ? "" : sb.substring(0, sb.length() - 1))
                                            .GET()
                                            .build();

                                    HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                                    if (send.statusCode() >= 400){
                                        Function.CacheList.remove(url);
                                    }

                                } else if (data.getServiceAPI().getServiceName().equals("TikTok")){

                                    TikTokResult video = Function.gson.fromJson(data.getResultJson(), TikTokResult.class);

                                    final StringBuilder sb = new StringBuilder();
                                    if (video.getVideoAccessCookie() != null && !video.getVideoAccessCookie().isEmpty()){
                                        sb.append(video.getVideoAccessCookie());
                                    }

                                    HttpRequest request = HttpRequest.newBuilder()
                                            .uri(new URI(video.getVideoURL()))
                                            .headers("User-Agent", Function.UserAgent)
                                            .headers("Cookie", sb.isEmpty() ? "" : sb.substring(0, sb.length() - 1))
                                            .GET()
                                            .build();

                                    HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                                    if (send.statusCode() >= 400){
                                        Function.CacheList.remove(url);
                                    }

                                } else {
                                    JsonElement video = Function.gson.fromJson(data.getResultJson(), JsonElement.class);
                                    String target = video.getAsJsonObject().has("VideoURL") ? video.getAsJsonObject().get("VideoURL").getAsString() : video.getAsJsonObject().has("AudioURL") ? video.getAsJsonObject().get("AudioURL").getAsString() : video.getAsJsonObject().get("LiveURL").getAsString();

                                    HttpRequest request = HttpRequest.newBuilder()
                                            .uri(new URI(target))
                                            .headers("User-Agent", Function.UserAgent)
                                            .headers("Referer", url)
                                            .GET()
                                            .build();

                                    HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                                    if (send.statusCode() >= 400){
                                        Function.CacheList.remove(url);
                                    }
                                }
                            } catch (Exception e){
                                //e.printStackTrace();
                            }

                            client.close();
                        }
                    });

                    map.clear();
                    map = null;
                });
            }
        }, 0L, 60000L);

        // HTTP受付
        httpServiceList.add(new NicoVRCWebAPI());
        httpServiceList.add(new GetURL());
        httpServiceList.add(new GetURL_old1()); // v2互換用、様子見て削除
        httpServiceList.add(new GetURL_old2()); // v2互換用、様子見て削除
        httpServiceList.add(new GetVideo());

        TCPServer tcpServer = new TCPServer(httpServiceList);
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
    }

    private static void WriteLog(){

        if (!Function.GetURLAccessLog.isEmpty()){
            Thread.ofVirtual().start(()->{
                System.out.println("[Info] ログ書き出し開始");

                int[] count = {0};
                try {
                    YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();

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
                            jedis.set("nicovrc:log:"+id, Function.gson.toJson(value));
                        });
                        count[0] = temp.size();
                        temp.clear();
                    } catch (Exception e){
                        // e.printStackTrace();
                    }
                } catch (Exception e){
                    // e.printStackTrace();
                }


                System.out.println("[Info] ログ書き出し完了 ("+count[0]+"件)");
            });
        }

    }
}
