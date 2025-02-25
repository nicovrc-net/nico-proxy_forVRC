package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.JsonElement;
import net.nicovrc.dev.http.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final List<NicoVRCHTTP> httpServiceList = new ArrayList<>();
    private static final Timer proxyCheckTimer = new Timer();

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
                proxyCheckTimer.cancel();
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
                                            JsonElement json = Function.gson.fromJson(matcher.group(1).replaceAll("&quot;", "\""), JsonElement.class);

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
                            // e.printStackTrace();
                        }
                    });

                    Thread.ofVirtual().start(()->{
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
                                            JsonElement json = Function.gson.fromJson(matcher.group(1).replaceAll("&quot;", "\""), JsonElement.class);

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
                                                            .POST(HttpRequest.BodyPublishers.ofString("{\"outputs\":[[\"video-h264-360p\",\"audio-aac-128kbps\"],[\"video-h264-360p-lowest\",\"audio-aac-128kbps\"]]}"))
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
                            // e.printStackTrace();
                        }
                    });

                } catch (Exception e){
                    // e.printStackTrace();
                }

            }
        }, 0L, 10000L);

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
    }
}
