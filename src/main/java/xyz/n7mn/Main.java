package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import xyz.n7mn.data.LogData;
import xyz.n7mn.data.PingHTTPServer;
import xyz.n7mn.data.PingTCPServer;
import xyz.n7mn.data.QueueData;
import xyz.n7mn.nico_proxy.*;
import xyz.n7mn.nico_proxy.data.*;

import java.io.*;
import java.net.*;
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

    public static void main(String[] args) {
        // Proxy読み込み
        File config1 = new File("./config.yml");
        File config2 = new File("./config-proxy.yml");
        File config3 = new File("./config-redis.yml");

        YamlMapping ConfigYaml1 = null;
        YamlMapping ConfigYaml2 = null;
        YamlMapping ConfigYaml3 = null;

        if (!config1.exists()){
            YamlMappingBuilder add = Yaml.createYamlMappingBuilder()
                    .add("Port", String.valueOf(ResponsePort))
                    .add("PingPort", String.valueOf(PingPort))
                    .add("PingHTTPPort", String.valueOf(PingHTTPPort))
                    .add("Master", "-:22552")
                    .add("LogToRedis", "False");
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

        // 同期用
        if (Master.split(":")[0].equals("-")){
            new SyncServer(Master, QueueList).start();
        } else {
            int[] count = new int[]{-1};
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    HashMap<String, String> temp = new HashMap<>(QueueList);
                    if (count[0] != temp.size()){
                        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] 現在のキュー数 : " + temp.size());
                        count[0] = temp.size();
                    }

                    // すでに有効期限が切れていて見れないものは削除

                    temp.forEach((id, url)->{
                        try {
                            OkHttpClient build = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url(url)
                                    .build();
                            Response response = build.newCall(request).execute();

                            if (response.code() == 403 || response.code() == 404){
                                QueueList.remove(id);
                                System.out.println("[Info] キュー " + id + "を削除");
                            }
                            response.close();
                        } catch (Exception e) {
                            //System.out.println(e.getMessage());
                        }
                    });

                    System.gc();
                }
            };

            timer.scheduleAtFixedRate(task, 0L, 5000L);
        }

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
                                Matcher matcher_4 = Pattern.compile("ext\\.nicovideo\\.jp").matcher(temp_url);

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
                                    temp_url = temp_url.replaceAll("ext","www").replaceAll("thumb","watch");
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
                                    Socket socket1 = new Socket(Master.split(":")[0], Integer.parseInt(Master.split(":")[1]));

                                    OutputStream outputStream = socket1.getOutputStream();
                                    InputStream inputStream = socket1.getInputStream();

                                    outputStream.write("{\"queue\":\"getList\"}".getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();

                                    if (inputStream.readAllBytes().length == 0){
                                        outputStream.close();
                                        inputStream.close();
                                        socket1.close();
                                    }

                                    QueueData[] json = new Gson().fromJson(new String(inputStream.readAllBytes()), QueueData[].class);

                                    if (json != null){
                                        for (QueueData qData : json) {
                                            if (qData.getID().equals(url.split("\\?")[0])){
                                                httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                                        "Host: "+host+"\n" +
                                                        "Date: "+new Date()+"\r\n" +
                                                        "Connection: close\r\n" +
                                                        "X-Powered-By: Java/8\r\n" +
                                                        "Location: " + qData.getURL() + "\r\n" +
                                                        "Access-Control-Allow-Origin: *\r\n" +
                                                        "Content-type: text/html; charset=UTF-8\r\n\r\n";

                                                out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                                                out.flush();
                                                in.close();
                                                out.close();
                                                sock.close();


                                                QueueList.put(url.split("\\?")[0], qData.getURL());

                                                if (!socket1.isClosed()){
                                                    outputStream.close();
                                                    inputStream.close();
                                                    socket1.close();
                                                }
                                                log.setResultURL(QueueList.get(url.split("\\?")[0]));

                                                String jsonText = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(log);
                                                if (logToRedis){
                                                    ToRedis("nico-proxy:ExecuteLog:"+log.getLogID(), jsonText);
                                                } else {
                                                    File file = new File("./log/");
                                                    if (!file.exists()){
                                                        file.mkdir();
                                                    }

                                                    File file1 = new File("./log/" + log.getLogID() + ".json");
                                                    try {
                                                        file1.createNewFile();
                                                        PrintWriter writer = new PrintWriter(file1);
                                                        writer.print(jsonText);
                                                        writer.close();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                return;
                                            }
                                        }
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

                                ShareService service = null;

                                // Youtubeは別のサービスに転送する
                                if (matcher_YoutubeURL.find()){
                                    String[] youtubeList = new String[]{
                                            "https://kvvs.net/proxy?url=",
                                            "https://api.yamachan.moe/proxy?url=",
                                            "https://yt.8uro.net/r?v=",
                                            "https://qst.akakitune87.net/q?url=",
                                            "https://vq.vrcprofile.com/?url="
                                    };

                                    int i = new SecureRandom().nextInt(0, youtubeList.length);

                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "Connection: close\r\n" +
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: " + youtubeList[i]+url + "\r\n" +
                                            "Content-type: text/html; charset=UTF-8\r\n\r\n";


                                    log.setResultURL(youtubeList[i]+url);
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
                                if (matcher_NicoVideoURL.find()){
                                    service = new NicoNicoVideo();

                                    Matcher matcher_Official = Pattern.compile("(live|so)").matcher(url);
                                    Matcher matcher_live = Pattern.compile("live").matcher(url);

                                    final List<String> proxyList;
                                    if (matcher_Official.find()){
                                        proxyList = ProxyList_Official;
                                    } else {
                                        proxyList = ProxyList_Video;
                                    }

                                    final String[] proxy = proxyList.size() > 0 ? proxyList.get(new SecureRandom().nextInt(0, proxyList.size())).split(":") : null;

                                    try {
                                        if (matcher_live.find()){
                                            //System.out.println("kita?");
                                            if (proxy != null){
                                                videoUrl = service.getLive(new RequestVideoData(url, new ProxyData(proxy[0], Integer.parseInt(proxy[1])))).getVideoURL();
                                            } else {
                                                videoUrl = service.getLive(new RequestVideoData(url, null)).getVideoURL();
                                            }
                                            //System.out.println("kiteru : " + videoUrl);
                                        } else {
                                            ResultVideoData video;
                                            if (proxy != null){
                                                video = service.getVideo(new RequestVideoData(url, new ProxyData(proxy[0], Integer.parseInt(proxy[1]))));
                                                videoUrl = video.getVideoURL();
                                            } else {
                                                video = service.getVideo(new RequestVideoData(url, null));
                                                videoUrl = video.getVideoURL();
                                            }

                                            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                                            final OkHttpClient client = proxy != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1])))).build() : new OkHttpClient();

                                            //System.out.println(video.isEncrypted());
                                            if (video.isEncrypted()){
                                                // 暗号化HLS
                                                EncryptedTokenJSON json = new Gson().fromJson(video.getTokenJson(), EncryptedTokenJSON.class);
                                                //
                                                Request m3u8 = new Request.Builder()
                                                        .url("https://nicovrc.net/m3u8/?vi="+video.getVideoURL()+"&nnkey="+json.getEncryptedURL()+"&mode=nico_enc")
                                                        .build();

                                                Response response = client.newCall(m3u8).execute();
                                                String s1 = response.body() != null ? response.body().string() : "";
                                                response.close();
                                                videoUrl = "https://nicovrc.net/m3u8/"+s1+".m3u8";
                                            }

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

                                        }
                                        // キューリスト追加
                                        QueueList.put(url.split("\\?")[0], videoUrl);

                                        // 同期鯖がある場合は送信する
                                        if (!Master.split(":")[0].equals("-")){
                                            String finalVideoUrl = videoUrl;
                                            new Thread(()->{
                                                try {
                                                    Socket socket1 = new Socket(Master.split(":")[0], Integer.parseInt(Master.split(":")[1]));

                                                    OutputStream outputStream = socket1.getOutputStream();
                                                    InputStream inputStream = socket1.getInputStream();

                                                    outputStream.write(("{\"queue\":\"" + url.split("\\?")[0] +"," + finalVideoUrl + "\"}").getBytes(StandardCharsets.UTF_8));
                                                    outputStream.flush();

                                                    outputStream.close();
                                                    inputStream.close();
                                                    socket1.close();
                                                } catch (Exception e){
                                                    //e.printStackTrace();
                                                }
                                            }).start();
                                        }
                                    } catch (Exception e){
                                        ErrorMessage = e.getMessage();
                                        e.printStackTrace();
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
                                if (matcher_BilibiliURL.find()){
                                    Matcher m = Pattern.compile("tv").matcher(url);
                                    service = m.find() ? new BilibiliTv() : new BilibiliCom();
                                    String[] split = ProxyList_Official.size() > 0 ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;

                                    try {
                                        ResultVideoData video;
                                        if (split != null){
                                            video = service.getVideo(new RequestVideoData(url, new ProxyData(split[0], Integer.parseInt(split[1]))));
                                        } else {
                                            video = service.getVideo(new RequestVideoData(url, null));
                                        }

                                        // tvの場合は映像と音声で分離するので結合処理が必要
                                        if (video.getAudioURL() != null){

                                            Request m3u8 = new Request.Builder()
                                                    .url("https://nico.7mi.site/m3u8/?vi="+video.getVideoURL()+"&music="+video.getAudioURL())
                                                    .build();

                                            final OkHttpClient client = new OkHttpClient();
                                            Response response = client.newCall(m3u8).execute();
                                            String s1 = response.body() != null ? response.body().string() : "";
                                            response.close();
                                            videoUrl = "https://nico.7mi.site/m3u8/video_"+s1+".m3u8";
                                        } else {
                                            videoUrl = video.getVideoURL();
                                        }

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
                                if (matcher_XvideoURL.find()){
                                    //System.out.println("test");
                                    service = new Xvideos();
                                    String[] split = ProxyList_Official.size() > 0 ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
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
                                if (matcher_TikTokURL.find()){
                                    service = new TikTok();
                                    String[] split = ProxyList_Official.size() > 0 ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;
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

                                if (videoUrl == null && ErrorMessage == null){
                                    httpResponse = "HTTP/1."+httpVersion+" 404 Not Found\r\n" +
                                            "date: "+ new Date() +"\r\n" +
                                            "content-type: application/json\r\n\r\n" +
                                            "{\"ErrorMessage\": \"Not Found\"}\r\n";
                                } else if (ErrorMessage != null) {

                                    httpResponse = "HTTP/1."+httpVersion+" 400 Bad Request\r\n" +
                                            "date: "+ new Date() +"\r\n" +
                                            "content-type: application/json\r\n\r\n" +
                                            "{\"ErrorMessage\": \""+ErrorMessage+"\"}\r\n";

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
        if (ConfigYml.string("RedisPass").length() > 0){
            jedis.auth(ConfigYml.string("RedisPass"));
        }

        jedis.set(key, content);


        jedis.close();
        jedisPool.close();

    }
}