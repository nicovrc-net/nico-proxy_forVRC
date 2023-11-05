package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import okhttp3.*;
import redis.clients.jedis.Protocol;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;


public class Main {
    private static int ResponsePort = 25252;
    private static int PingPort = 25253;
    private static int PingHTTPPort = 25280;
    private static String Master = "-:22552";
    private static final ConcurrentHashMap<String, String> QueueList = new ConcurrentHashMap<>();
    private static boolean logToRedis = false;
    private static String bilibiliSystemURL = "http://localhost:28280";

    private static String twitcastClientId = "";
    private static String twitcastClientSecret = "";

    public static void main(String[] args) {
        // Proxy読み込み
        File config1 = new File("./config.yml");
        File config2 = new File("./config-proxy.yml");
        File config3 = new File("./config-redis.yml");
        File config4 = new File("./config-twitcast.yml");

        YamlMapping ConfigYaml1 = null;
        YamlMapping ConfigYaml2 = null;
        YamlMapping ConfigYaml3 = null;
        YamlMapping ConfigYaml4 = null;

        if (!config1.exists()){
            YamlMappingBuilder add = Yaml.createYamlMappingBuilder()
                    .add("Port", String.valueOf(ResponsePort))
                    .add("PingPort", String.valueOf(PingPort))
                    .add("PingHTTPPort", String.valueOf(PingHTTPPort))
                    .add("Master", "-:22552")
                    .add("LogToRedis", "False")
                    .add("bilibiliSystemURL", bilibiliSystemURL);
            ConfigYaml1 = add.build();

            try {
                config1.createNewFile();
                PrintWriter writer = new PrintWriter(config1);
                writer.print(ConfigYaml1.toString());
                writer.close();

                System.out.println("[Info] config.ymlを設定してください。");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            try {
                ConfigYaml1 = Yaml.createYamlInput(config1).readYamlMapping();
                ResponsePort = ConfigYaml1.integer("Port");
                PingPort = ConfigYaml1.integer("PingPort");
                PingHTTPPort = ConfigYaml1.integer("PingHTTPPort");
                Master = ConfigYaml1.string("Master");
                logToRedis = ConfigYaml1.string("LogToRedis").equals("True");
                bilibiliSystemURL = ConfigYaml1.string("bilibiliSystemURL");
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        }

        if (!config2.exists()){
            YamlMappingBuilder add = Yaml.createYamlMappingBuilder()
                    .add("VideoProxy", Yaml.createYamlSequenceBuilder().add("localhost:3128").add("127.0.0.1:3128").build())
                    .add("OfficialProxy", Yaml.createYamlSequenceBuilder().add("localhost:3128").add("127.0.0.1:3128").build());
            ConfigYaml2 = add.build();

            try {
                config2.createNewFile();
                PrintWriter writer = new PrintWriter(config2);
                writer.print(ConfigYaml2.toString());
                writer.close();

                //System.out.println("[Error] ProxyList is Empty!!");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            // Proxyチェック
            try {
                YamlMapping yamlMapping = Yaml.createYamlInput(config2).readYamlMapping();
                System.out.println("[Info] プロキシチェック中...");
                YamlSequence list = yamlMapping.yamlSequence("VideoProxy");

                final OkHttpClient.Builder builder = new OkHttpClient.Builder();

                if (list != null){
                    for (int i = 0; i < list.size(); i++){
                        String[] s = list.string(i).split(":");
                        try {
                            OkHttpClient build = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(s[0], Integer.parseInt(s[1])))).build();
                            Request request_html = new Request.Builder()
                                    .url("https://www.google.co.jp/")
                                    .build();
                            Response response = build.newCall(request_html).execute();
                            response.close();
                        } catch (Exception e){
                            System.out.println("[Info] "+s[0]+":"+s[1]+" 接続失敗");
                            continue;
                        }
                        System.out.println("[Info] "+s[0]+":"+s[1]+" 接続成功");
                    }
                }

                list = yamlMapping.yamlSequence("OfficialProxy");
                if (list != null){
                    for (int i = 0; i < list.size(); i++){
                        String[] s = list.string(i).split(":");
                        try {
                            OkHttpClient build = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(s[0], Integer.parseInt(s[1])))).build();
                            Request request_html = new Request.Builder()
                                    .url("https://www.google.co.jp/")
                                    .build();
                            Response response = build.newCall(request_html).execute();
                            response.close();
                        } catch (Exception e){
                            System.out.println("[Info] "+s[0]+":"+s[1]+" 接続失敗");
                            continue;
                        }
                        System.out.println("[Info] "+s[0]+":"+s[1]+" 接続成功");
                    }
                }
                System.out.println("[Info] プロキシチェック完了");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!config3.exists()){
            YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
            ConfigYaml3 = builder.add(
                    "RedisServer", "127.0.0.1"
            ).add(
                    "RedisPort", String.valueOf(Protocol.DEFAULT_PORT)
            ).add(
                    "RedisPass", ""
            ).build();

            try {
                config3.createNewFile();
                PrintWriter writer = new PrintWriter(config3);
                writer.print(ConfigYaml3.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!config4.exists()){
            YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
            ConfigYaml4 = builder.add(
                    "ClientID", "xxxx"
            ).add(
                    "ClientSecret", "xxxx"
            ).build();

            try {
                config4.createNewFile();
                PrintWriter writer = new PrintWriter(config4);
                writer.print(ConfigYaml4.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ConfigYaml4 = Yaml.createYamlInput(config4).readYamlMapping();
                twitcastClientId = ConfigYaml4.string("ClientID");
                twitcastClientSecret = ConfigYaml4.string("ClientSecret");
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        // TCP死活管理用
        Thread thread_tcpCheck = new CheckTCPServer(PingPort);

        // HTTP死活管理用
        Thread thread_httpCheck = new CheckHTTPServer(PingHTTPPort);

        // UDP経由での受付
        Thread thread_udp = new RequestServer(ResponsePort);

        // HTTP経由での受付
        Thread thread_http = new RequestHTTPServer(ResponsePort);

        thread_udp.start();
        thread_http.start();
        thread_tcpCheck.start();
        thread_httpCheck.start();
    }
}