package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import net.nicovrc.dev.http.*;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final List<NicoVRCHTTP> httpServiceList = new ArrayList<>();

    public static void main(String[] args) {

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
# 国内判定されるプロキシ (TVerとかで使う用)
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

    }
}
