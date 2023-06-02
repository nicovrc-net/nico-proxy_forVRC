package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import com.google.gson.Gson;
import okhttp3.*;
import redis.clients.jedis.Protocol;
import xyz.n7mn.api.*;
import xyz.n7mn.data.QueueData;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static OkHttpClient client = new OkHttpClient();
    private static int ResponsePort = 25252;
    private static int PingPort = 25253;
    private static int PingHTTPPort = 25280;
    private static String Master = "-:22552";
    private static final HashMap<String, String> QueueList = new HashMap<>();

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
                    .add("Master", "-:22552");
            ConfigYaml1 = add.build();

            try {
                config1.createNewFile();
                PrintWriter writer = new PrintWriter(config1);
                writer.print(ConfigYaml1.toString());
                writer.close();

                System.out.println("[info] config.ymlを設定してください。");
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

                //System.out.println("[Error] ProxyList is Empty!!");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            // Proxyチェック
            try {
                YamlMapping yamlMapping = Yaml.createYamlInput(config2).readYamlMapping();
                System.out.println("[info] プロキシチェック中...");
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
                            System.out.println("[info] "+s[0]+":"+s[1]+" 接続失敗");
                            continue;
                        }
                        System.out.println("[info] "+s[0]+":"+s[1]+" 接続成功");
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
                            System.out.println("[info] "+s[0]+":"+s[1]+" 接続失敗");
                            continue;
                        }
                        System.out.println("[info] "+s[0]+":"+s[1]+" 接続成功");
                    }
                }
                System.out.println("[info] プロキシチェック完了");
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
        Thread thread_master = null;
        if (Master.split(":")[0].equals("-")){
            thread_master = new Thread(() -> {

                ServerSocket svSock1 = null;
                try {
                    svSock1 = new ServerSocket(Integer.parseInt(Master.split(":")[1]));
                    System.out.println(Master+"で 同期サーバー待機開始");
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                while (true) {
                    try {
                        System.gc();
                        Socket socket = svSock1.accept();
                        new Thread(() -> {
                            try {
                                InputStream in = socket.getInputStream();
                                OutputStream out = socket.getOutputStream();
                                byte[] tempText = in.readAllBytes();
                                String requestText = new String(tempText);

                                if (requestText.equals("{\"queue\":\"getList\"}")) {
                                    QueueData[] temp = new QueueData[QueueList.size()];

                                    AtomicInteger i = new AtomicInteger();
                                    QueueList.forEach((id, url) -> {
                                        temp[i.get()] = new QueueData(id, url);
                                        i.getAndIncrement();
                                    });

                                    out.write(new Gson().toJson(temp).getBytes(StandardCharsets.UTF_8));

                                    out.flush();
                                    in.close();
                                    socket.close();
                                    return;
                                }

                                Matcher matcher = Pattern.compile("\\{\"queue\":\"(.*)\"\\}").matcher(requestText);
                                if (matcher.find()) {
                                    String[] split = matcher.group(1).split(",");

                                    if (split[1].length() != 0) {
                                        QueueList.put(split[0], split[1]);
                                    } else {
                                        QueueList.remove(split[0]);
                                    }

                                    out.write("{\"queue\": \"ok\"}".getBytes(StandardCharsets.UTF_8));
                                } else {
                                    out.write("{}}".getBytes(StandardCharsets.UTF_8));
                                }
                                out.flush();
                                in.close();
                                socket.close();

                            } catch (Exception e) {
                                e.printStackTrace();
                                try {
                                    socket.getOutputStream().write("{\"queue\":\"error\"}".getBytes(StandardCharsets.UTF_8));
                                    socket.getOutputStream().flush();
                                    socket.close();
                                } catch (IOException ex) {
                                    // ex.printStackTrace();
                                }
                            }
                        }).start();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });

            thread_master.start();
        }

        // TCP死活管理用
        Thread thread_tcp = new Thread(() -> {
            ServerSocket svSock1 = null;
            try {
                svSock1 = new ServerSocket(PingPort);
                System.out.println("Port"+PingPort+"で 死活監視用TCPサーバー待機開始");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            while (true) {
                try {
                    Socket socket = svSock1.accept();
                    OutputStream stream = socket.getOutputStream();

                    stream.write("{status: \"OK\"}".getBytes(StandardCharsets.UTF_8));
                    stream.flush();

                    stream.close();
                    System.gc();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // HTTP死活管理用
        Thread thread_http = new Thread(() -> {
            ServerSocket svSock2 = null;
            try {
                svSock2 = new ServerSocket(PingHTTPPort);
                System.out.println("Port"+PingHTTPPort+"で 死活監視用HTTPサーバー待機開始");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            while (true) {
                try {
                    Socket socket1 = svSock2.accept();
                    InputStream inputStream = socket1.getInputStream();
                    OutputStream outputStream = socket1.getOutputStream();

                    byte[] data = new byte[1073741824];
                    int readSize = inputStream.read(data);
                    if (readSize == 0){
                        outputStream.write(("HTTP/1.1 400 Bad Request\r\n" +
                                "date: " + new Date() + "\r\n" +
                                "content-type: text/plain\r\n\r\n" +
                                "400\r\n").getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();
                        socket1.close();
                        return;
                    }
                    data = Arrays.copyOf(data, readSize);

                    String text = new String(data, StandardCharsets.UTF_8);
                    Matcher matcher1 = Pattern.compile("GET").matcher(text);
                    Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);

                    String httpVersion = "1." + (matcher2.find() ? matcher2.group(1) : "1");

                    String response;
                    if (!matcher1.find()) {
                        response = "HTTP/1." + httpVersion + " 400 Bad Request\r\n" +
                                "date: " + new Date() + "\r\n" +
                                "content-type: text/plain\r\n\r\n" +
                                "400\r\n";

                    } else {
                        response = "HTTP/1." + httpVersion + " 200 OK\r\n" +
                                "date: " + new Date() + "\r\n" +
                                "Content-type: text/plain; charset=UTF-8\r\n\r\n" +
                                "ok";
                    }

                    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();

                    inputStream.close();
                    outputStream.close();

                    socket1.close();

                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Thread thread_main = new Thread(() -> {
            ServerSocket svSock = null;
            try {
                svSock = new ServerSocket(ResponsePort);

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
                        final String AccessCode = UUID.randomUUID().toString()+"-" + new Date().getTime();
                        try {
                            InputStream in = sock.getInputStream();
                            OutputStream out = sock.getOutputStream();

                            byte[] data = new byte[1073741824];
                            int readSize = in.read(data);
                            if (readSize <= 0){
                                sock.close();
                                return;
                            }
                            data = Arrays.copyOf(data, readSize);

                            String RequestHttp = new String(data, StandardCharsets.UTF_8);
                            String RequestIP = sock.getInetAddress().getHostAddress();

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
                                String url = matcher1.group(1);
                                String videoUrl = null;

                                // すでにあったら処理済みURLを返す
                                String queueURL = QueueList.get(url.split("\\?")[0]);
                                if (queueURL != null){
                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "Connection: close\r\n" +
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: " + QueueList.get(url) + "\r\n" +
                                            "Content-type: text/html; charset=UTF-8\r\n\r\n";

                                    out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                                    out.flush();
                                    in.close();
                                    out.close();
                                    sock.close();
                                    System.out.println("キャッシュ : "+ videoUrl);
                                    // TODO ログ書き出し処理
                                    return;
                                }

                                // 念のため問い合わせもする
                                if (!Master.split(":")[0].equals("-")){
                                    Socket socket = new Socket(Master.split(":")[0], Integer.parseInt(Master.split(":")[1]));

                                    OutputStream outputStream = socket.getOutputStream();
                                    InputStream inputStream = socket.getInputStream();

                                    outputStream.write("{\"queue\":\"getList\"}".getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.close();

                                    if (inputStream.readAllBytes().length == 0){
                                        inputStream.close();
                                        socket.close();
                                        return;
                                    }

                                    QueueData[] json = new Gson().fromJson(new String(inputStream.readAllBytes()), QueueData[].class);
                                    for (QueueData qData : json) {
                                        if (qData.getID().equals(url.split("\\?")[0])){
                                            httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                                    "Host: "+host+"\n" +
                                                    "Date: "+new Date()+"\r\n" +
                                                    "Connection: close\r\n" +
                                                    "X-Powered-By: Java/8\r\n" +
                                                    "Location: " + qData.getURL() + "\r\n" +
                                                    "Content-type: text/html; charset=UTF-8\r\n\r\n";

                                            out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                                            out.flush();
                                            in.close();
                                            out.close();
                                            sock.close();


                                            QueueList.put(url, qData.getURL());

                                            inputStream.close();
                                            socket.close();
                                            System.out.println("問い合わせキャッシュ : "+ videoUrl);
                                            // TODO ログ書き出し処理
                                            return;
                                        }
                                    }
                                }

                                System.gc();
                                System.out.println("キャッシュヒットせず : " + url);

                                String ErrorMessage = null;

                                Matcher matcher_NicoVideoURL = Pattern.compile("\\.nicovideo\\.jp").matcher(url);
                                Matcher matcher_BilibiliURL = Pattern.compile("bilibili(\\.com|\\.tv)").matcher(url);

                                ShareService service = null;

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
                                                videoUrl = service.getLive(url, new ProxyData(proxy[0], Integer.parseInt(proxy[1])));
                                            } else {
                                                videoUrl = service.getLive(url, null);
                                            }
                                            //System.out.println("kiteru : " + videoUrl);
                                        } else {
                                            if (proxy != null){
                                                videoUrl = service.getVideo(url, new ProxyData(proxy[0], Integer.parseInt(proxy[1])));
                                            } else {
                                                videoUrl = service.getVideo(url, null);
                                            }
                                        }
                                    } catch (Exception e){
                                        // TODO エラーログ
                                        ErrorMessage = e.getMessage();
                                        System.out.println(e.getMessage());
                                        videoUrl = null;
                                    }
                                }

                                // ビリビリ
                                boolean isBili = false;
                                if (matcher_BilibiliURL.find()){
                                    isBili = true;
                                    Matcher m = Pattern.compile("tv").matcher(url);
                                    service = m.find() ? new BilibiliTv() : new BilibiliCom();
                                    String[] split = ProxyList_Official.size() > 0 ? ProxyList_Official.get(new SecureRandom().nextInt(0, ProxyList_Official.size())).split(":") : null;

                                    try {
                                        if (split != null){
                                            videoUrl = service.getVideo(url.split("\\?")[0], new ProxyData(split[0], Integer.parseInt(split[1])));
                                        } else {
                                            videoUrl = service.getVideo(url.split("\\?")[0], null);
                                        }
                                    } catch (Exception e){
                                        // TODO エラーログ
                                        ErrorMessage = e.getMessage();
                                        videoUrl = null;
                                    }
                                }

                                // TODO ログ書き出し処理
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
                                    httpResponse = "HTTP/1."+httpVersion+" 302 Found\n" +
                                            "Host: "+host+"\n" +
                                            "Date: "+new Date()+"\r\n" +
                                            "Connection: close\r\n" +
                                            "X-Powered-By: Java/8\r\n" +
                                            "Location: " + videoUrl + "\r\n" +
                                            "Content-type: text/html; charset=UTF-8\r\n\r\n";

                                    if (!isBili){
                                        QueueList.put(url, videoUrl);
                                    }
                                }
                            } else {
                                // TODO ログ書き出し処理
                                httpResponse = "HTTP/1."+httpVersion+" 400 Bad Request\r\n" +
                                        "date: "+new Date()+"\r\n" +
                                        "content-type: application/json\r\n\r\n" +
                                        "{\"ErrorMessage\": \"Not Support\"}\r\n";
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
        });

        thread_main.start();
        thread_tcp.start();
        thread_http.start();
    }

}