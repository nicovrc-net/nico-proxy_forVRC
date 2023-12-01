package xyz.n7mn;


import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

import java.io.File;
import java.io.PrintWriter;

public class Main {

    public static void main(String[] args) {

        if (!new File("./config.yml").exists()){

            String str = """
                    # 受付ポート (HTTP/UDP共通)
                    Port: 25252
                    # ログをRedisに書き出すときはtrue
                    LogToRedis: false
                    
                    # HTTPで受付する場合はtrue
                    HTTPServer: true
                    # UDPで受付する場合はtrue
                    UDPServer: false
                    
                    # 他に処理鯖がある場合はそのリストを「IP:受付ポート」形式で記載する
                    # (HTTP受付の鯖ではUDP受付をfalseにし他の処理鯖ではHTTP受付をfalse、UDP受付をtrueにすることを推奨)
                    ServerList:
                        - "127.0.0.1:25252"

                    # ツイキャスの設定
                    # https://twitcasting.tv/developer.phpでAPIキーを取得してください
                    ClientID: ""
                    ClientSecret: ""

                    # bilibili変換システムのURL
                    BiliBliSystem: "https://b.nicovrc.net"
                    
                    # Youtube変換システムのIP
                    YoutubeSystem: "127.0.0.1"
                    
                    # ニコ動domand鯖の変換システム
                    NicoVideoSystem: "127.0.0.1"

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

        int ResponsePort;
        boolean UDPServer;
        boolean HTTPServer;
        try {
            YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            ResponsePort = Integer.parseInt(yamlMapping.string("Port"));
        } catch (Exception e){
            System.out.println("[Error] 接続用のポート番号が正しく設定されていない可能性があります。");
            ResponsePort = 25252;
        }

        try {
            YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            //System.out.println(yamlMapping.string("UDPServer"));
            UDPServer = yamlMapping.string("UDPServer").equals("true");
            //System.out.println(UDPServer);
            HTTPServer = yamlMapping.string("HTTPServer").equals("true");
        } catch (Exception e){
            e.printStackTrace();
            UDPServer = false;
            HTTPServer = true;
        }

        // UDP経由での受付
        if (UDPServer){
            Thread thread_udp = new RequestServer(ResponsePort);
            thread_udp.start();
        }

        // HTTP経由での受付
        if (HTTPServer){
            Thread thread_http = new RequestHTTPServer(ResponsePort);
            thread_http.start();
        }

    }
}