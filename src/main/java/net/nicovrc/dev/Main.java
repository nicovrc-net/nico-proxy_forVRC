package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import net.nicovrc.dev.http.*;

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


        // HTTP受付
        httpServiceList.add(new NicoVRCWebAPI());
        httpServiceList.add(new GetURL());
        httpServiceList.add(new GetURL_old1()); // v2互換用、様子見て削除
        httpServiceList.add(new GetURL_old2()); // v2互換用、様子見て削除

        TCPServer tcpServer = new TCPServer(httpServiceList);
        tcpServer.start();
        try {
            tcpServer.join();
        } catch (Exception e){
            // e.printStackTrace();
        }

    }
}
