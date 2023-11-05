package xyz.n7mn;


import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

import java.io.File;
import java.io.PrintWriter;

public class Main {
    private static int ResponsePort = 25252;

    public static void main(String[] args) {

        if (!new File("./config.yml").exists()){

            String str = """
                    # 受付ポート (TCP/UDP両方)
                    Port: 25252
                    # ログをRedisに書き出すときはtrue
                    LogToRedis: false

                    # ツイキャスの設定
                    # https://twitcasting.tv/developer.phpでAPIキーを取得してください
                    ClientID: ""
                    ClientSecret: ""

                    # bilibili変換システムのURL
                    BiliBliSystem: "https://b.nicovrc.net"

                    # Redisの設定(LogToRedisをtrue)にしていない場合は設定不要
                    # RedisサーバーIP
                    RedisServer: 127.0.0.1
                    # Redisサーバーポート
                    RedisPort: 6379
                    # Redis AUTHパスワード
                    # パスワードがない場合は以下の通りに設定してください
                    RedisPass: "\"""";

            try {
                new File("./config.yml").createNewFile();
                PrintWriter writer = new PrintWriter("./config.yml");
                writer.print(str);
                writer.close();
            } catch (Exception e){
                e.printStackTrace();
            }

            System.out.println("[Info] config.ymlを設定してください。");
            return;
        }

        if (!new File("./config-proxy.yml").exists()){
            try {
                new File("./config-proxy.yml").createNewFile();
                String str = """
                    # 動画取得用 (ニコ動が見れればどこでも可)
                    VideoProxy:
                      - "127.0.0.1:3128"
                    # 公式動画、生放送用 (ニコ動が見れる国内IPならどこでも可)
                    OfficialProxy:
                      - "127.0.0.1:3128\"""";

                PrintWriter writer = new PrintWriter("./config-proxy.yml");
                writer.print(str);
                writer.close();
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        }

        try {
            YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            ResponsePort = Integer.parseInt(yamlMapping.string("Port"));
        } catch (Exception e){
            System.out.println("[Error] 接続用のポート番号が正しく設定されていない可能性があります。");
            ResponsePort = 25252;
        }

        // UDP経由での受付
        Thread thread_udp = new RequestServer(ResponsePort);

        // HTTP経由での受付
        Thread thread_http = new RequestHTTPServer(ResponsePort);

        thread_udp.start();
        thread_http.start();
    }
}