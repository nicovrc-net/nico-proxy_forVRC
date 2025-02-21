package net.nicovrc.dev;

import net.nicovrc.dev.http.NicoVRCWebAPI;
import net.nicovrc.dev.http.NicoVRCHTTP;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
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
# 受付ポート (HTTP/UDP共通)
Port: 25252
# ログをRedisに書き出すときはtrue
LogToRedis: false
# (Redis使わない場合)ログの保存先
LogFileFolderPass: "./log"
# ----------------------------
#
# Redis設定
# ※ログをRedisに保存する際に使う
#
# ----------------------------
# Redisサーバー
RedisServer: "127.0.0.1"
# Redisサーバーのポート
RedisPort: 6379
# Redis AUTHパスワード
# パスワードがない場合は以下の通りに設定してください
RedisPass: ""
                    """;

        File file1 = new File("./config.yml");
        if (!file1.exists()){
            try {
                FileWriter file = new FileWriter("./config.yml");
                PrintWriter pw = new PrintWriter(new BufferedWriter(file));
                pw.print(config);
                pw.close();
                file.close();
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


        // HTTP受付
        httpServiceList.add(new NicoVRCWebAPI());

        TCPServer tcpServer = new TCPServer(httpServiceList);
        tcpServer.start();
        try {
            tcpServer.join();
        } catch (Exception e){
            // e.printStackTrace();
        }

    }
}
