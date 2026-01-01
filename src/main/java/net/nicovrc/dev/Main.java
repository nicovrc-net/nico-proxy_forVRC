package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import redis.clients.jedis.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class Main {

    private static String configText = """
            # ----------------------------
            #
            # 基本設定
            #
            # ----------------------------
            # 受付ポート (HTTP)
            Port: 25252
            # ツイキャスAPIキー (https://twitcasting.tv/developerapp.php から取得可能)
            TwitcastingClientID: ""
            TwitcastingClientSecret: ""
            # Discord Webhook URL (設定しない場合は空欄)
            DiscordWebhookURL: ""
            # 画面上にURLログを流すか (流す場合はtrue)
            ViewLog: false
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
            # ニコニコアカウント設定
            # ※センシティブ設定になっている動画を見れるようにはなりますが何があっても自己責任です
            #
            # ----------------------------
            # Cookie nicosidの値
            NicoNico_nicosid: ""
            # Cookie user_sessionの値
            NicoNico_user_session: ""
            """;

    public static void main(String[] args) {

        File file = new File("./config.yml");

        if (!file.exists()){
            try {
                FileWriter file1 = new FileWriter("./config.yml");
                PrintWriter pw = new PrintWriter(new BufferedWriter(file1));
                pw.print(configText);
                pw.close();
                file1.close();
                pw = null;
                file1 = null;
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
                System.out.println("config.ymlを設定してください。\nPlease configure the config.yml file as needed.");
                return;
            }
        }

        try {

            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            Function.config.setHttpPort(yamlMapping.integer("Port"));
            Function.config.setTwitcastingClientID(yamlMapping.string("TwitcastingClientID").isEmpty() ? null : yamlMapping.string("TwitcastingClientID"));
            Function.config.setTwitcastingClientSecret(yamlMapping.string("TwitcastingClientSecret").isEmpty() ? null : yamlMapping.string("TwitcastingClientSecret"));
            Function.config.setDiscordWebhookURL(yamlMapping.string("DiscordWebhookURL").isEmpty() ? null : yamlMapping.string("DiscordWebhookURL"));
            Function.config.setViewLog(yamlMapping.bool("ViewLog"));

            Function.config.setRedisServer(yamlMapping.string("RedisServer").isEmpty() ? null : yamlMapping.string("RedisServer"));
            Function.config.setRedisPort(yamlMapping.integer("RedisPort"));
            Function.config.setRedisPass(yamlMapping.string("RedisPass").isEmpty() ? null : yamlMapping.string("RedisPass"));
            Function.config.setRedisSSL(yamlMapping.bool("RedisSSL"));

            Function.config.setNicosid(yamlMapping.string("NicoNico_nicosid"));
            Function.config.setUser_session(yamlMapping.string("NicoNico_user_session"));


        } catch (Exception e){
            // e.printStackTrace();
            Function.config.setHttpPort(25252);
            Function.config.setTwitcastingClientID(null);
            Function.config.setTwitcastingClientSecret(null);
            Function.config.setDiscordWebhookURL(null);
            Function.config.setViewLog(false);

            Function.config.setRedisServer(null);
            Function.config.setRedisPort(6379);
            Function.config.setRedisPass(null);
            Function.config.setRedisSSL(false);

            Function.config.setNicosid(null);
            Function.config.setUser_session(null);

        }

        JedisClientConfig config = null;
        if (Function.config.getRedisServer() != null){

            if (Function.config.isRedisSSL()){
                config = Function.config.getRedisPass() != null && Function.config.getRedisPass().isEmpty() ? DefaultJedisClientConfig.builder()
                        .ssl(true)
                        .build() : DefaultJedisClientConfig.builder()
                        .ssl(true)
                        .password(Function.config.getRedisPass())
                        .build();
            } else {
                config = Function.config.getRedisPass() != null && Function.config.getRedisPass().isEmpty() ? DefaultJedisClientConfig.builder()
                        .build() : DefaultJedisClientConfig.builder()
                        .password(Function.config.getRedisPass())
                        .build();
            }
        }

        System.out.println("[Info] nico-proxy_forVRC Ver "+Function.Version);
        System.out.println("[Info] URL : http://(ServerIP):"+Function.config.getHttpPort()+"/?url=(URL)");

        if (Function.config.getRedisServer() == null){
            System.out.println("[Info] Redisの設定がされていません。このままだとログを保存しません。");
        }

        if (config != null){
            try {
                RedisClient jedis = RedisClient.builder()
                        .hostAndPort(new HostAndPort(Function.config.getRedisServer(), Function.config.getRedisPort()))
                        .clientConfig(config)
                        .build();

                Function.config.setRedisClient(jedis);
            } catch (Exception e){
                System.out.println("[Error] Redisの設定が正しくないようです。");
                Function.config.setRedisClient(null);
            }
        }

        if (Function.config.getDiscordWebhookURL() == null && !Function.config.isViewLog()){
            System.out.println("[Info] ログを表示しないモード かつ DiscordのWebhookの設定がされていません。Redisの設定をしていない場合はログを見る手段がなくなります。");
        }
        if (Function.config.getTwitcastingClientID() == null || Function.config.getTwitcastingClientSecret() == null){
            System.out.println("[Info] ツイキャスの設定がされていません。このままの設定ではツイキャスが見れません。");
        }
        if (Function.config.getNicosid() == null || Function.config.getUser_session() == null){
            System.out.println("[Info] ニコニコ動画のアカウント設定がされていません。一部動画が再生できない場合があります。");
        }

        HTTPServer server = new HTTPServer();
        server.start();
        try {
            server.join();
        } catch (Exception e){
            // e.printStackTrace();
        }

        System.out.println("[Info] 終了します...");
    }

}
