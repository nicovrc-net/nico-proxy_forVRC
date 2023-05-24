package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import okhttp3.*;
import redis.clients.jedis.Protocol;
import xyz.n7mn.lib.Bilibili;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.n7mn.lib.NicoVideo.getLive;
import static xyz.n7mn.lib.NicoVideo.getVideo;
import static xyz.n7mn.lib.Redis.LogRedisWrite;

public class Main {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static OkHttpClient client = new OkHttpClient();
    private static int ResponsePort = 25252;
    private static int PingPort = 25253;
    private static int PingHTTPPort = 25280;

    public static void main(String[] args) {
        // Proxy読み込み
        File config1 = new File("./config.yml");
        File config2 = new File("./config-proxy.yml");
        File config3 = new File("./config-redis.yml");

        YamlMapping ConfigYaml1 = null;
        YamlMapping ConfigYaml2 = null;
        YamlMapping ConfigYaml3 = null;

        if (!config1.exists()){
            YamlMappingBuilder add = Yaml.createYamlMappingBuilder().add("Port", String.valueOf(ResponsePort)).add("PingPort", String.valueOf(PingPort)).add("PingHTTPPort", String.valueOf(PingHTTPPort));
            ConfigYaml1 = add.build();

            try {
                config1.createNewFile();
                PrintWriter writer = new PrintWriter(config1);
                writer.print(ConfigYaml1.toString());
                writer.close();

                System.out.println("[Error] ProxyList is Empty!!");
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

                System.out.println("[Error] ProxyList is Empty!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
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
                PrintWriter writer = new PrintWriter(config3);
                writer.print(ConfigYaml3.toString());
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // 受付サーバー
        ServerSocket svSock = null;
        try {

            new Thread(()->{
                ServerSocket svSock1 = null;
                try {
                    svSock1 = new ServerSocket(PingPort);
                } catch (Exception e){
                    e.printStackTrace();
                }

                while (true){
                    try {
                        Socket socket = svSock1.accept();
                        OutputStream stream = socket.getOutputStream();

                        stream.write("{status: \"OK\"}".getBytes(StandardCharsets.UTF_8));
                        stream.flush();

                        stream.close();
                        System.gc();

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread(()->{
                ServerSocket svSock2 = null;
                try {
                    svSock2 = new ServerSocket(PingHTTPPort);
                } catch (Exception e){
                    e.printStackTrace();
                }
                while (true){
                    try {
                        Socket socket = svSock2.accept();
                        InputStream inputStream = socket.getInputStream();
                        OutputStream outputStream = socket.getOutputStream();

                        byte[] data = new byte[100000000];
                        int readSize = inputStream.read(data);
                        if (readSize > 0){
                            data = Arrays.copyOf(data, readSize);
                        }

                        String text = new String(data, StandardCharsets.UTF_8);
                        Matcher matcher1 = Pattern.compile("GET / HTTP").matcher(text);
                        Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);

                        String httpVersion = "1." + (matcher2.find() ? matcher2.group(1) : "1");

                        String response;
                        if (!matcher1.find()){
                            response = "HTTP/1."+httpVersion+" 400 Bad Request\r\n" +
                                    "date: "+ new Date() +"\r\n" +
                                    "content-type: text/plain\r\n\r\n" +
                                    "400\r\n";

                        } else {
                            response = "HTTP/1."+httpVersion+" 200 OK\r\n" +
                                    "date: "+ new Date() +"\r\n" +
                                    "Content-type: text/plain; charset=UTF-8\r\n\r\n" +
                                    "ok";
                        }

                        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();

                        inputStream.close();
                        outputStream.close();

                        System.gc();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();

            svSock = new ServerSocket(ResponsePort);
            while (true){
                System.gc();
                Socket sock = svSock.accept();
                new Thread(()->{
                    final String AccessCode = UUID.randomUUID().toString()+"-" + new Date().getTime();
                    try {
                        byte[] data = new byte[100000000];
                        InputStream in = sock.getInputStream();
                        OutputStream out = sock.getOutputStream();

                        int readSize = in.read(data);
                        data = Arrays.copyOf(data, readSize);
                        String RequestHttp = new String(data, StandardCharsets.UTF_8);

                        LogRedisWrite(AccessCode, "access", RequestHttp);
                        LogRedisWrite(AccessCode, "access-ip", sock.getInetAddress().getHostAddress());

                        String text = new String(data, StandardCharsets.UTF_8);
                        data = null;

                        Matcher matcher1 = Pattern.compile("GET /\\?vi=(.*) HTTP").matcher(text);
                        Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);
                        String httpResponse;

                        String httpVersion = matcher2.find() ? matcher2.group(1) : "1";
                        if (matcher1.find()){

                            // "https://www.nicovideo.jp/watch/sm10759623"
                            String url = matcher1.group(1);
                            String videoUrl = null;
                            Matcher matcher_NicoVideoURL = Pattern.compile("(http|https)://(www\\.nicovideo\\.jp|nicovideo\\.jp/watch/(sm|nm|so)|sp\\.nicovideo\\.jp/watch/|nico\\.ms/(sm|nm|so))").matcher(url);
                            Matcher matcher_NicoLiveURL = Pattern.compile("(http|https)://(live\\.nicovideo\\.jp|nicovideo\\.jp/watch/(lv)|sp\\.live\\.nicovideo\\.jp/watch/|nico\\.ms/lv)").matcher(url);
                            Matcher matcher_bilibiliURL = Pattern.compile("(http|https)://(www\\.bilibili\\.com|bilibili\\.com/video/)").matcher(url);
                            if (matcher_NicoVideoURL.find()){
                                //System.out.println("video");
                                videoUrl = getVideo(url, AccessCode);
                            } else if (matcher_NicoLiveURL.find()) {
                                //System.out.println(url);
                                //System.out.println("live");
                                videoUrl = getLive(url, AccessCode);
                            } else if (matcher_bilibiliURL.find()) {
                                videoUrl = Bilibili.getVideo(url, AccessCode);
                            } else {
                                System.out.println("Unk url "+url);
                            }



                            if (videoUrl == null || !videoUrl.startsWith("http")){
                                httpResponse = "HTTP/1."+httpVersion+" 403 Forbidden\r\n" +
                                        "date: "+ new Date() +"\r\n" +
                                        "content-type: text/plain\r\n\r\n" +
                                        "403\r\n";
                            } else {

                                Matcher matcher = Pattern.compile("Host: (.*)\r\n").matcher(RequestHttp);
                                String host = "localhost:"+ResponsePort;
                                if (matcher.find()){
                                    host = matcher.group(1);
                                }

                                httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                        "Host: "+host+"\n" +
                                        "Date: "+new Date()+"\r\n" +
                                        "Connection: close\r\n" +
                                        "X-Powered-By: Java/8\r\n" +
                                        "Location: " + videoUrl + "\r\n" +
                                        "Content-type: text/html; charset=UTF-8\r\n\r\n";
                            }
                        } else {
                            httpResponse = "HTTP/1."+httpVersion+" 403 Forbidden\r\n" +
                                    "date: "+new Date()+"\r\n" +
                                    "content-type: text/plain\r\n\r\n" +
                                    "403\r\n";
                        }
                        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                        out.flush();



                        in.close();
                        out.close();
                        sock.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (svSock != null){
                try {
                    svSock.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }

}