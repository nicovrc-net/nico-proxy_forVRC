package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import xyz.n7mn.data.*;
import xyz.n7mn.nico_proxy.*;
import xyz.n7mn.nico_proxy.data.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    private static int ResponsePort = 25252;
    private static int PingPort = 25253;
    private static int PingHTTPPort = 25280;
    private static String Master = "-:22552";
    private static final HashMap<String, String> QueueList = new HashMap<>();
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

        // 同期用
        new SyncServer(Master, QueueList).start();

        // TCP死活管理用
        Thread thread_tcp = new PingTCPServer(PingPort);

        // HTTP死活管理用
        Thread thread_http = new PingHTTPServer(PingHTTPPort);

        Thread thread_main = new Thread(() -> {
            ServerSocket svSock = null;
            try {
                svSock = new ServerSocket(ResponsePort);

                System.out.println("Use URL : http://<IP>:"+ResponsePort+"/?vi=<NicoNicoURL>");

                while (true){
                    final File config = new File("./config-proxy.yml");

                    final List<String> ProxyList_Video = new ArrayList<>();
                    final List<String> ProxyList_Official = new ArrayList<>();

                    try {
                        YamlMapping ConfigYaml = null;
                        if (config.exists()){
                            ConfigYaml = Yaml.createYamlInput(config).readYamlMapping();
                        }

                        if (ConfigYaml != null){
                            YamlSequence list = ConfigYaml.yamlSequence("VideoProxy");
                            if (list != null){
                                for (int i = 0; i < list.size(); i++){
                                    ProxyList_Video.add(list.string(i));
                                }
                            }

                            YamlSequence list_so = ConfigYaml.yamlSequence("OfficialProxy");
                            if (list_so != null){
                                for (int i = 0; i < list_so.size(); i++){
                                    ProxyList_Official.add(list_so.string(i));
                                }
                            }
                        }

                    } catch (IOException e) {
                        //e.printStackTrace();
                    }

                    System.gc();
                    Socket sock = svSock.accept();
                    new Thread(()->{
                        final LogData log = new LogData();
                        log.setLogID(UUID.randomUUID().toString()+"-" + new Date().getTime());
                        log.setDate(new Date());

                        try {
                            InputStream in = sock.getInputStream();
                            OutputStream out = sock.getOutputStream();

                            byte[] data = new byte[1000000];
                            int readSize = in.read(data);
                            if (readSize <= 0){
                                sock.close();
                                return;
                            }
                            data = Arrays.copyOf(data, readSize);

                            String RequestHttp = new String(data, StandardCharsets.UTF_8);
                            String RequestIP = sock.getInetAddress().getHostAddress();
                            log.setHTTPRequest(RequestHttp);
                            log.setRequestIP(RequestIP);

                            String text = new String(data, StandardCharsets.UTF_8);

                            Matcher matcher1 = Pattern.compile("GET /\\?vi=(.*) HTTP").matcher(text);
                            Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);
                            String httpResponse;

                            String httpVersion = matcher2.find() ? matcher2.group(1) : "1";
                            Matcher matcher = Pattern.compile("Host: (.*)\r\n").matcher(RequestHttp);
                            String host = "localhost:"+ResponsePort;
                            if (matcher.find()){
                                host = matcher.group(1);
                            }

                            if (matcher1.find()){
                                // "https://www.nicovideo.jp/watch/sm10759623"
                                String temp_url = matcher1.group(1);
                                log.setRequestURL(temp_url);

                                Matcher matcher_1 = Pattern.compile("api\\.nicoad\\.nicovideo\\.jp").matcher(temp_url);
                                Matcher matcher_2 = Pattern.compile("b23\\.tv").matcher(temp_url);
                                Matcher matcher_3 = Pattern.compile("https://shinchan\\.biz/player\\.html\\?video_id=(.*)").matcher(temp_url);
                                Matcher matcher_4 = Pattern.compile("(ext|commons)\\.nicovideo\\.jp").matcher(temp_url);

                                if (matcher_1.find()){
                                    OkHttpClient build = new OkHttpClient();
                                    Request request = new Request.Builder()
                                            .url(temp_url)
                                            .build();
                                    Response response = build.newCall(request).execute();
                                    if (response.body() != null){
                                        Matcher matcher4 = Pattern.compile("<meta property=\"og:url\" content=\"(.*)\">").matcher(response.body().string());
                                        if (matcher4.find()){
                                            temp_url = matcher4.group(1);
                                            //System.out.println(temp_url);
                                        }
                                    }
                                    response.close();
                                }
                                if (matcher_2.find()){
                                    OkHttpClient build = new OkHttpClient();
                                    Request request = new Request.Builder()
                                            .url(temp_url)
                                            .build();
                                    Response response = build.newCall(request).execute();
                                    temp_url = response.request().url().toString();
                                    response.close();
                                }

                                if (matcher_3.find()){
                                    temp_url = "https://www.nicovideo.jp/watch/"+matcher_3.group(1);
                                }

                                if (matcher_4.find()){
                                    temp_url = temp_url.replaceAll("ext","www").replaceAll("commons","www").replaceAll("thumb","watch").replaceAll("works", "watch");
                                }

                                // URL変換サービスのURLは取り除く
                                if (temp_url.startsWith("http://yt.8uro.net") || temp_url.startsWith("https://yt.8uro.net")){
                                    temp_url = URLDecoder.decode(temp_url, StandardCharsets.UTF_8);
                                }

                                String[] list = {""+
                                        "http://yt.8uro.net/r?v=",
                                        "https://yt.8uro.net/r?v=",
                                        "http://nextnex.com/?url=",
                                        "https://nextnex.com/?url=",
                                        "http://vrc.kuroneko6423.com/proxy?url=",
                                        "https://vrc.kuroneko6423.com/proxy?url=",
                                        "http://kvvs.net/proxy?url=",
                                        "https://kvvs.net/proxy?url=",
                                        "http://questify.dev/?url=",
                                        "https://questify.dev/?url=",
                                        "http://questing.thetechnolus.com/v?url=",
                                        "https://questing.thetechnolus.com/v?url=",
                                        "http://questing.thetechnolus.com/",
                                        "https://questing.thetechnolus.com/",
                                        "http://vq.vrcprofile.com/?url=",
                                        "https://vq.vrcprofile.com/?url=",
                                        "http://api.yamachan.moe/proxy?url=",
                                        "https://api.yamachan.moe/proxy?url=",
                                        "http://nicovrc.net/proxy/?",
                                        "https://nicovrc.net/proxy/?",
                                        "http://nicovrc.net/proxy/dummy.m3u8?",
                                        "https://nicovrc.net/proxy/dummy.m3u8?",
                                        "http://nico.7mi.site/proxy/?",
                                        "https://nico.7mi.site/proxy/?",
                                        "http://nico.7mi.site/proxy/dummy.m3u8?",
                                        "https://nico.7mi.site/proxy/dummy.m3u8?",
                                        "http://qst.akakitune87.net/q?url=",
                                        "https://qst.akakitune87.net/q?url=",
                                        "http://u2b.cx/",
                                        "https://u2b.cx/"
                                };

                                String[] list_tube = {"" +
                                        "http://shay.loan/",
                                        "https://shay.loan/",
                                        "http://questing.thetechnolus.com/watch?v=",
                                        "https://questing.thetechnolus.com/watch?v=",
                                        "http://questing.thetechnolus.com/v/",
                                        "https://questing.thetechnolus.com/v/",
                                        "http://youtube.irunu.co/watch?v=",
                                        "https://youtube.irunu.co/watch?v="
                                };
                                String[] list_nico = {"" +
                                        "http://www.nicovideo.life/watch?v=",
                                        "https://www.nicovideo.life/watch?v=",
                                        "http://live.nicovideo.life/watch?v=",
                                        "https://live.nicovideo.life/watch?v=",
                                };

                                for (String str : list){
                                    temp_url = temp_url.replaceAll(str, "");
                                }
                                for (String str : list_tube){
                                    temp_url = temp_url.replaceAll(str, "https://youtu.be/");
                                }
                                for (String str : list_nico){
                                    temp_url = temp_url.replaceAll(str, "https://www.nicovideo.jp/watch/");
                                }

                                final String url = temp_url;
                                String videoUrl = null;

                                // すでにあったら処理済みURLを返す
                                String queueURL = QueueList.get(url.split("\\?")[0]);
                                if (queueURL != null){
                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "Connection: close\r\n" +
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: " + QueueList.get(url.split("\\?")[0]) + "\r\n" +
                                            "Content-type: text/html; charset=UTF-8\r\n\r\n";

                                    out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                                    out.flush();
                                    in.close();
                                    out.close();
                                    sock.close();
                                    log.setResultURL(QueueList.get(url.split("\\?")[0]));

                                    String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                    if (logToRedis){
                                        ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), json);
                                    } else {
                                        File file = new File("./log/");
                                        if (!file.exists()){
                                            file.mkdir();
                                        }

                                        File file1 = new File("./log/" + log.getLogID() + ".json");
                                        try {
                                            file1.createNewFile();
                                            PrintWriter writer = new PrintWriter(file1);
                                            writer.print(json);
                                            writer.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    return;
                                }

                                // 念のため問い合わせもする
                                if (!Master.split(":")[0].equals("-")){

                                    String jsonText = new Gson().toJson(new SyncData(url.split("\\?")[0], null));

                                    DatagramSocket udp_sock = new DatagramSocket();
                                    DatagramPacket udp_packet = new DatagramPacket(jsonText.getBytes(StandardCharsets.UTF_8), jsonText.getBytes(StandardCharsets.UTF_8).length,new InetSocketAddress(Master.split(":")[0],Integer.parseInt(Master.split(":")[1])));
                                    udp_sock.send(udp_packet);
                                    //System.out.println("キュー送信 : " + jsonText);

                                    byte[] temp = new byte[100000];
                                    DatagramPacket udp_packet2 = new DatagramPacket(temp, temp.length);
                                    udp_sock.receive(udp_packet2);
                                    //System.out.println("キュー受信 : " + new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength())));
                                    udp_sock.close();

                                    String result = new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength()));

                                    if (!result.equals("null")){
                                        httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                                "Host: "+host+"\n" +
                                                "Date: "+new Date()+"\r\n" +
                                                "Connection: close\r\n" +
                                                "X-Powered-By: Java/8\r\n" +
                                                "Location: " + result + "\r\n" +
                                                "Access-Control-Allow-Origin: *\r\n" +
                                                "Content-type: text/html; charset=UTF-8\r\n\r\n";

                                        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                                        out.flush();
                                        in.close();
                                        out.close();
                                        sock.close();

                                        log.setResultURL(result);

                                        new Thread(()->{
                                            String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                            if (logToRedis){
                                                ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), json);
                                            } else {
                                                File file = new File("./log/");
                                                if (!file.exists()){
                                                    file.mkdir();
                                                }

                                                File file1 = new File("./log/" + log.getLogID() + ".json");
                                                try {
                                                    file1.createNewFile();
                                                    PrintWriter writer = new PrintWriter(file1);
                                                    writer.print(json);
                                                    writer.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }).start();

                                        return;
                                    }
                                }

                                System.gc();
                                //System.out.println("キャッシュヒットせず : " + url);

                                String ErrorMessage = null;

                                Matcher matcher_NicoVideoURL = Pattern.compile("(\\.nicovideo\\.jp|nico\\.ms)").matcher(url);
                                Matcher matcher_BilibiliURL = Pattern.compile("bilibili(\\.com|\\.tv)").matcher(url);
                                Matcher matcher_YoutubeURL = Pattern.compile("(youtu\\.be|youtube\\.com)").matcher(url);
                                Matcher matcher_XvideoURL = Pattern.compile("xvideo").matcher(url);
                                Matcher matcher_TikTokURL = Pattern.compile("tiktok").matcher(url);
                                Matcher matcher_TwitterURL = Pattern.compile("(x|twitter)\\.com/(.*)/status/(.*)").matcher(url);
                                Matcher matcher_OpenrecURL = Pattern.compile("openrec").matcher(url);
                                Matcher matcher_PornhubURL = Pattern.compile("pornhub\\.com").matcher(url);
                                Matcher matcher_TwicastURL = Pattern.compile("twitcasting\\.tv").matcher(url);
                                Matcher matcher_AbemaURL = Pattern.compile("abema\\.tv").matcher(url);


                                boolean isNico = matcher_NicoVideoURL.find();
                                boolean isBiliBili = matcher_BilibiliURL.find();
                                boolean isYoutube = matcher_YoutubeURL.find();
                                boolean isXvideo = matcher_XvideoURL.find();
                                boolean isTiktok = matcher_TikTokURL.find();
                                boolean isTwitter = matcher_TwitterURL.find();
                                boolean isOpenrec = matcher_OpenrecURL.find();
                                boolean isPornhub = matcher_PornhubURL.find();
                                boolean isTwicast = matcher_TwicastURL.find();
                                boolean isAbema = matcher_AbemaURL.find();

                                Matcher matcher_vrcString = Pattern.compile("user-agent: unityplayer/").matcher(RequestHttp.toLowerCase(Locale.ROOT));
                                Matcher matcher_TitleGet= Pattern.compile("x-nicovrc-titleget: yes").matcher(RequestHttp.toLowerCase(Locale.ROOT));
                                boolean isTitleGet = matcher_vrcString.find() || matcher_TitleGet.find();

                                final ShareService service;
                                final String BiliBili;

                                if (isNico){
                                    service = new NicoNicoVideo();
                                    BiliBili = "";
                                } else if (isBiliBili){
                                    Matcher m = Pattern.compile("tv").matcher(url);
                                    service = m.find() ? new BilibiliTv() : new BilibiliCom();
                                    BiliBili = m.find() ? "tv" : "com";
                                } else if (isXvideo){
                                    service = new Xvideos();
                                    BiliBili = "";
                                } else if (isTiktok) {
                                    service = new TikTok();
                                    BiliBili = "";
                                }else if (isTwitter){
                                    service = new Twitter();
                                    BiliBili = "";
                                } else if (isOpenrec){
                                    service = new OPENREC();
                                    BiliBili = "";
                                } else if (isPornhub){
                                    service = new Pornhub();
                                    BiliBili = "";
                                } else if (isTwicast) {
                                    service = new Twicast(twitcastClientId, twitcastClientSecret);
                                    BiliBili = "";
                                } else if (isAbema){
                                    service = new Abema();
                                    BiliBili = "";
                                } else {
                                    service = null;
                                    BiliBili = "";
                                }


                                // VRCStringDownloaderっぽいアクセスとx-nicovrc-titlegetがyesのときは動画情報の取得だけして200を返す
                                if (isTitleGet){

                                    final List<String> proxyList;

                                    if (isNico){
                                        Matcher matcher_Official = Pattern.compile("(live|so)").matcher(url);

                                        if (matcher_Official.find()){
                                            proxyList = ProxyList_Official;
                                        } else {
                                            proxyList = ProxyList_Video;
                                        }
                                    } else {
                                        proxyList = ProxyList_Official;
                                    }

                                    final String[] proxy = !proxyList.isEmpty() ? proxyList.get(new SecureRandom().nextInt(0, proxyList.size())).split(":") : null;
                                    String title = "";
                                    try {
                                        if (service != null){
                                            if (proxy != null){
                                                title = service.getTitle(new RequestVideoData(url, new ProxyData(proxy[0], Integer.parseInt(proxy[1]))));
                                            } else {
                                                title = service.getTitle(new RequestVideoData(url, null));
                                            }
                                        }

                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }

                                    httpResponse = "HTTP/1."+httpVersion+" 200 OK\r\n" +
                                            "date: "+ new Date() +"\r\n" +
                                            "content-type: text/plain\r\n\r\n" +
                                            title+"\r\n";

                                    out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                                    out.flush();

                                    new Thread(()->{
                                        String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                        if (logToRedis){
                                            ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), json);
                                        } else {
                                            File file = new File("./log/");
                                            if (!file.exists()){
                                                file.mkdir();
                                            }

                                            File file1 = new File("./log/" + log.getLogID() + ".json");
                                            try {
                                                file1.createNewFile();
                                                PrintWriter writer = new PrintWriter(file1);
                                                writer.print(json);
                                                writer.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();

                                    in.close();
                                    out.close();
                                    sock.close();

                                    return;
                                }

                                // youtubeはそのまま転送
                                if (isYoutube){

                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "Connection: close\r\n" +
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: " + url + "\r\n" +
                                            "Content-type: text/html; charset=UTF-8\r\n\r\n";


                                    log.setResultURL(url);
                                    out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                                    out.flush();

                                    new Thread(()->{
                                        String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                        if (logToRedis){
                                            ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), json);
                                        } else {
                                            File file = new File("./log/");
                                            if (!file.exists()){
                                                file.mkdir();
                                            }

                                            File file1 = new File("./log/" + log.getLogID() + ".json");
                                            try {
                                                file1.createNewFile();
                                                PrintWriter writer = new PrintWriter(file1);
                                                writer.print(json);
                                                writer.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();

                                    in.close();
                                    out.close();
                                    sock.close();

                                    return;
                                }

                                // ニコ動 / ニコ生
                                if (isNico){
                                    Matcher matcher_Official = Pattern.compile("(live|so)").matcher(url);
                                    Matcher matcher_live = Pattern.compile("live").matcher(url);

                                    final List<String> proxyList;
                                    if (matcher_Official.find()){
                                        proxyList = ProxyList_Official;
                                    } else {
                                        proxyList = ProxyList_Video;
                                    }

                                    final String[] proxy = !proxyList.isEmpty() ? proxyList.get(new SecureRandom().nextInt(0, proxyList.size())).split(":") : null;

                                    try {
                                        if (matcher_live.find()){
                                            //System.out.println("kita?");
                                            if (proxy != null){
                                                videoUrl = service.getLive(new RequestVideoData(url, new ProxyData(proxy[0], Integer.parseInt(proxy[1])))).getVideoURL();
                                            } else {
                                                videoUrl = service.getLive(new RequestVideoData(url, null)).getVideoURL();
                                            }
                                            //System.out.println("kiteru : " + videoUrl);

                                            // キューリスト追加
                                            QueueList.put(url.split("\\?")[0], videoUrl);

                                            // 同期鯖がある場合は送信する
                                            if (!Master.split(":")[0].equals("-")){
                                                String finalVideoUrl = videoUrl;
                                                new Thread(()->{
                                                    try {
                                                        String[] s = Master.split(":");
                                                        byte[] bytes = new Gson().toJson(new SyncData(url.split("\\?")[0], finalVideoUrl)).getBytes(StandardCharsets.UTF_8);
                                                        //System.out.println("[Debug] " + new String(bytes) + "を送信");
                                                        DatagramSocket udp_sock = new DatagramSocket();//UDP送信用ソケットの構築
                                                        DatagramPacket udp_packet = new DatagramPacket(bytes, bytes.length,new InetSocketAddress(s[0],Integer.parseInt(s[1])));
                                                        udp_sock.send(udp_packet);
                                                        udp_sock.close();
                                                    } catch (Exception e){
                                                        //e.printStackTrace();
                                                    }
                                                }).start();
                                            }
                                        } else {
                                            ResultVideoData video;
                                            if (proxy != null){
                                                video = service.getVideo(new RequestVideoData(url, new ProxyData(proxy[0], Integer.parseInt(proxy[1]))));
                                                videoUrl = video.getVideoURL();
                                            } else {
                                                video = service.getVideo(new RequestVideoData(url, null));
                                                videoUrl = video.getVideoURL();
                                            }

                                            // キューリスト追加
                                            QueueList.put(url.split("\\?")[0], videoUrl);

                                            // 同期鯖がある場合は送信する
                                            if (!Master.split(":")[0].equals("-")){
                                                String finalVideoUrl = videoUrl;
                                                new Thread(()->{
                                                    try {
                                                        String[] s = Master.split(":");
                                                        byte[] bytes = new Gson().toJson(new SyncData(url.split("\\?")[0], finalVideoUrl)).getBytes(StandardCharsets.UTF_8);
                                                        //System.out.println("[Debug] " + new String(bytes) + "を送信");
                                                        DatagramSocket udp_sock = new DatagramSocket();//UDP送信用ソケットの構築
                                                        DatagramPacket udp_packet = new DatagramPacket(bytes, bytes.length,new InetSocketAddress(s[0],Integer.parseInt(s[1])));
                                                        udp_sock.send(udp_packet);
                                                        udp_sock.close();
                                                    } catch (Exception e){
                                                        //e.printStackTrace();
                                                    }
                                                }).start();
                                            }

                                            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                                            final OkHttpClient client = proxy != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1])))).build() : new OkHttpClient();
                                            // ハートビート信号送る
                                            Request request_html = new Request.Builder()
                                                    .url(url)
                                                    .build();
                                            Response response1 = client.newCall(request_html).execute();
                                            String HtmlText;
                                            if (response1.body() != null){
                                                HtmlText = response1.body().string();
                                            } else {
                                                HtmlText = "";
                                            }
                                            response1.close();
                                            Matcher matcher_video = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">").matcher(HtmlText);

                                            final long videoTime;
                                            if (matcher_video.find()){
                                                videoTime = Long.parseLong(matcher_video.group(1));
                                            } else {
                                                videoTime = 3600L;
                                            }

                                            TokenJSON json = new Gson().fromJson(video.getTokenJson(), TokenJSON.class);
                                            Timer timer = new Timer();
                                            int[] count = new int[]{0};
                                            timer.scheduleAtFixedRate(new TimerTask() {
                                                @Override
                                                public void run() {
                                                    if (count[0] > (videoTime / 40L)){
                                                        timer.cancel();
                                                        return;
                                                    }

                                                    RequestBody body = RequestBody.create(json.getTokenValue(), MediaType.get("application/json; charset=utf-8"));
                                                    Request request1 = new Request.Builder()
                                                            .url(json.getTokenSendURL())
                                                            .post(body)
                                                            .build();
                                                    try {
                                                        Response response1 = client.newCall(request1).execute();
                                                        //System.out.println(response.body().string());
                                                        response1.close();
                                                    } catch (IOException e) {
                                                        // e.printStackTrace();
                                                        return;
                                                    }

                                                    count[0]++;
                                                }
                                            }, 0L, 40000L);


                                            if (video.isEncrypted()){
                                                // 暗号化HLS
                                                Request m3u8 = new Request.Builder()
                                                        .url("https://n.nicovrc.net/?url="+video.getVideoURL()+"&proxy="+(proxy != null ? proxy[0]+":"+proxy[1] : ""))
                                                        .build();

                                                Response response = client.newCall(m3u8).execute();
                                                String s1 = response.body() != null && response.code() == 200 ? response.body().string() : "";
                                                response.close();
                                                if (!s1.isEmpty()){
                                                    videoUrl = "https://n.nicovrc.net"+s1;
                                                }
                                            }

                                        }

                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        //e.printStackTrace();
                                        log.setErrorMessage(e.getMessage());

                                        String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                        if (logToRedis){
                                            ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), json);
                                        } else {
                                            File file = new File("./log/");
                                            if (!file.exists()){
                                                file.mkdir();
                                            }

                                            File file1 = new File("./log/" + log.getLogID() + ".json");
                                            try {
                                                file1.createNewFile();
                                                PrintWriter writer = new PrintWriter(file1);
                                                writer.print(json);
                                                writer.close();
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                }

                                // ビリビリ
                                if (isBiliBili){
                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;

                                    try {
                                        ResultVideoData video;
                                        if (split != null){
                                            video = service.getVideo(new RequestVideoData(url, new ProxyData(split[0], Integer.parseInt(split[1]))));
                                        } else {
                                            video = service.getVideo(new RequestVideoData(url, null));
                                        }

                                        //System.out.println(video.getVideoURL());

                                        // 映像と音声で分離しているので結合処理が必要
                                        final Request m3u8;
                                        if (split != null){
                                            //System.out.println(bilibiliSystemURL+"/?url="+video.getVideoURL()+",,"+video.getAudioURL()+"&cc&" + (m.find() ? "tv" : "com")+"&cc&"+split[0]+":"+split[1]);
                                            m3u8 = new Request.Builder()
                                                    .url(bilibiliSystemURL+"/?url="+video.getVideoURL()+",,"+video.getAudioURL()+"&cc&" + BiliBili+"&cc&"+split[0]+":"+split[1])
                                                    .build();
                                        } else {
                                            //System.out.println(bilibiliSystemURL+"/?url="+video.getVideoURL()+",,"+video.getAudioURL()+"&cc&" + (isTv ? "tv" : "com"));
                                            m3u8 = new Request.Builder()
                                                    .url(bilibiliSystemURL+"/?url="+video.getVideoURL()+",,"+video.getAudioURL()+"&&cc&" + BiliBili)
                                                    .build();
                                        }


                                        final OkHttpClient client = new OkHttpClient();
                                        Response response = client.newCall(m3u8).execute();
                                        String s1 = response.body() != null ? response.body().string() : "";
                                        response.close();
                                        videoUrl = bilibiliSystemURL+s1;
                                        //System.out.println(videoUrl);

                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());

                                        String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                        if (logToRedis){
                                            ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), json);
                                        } else {
                                            File file = new File("./log/");
                                            if (!file.exists()){
                                                file.mkdir();
                                            }

                                            File file1 = new File("./log/" + log.getLogID() + ".json");
                                            try {
                                                file1.createNewFile();
                                                PrintWriter writer = new PrintWriter(file1);
                                                writer.print(json);
                                                writer.close();
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                }

                                // xvideos
                                //System.out.println(url);
                                if (isXvideo){
                                    //System.out.println("test");
                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
                                    try {
                                        ResultVideoData video = service.getVideo(new RequestVideoData(url,split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                                        videoUrl = video.getVideoURL();
                                        //System.out.println(videoUrl);
                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());
                                    }
                                }

                                // TikTok
                                if (isTiktok){
                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
                                    try {
                                        ResultVideoData video = service.getVideo(new RequestVideoData(url,split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                                        videoUrl = video.getVideoURL();
                                        //System.out.println(videoUrl);
                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());
                                    }
                                }

                                // Twitter
                                if (isTwitter){
                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
                                    try {
                                        ResultVideoData video = service.getVideo(new RequestVideoData(url,split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                                        videoUrl = video.getVideoURL();
                                        //System.out.println(videoUrl);
                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());
                                    }
                                }

                                // OPENREC
                                if (isOpenrec){

                                    //System.out.println("openrec test");
                                    Matcher m = Pattern.compile("live").matcher(url);

                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
                                    try {
                                        ResultVideoData video = m.find() ? service.getLive(new RequestVideoData(url,split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null)) : service.getVideo(new RequestVideoData(url,split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                                        //videoUrl = video.getVideoURL();

                                        String proxy = "null";
                                        if (split != null){
                                            proxy = split[0]+":"+split[1];
                                        }
                                        String isStream = video.isStream() ? "true" : "false";

                                        Request m3u8 = new Request.Builder()
                                                .url("https://nicovrc.net/rec/?isStream="+isStream+"&u="+video.getVideoURL()+"&proxy="+proxy)
                                                .build();

                                        final OkHttpClient client = new OkHttpClient();
                                        Response response = client.newCall(m3u8).execute();
                                        videoUrl = response.body() != null ? response.body().string() : "";
                                        response.close();
                                        //System.out.println(videoUrl);

                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());
                                    }
                                }

                                // Pornhub
                                if (isPornhub){
                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
                                    try {
                                        ResultVideoData video = service.getVideo(new RequestVideoData(url,split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                                        videoUrl = video.getVideoURL();

                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());
                                    }
                                }

                                // ツイキャス
                                if (isTwicast){
                                    //System.out.println("id : " +twitcastClientId+ " / "+twitcastClientSecret);
                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
                                    try {
                                        ResultVideoData video = service.getLive(new RequestVideoData(url,split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                                        videoUrl = video.getVideoURL();

                                        if (!video.isStream()){
                                            String tempVideoUrl = videoUrl;
                                            videoUrl = null;
                                            // アーカイブは最初のmaster.m3u8だけリファラが必須。それ以降は必要なさそう
                                            Request twicast = new Request.Builder()
                                                    .url(tempVideoUrl)
                                                    .addHeader("Referer", "https://twitcasting.tv/")
                                                    .build();


                                            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                                            final OkHttpClient client = !ProxyList_Official.isEmpty() ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split[0], Integer.parseInt(split[1])))).build() : new OkHttpClient();
                                            Response response_twicast = client.newCall(twicast).execute();

                                            if (response_twicast.code() == 200){
                                                if (response_twicast.body() != null){

                                                    Matcher tempUrl = Pattern.compile("https://(.+)/tc.vod.v2").matcher(tempVideoUrl);

                                                    String baseUrl = "";
                                                    if (tempUrl.find()){
                                                        baseUrl = "https://"+tempUrl.group(1);
                                                    }

                                                    String str = response_twicast.body().string();
                                                    for (String s : str.split("\n")){
                                                        if (s.startsWith("#")){
                                                            continue;
                                                        }

                                                        videoUrl = baseUrl+s;
                                                        break;
                                                    }
                                                }
                                            }
                                            response_twicast.close();
                                        }

                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());
                                        //e.printStackTrace();
                                    }
                                }

                                // Abema
                                if (isAbema){
                                    String[] split = !ProxyList_Official.isEmpty() ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
                                    try {
                                        ResultVideoData video = service.getVideo(new RequestVideoData(url, split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                                        videoUrl = video.getVideoURL();
                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                        log.setErrorMessage(e.getMessage());
                                    }
                                }

                                if (!isNico && !isBiliBili && !isXvideo && !isTiktok && !isTwitter && !isOpenrec && !isPornhub && !isTwicast && !isAbema && url.startsWith("http")){
                                    // 画像
                                    final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                                    String[] split = !ProxyList_Video.isEmpty() ? ProxyList_Video.get(new SecureRandom().nextInt(0, ProxyList_Video.size())).split(":") : null;
                                    final OkHttpClient client = !ProxyList_Video.isEmpty() ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split[0], Integer.parseInt(split[1])))).build() : new OkHttpClient();
                                    Request img = new Request.Builder()
                                            .url(url)
                                            .build();
                                    Response response_img = client.newCall(img).execute();

                                    if (response_img.body() != null && response_img.body().contentType().toString().startsWith("image")){
                                        videoUrl = "https://i2v.nicovrc.net/?url="+url;
                                    }
                                    response_img.close();
                                }

                                if (videoUrl == null && ErrorMessage == null){
                                    /*httpResponse = "HTTP/1."+httpVersion+" 404 Not Found\r\n" +
                                            "date: "+ new Date() +"\r\n" +
                                            "content-type: application/json\r\n\r\n" +
                                            "{\"ErrorMessage\": \"Not Found\"}\r\n";*/
                                    //

                                    String locationText = "Found. Redirecting to https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=Not Found";
                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "content-length: "+locationText.getBytes(StandardCharsets.UTF_8).length+"\r\n"+
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=Not Found\r\n" +
                                            "Content-type: text/plain; charset=UTF-8\r\n\r\n"+locationText;
                                } else if (ErrorMessage != null) {
                                    /*
                                    httpResponse = "HTTP/1."+httpVersion+" 400 Bad Request\r\n" +
                                            "date: "+ new Date() +"\r\n" +
                                            "content-type: application/json\r\n\r\n" +
                                            "{\"ErrorMessage\": \""+ErrorMessage+"\"}\r\n";*/

                                    String locationText = "Found. Redirecting to https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg="+ErrorMessage;
                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "content-length: "+locationText.getBytes(StandardCharsets.UTF_8).length+"\r\n"+
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=" + ErrorMessage + "\r\n" +
                                            "Content-type: text/plain; charset=UTF-8\r\n\r\n"+locationText;

                                } else {
                                    String locationText = "Found. Redirecting to "+videoUrl;

                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "content-length: "+locationText.getBytes(StandardCharsets.UTF_8).length+"\r\n"+
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: " + videoUrl + "\r\n" +
                                            "Content-type: text/plain; charset=UTF-8\r\n\r\n"+locationText;

                                    log.setResultURL(videoUrl);
                                }
                            } else {
                                httpResponse = "HTTP/1."+httpVersion+" 400 Bad Request\r\n" +
                                        "date: "+new Date()+"\r\n" +
                                        "content-type: application/json\r\n\r\n" +
                                        "{\"ErrorMessage\": \"Not Support\"}\r\n";


                            }

                            new Thread(()->{
                                String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                if (logToRedis){
                                    ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), json);
                                } else {
                                    File file = new File("./log/");
                                    if (!file.exists()){
                                        file.mkdir();
                                    }

                                    File file1 = new File("./log/" + log.getLogID() + ".json");
                                    try {
                                        file1.createNewFile();
                                        PrintWriter writer = new PrintWriter(file1);
                                        writer.print(json);
                                        writer.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
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
        });

        thread_main.start();
        thread_tcp.start();
        thread_http.start();
    }


    private static void ToRedis(String key, String content){

        File config = new File("./config-redis.yml");
        YamlMapping ConfigYml = null;

        if (!config.exists()){
            YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
            ConfigYml = builder.add(
                    "RedisServer", "127.0.0.1"
            ).add(
                    "RedisPort", String.valueOf(Protocol.DEFAULT_PORT)
            ).add(
                    "RedisPass", ""
            ).build();

            try {
                config.createNewFile();
                PrintWriter writer = new PrintWriter(config);
                writer.print(ConfigYml.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ConfigYml = Yaml.createYamlInput(config).readYamlMapping();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
        Jedis jedis = jedisPool.getResource();
        if (!ConfigYml.string("RedisPass").isEmpty()){
            jedis.auth(ConfigYml.string("RedisPass"));
        }

        jedis.set(key, content);


        jedis.close();
        jedisPool.close();

    }
}