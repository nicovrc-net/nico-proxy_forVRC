package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
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
                            Matcher matcher_VideoURL = Pattern.compile(".*(www\\.nicovideo\\.jp|nicovideo\\.jp/watch/(sm|nm|so)|sp\\.nicovideo\\.jp/watch/|nico\\.ms/(sm|nm|so)).*").matcher(url);
                            if (matcher_VideoURL.find()){
                                //System.out.println("video");
                                videoUrl = getVideo(url, AccessCode);
                            } else {
                                //System.out.println(url);
                                //System.out.println("live");
                                videoUrl = getLive(url, AccessCode);
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



    private static String getVideo(String url, String AccessCode){
        System.gc();
        
        // Proxy読み込み
        List<String> ProxyList_video = new ArrayList<>();
        List<String> ProxyList_official = new ArrayList<>();
        
        File config = new File("./config-proxy.yml");
        YamlMapping ConfigYaml = null;
        try {
            if (config.exists()){
                ConfigYaml = Yaml.createYamlInput(config).readYamlMapping();
            } else {
                System.out.println("ProxyList is Empty!!!");
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
        YamlSequence list = ConfigYaml.yamlSequence("VideoProxy");
        for (int i = 0; i < list.size(); i++){
            ProxyList_video.add(list.string(i));
        }

        YamlSequence list_so = ConfigYaml.yamlSequence("OfficialProxy");
        for (int i = 0; i < list_so.size(); i++){
            ProxyList_official.add(list_so.string(i));
        }
        
        if (ProxyList_video.size() == 0 || ProxyList_official.size() == 0){
            list = null;
            list_so = null;

            System.gc();
            
            System.out.println("ProxyList is Empty!!!");
            return null;
        }
        
        list = null;
        list_so = null;        

        System.gc();
        String resUrl = null;

        if (url.startsWith("sp.nicovideo.jp") || url.startsWith("nicovideo.jp") || url.startsWith("www.nicovideo.jp") || url.startsWith("nico.ms")){
            url = "https://"+url;
        }

        LogRedisWrite(AccessCode, "getURL:request",url);

        // 余計なものは削除
        url = url.replaceAll("http://nextnex.com/\\?url=","").replaceAll("https://nextnex.com/\\?url=","").replaceAll("nextnex.com/\\?url=","");
        url = url.replaceAll("http://nico.7mi.site/proxy/\\?","").replaceAll("https://nico.7mi.site/proxy/\\?","").replaceAll("nico.7mi.site/proxy/\\?","");

        // 送られてきたURLを一旦IDだけにする
        String id = url.replaceAll("http://sp.nicovideo.jp/watch/","").replaceAll("https://sp.nicovideo.jp/watch/","").replaceAll("http://nicovideo.jp/watch/","").replaceAll("https://nicovideo.jp/watch/","").replaceAll("http://www.nicovideo.jp/watch/","").replaceAll("https://www.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","");
        id = id.split("\\?")[0];

        //System.out.println("[Debug] ID: " + id + " "+sdf.format(new Date()));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        String[] split;
        if (!id.startsWith("so")){
            split = ProxyList_video.get(new SecureRandom().nextInt(0, ProxyList_video.size())).split(":");
        } else {
            split = ProxyList_official.get(new SecureRandom().nextInt(0, ProxyList_official.size())).split(":");
        }
        String ProxyIP = split[0];
        int ProxyPort = Integer.parseInt(split[1]);


        client = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ProxyIP, ProxyPort))).build();
        //client = new OkHttpClient();

        // 動画情報取得 (無駄にハートビート信号送らないようにするため)


        // HTML取得
        //System.out.println("[Debug] HTML取得開始 "+sdf.format(new Date()));
        final String HtmlText;
        Request request_html = new Request.Builder()
                .url("https://www.nicovideo.jp/watch/"+id)
                .build();

        try {
            Response response = client.newCall(request_html).execute();
            HtmlText = response.body().string();
        } catch (IOException e) {
            //e.printStackTrace();
            LogRedisWrite(AccessCode, "getURL:error","www.nicovideo.jp");
            return resUrl;
        }

        Matcher matcher = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">").matcher(HtmlText);
        if (!matcher.find()){
            LogRedisWrite(AccessCode, "getURL:error","not found");
            return null;
        }

        long time = Long.parseLong(matcher.group(1));

        //System.out.println("[Debug] HTML取得完了 "+sdf.format(new Date()));
        //System.out.println(HtmlText);


        // いろいろ必要なものを取ってくる
        String SessionId = null;
        String Token = null;
        String Signature = null;

        // セッションID
        Matcher matcher1 = Pattern.compile("player_id\\\\&quot;:\\\\&quot;nicovideo-(.*)\\\\&quot;,\\\\&quot;recipe_id").matcher(HtmlText);

        if (matcher1.find()){
            SessionId = matcher1.group(1);
            //System.out.println("[Debug] セッションID : "+SessionId+" "+sdf.format(new Date()));
        }

        // Tokenデータ
        Matcher matcher2 = Pattern.compile("\\{\\\\&quot;service_id\\\\&quot;:\\\\&quot;nicovideo\\\\&quot;(.*)\\\\&quot;transfer_presets\\\\&quot;:\\[\\]\\}").matcher(HtmlText);
        if (matcher2.find()){
            Token = matcher2.group().replaceAll("\\\\","").replaceAll("&quot;","\"").replaceAll("\"","\\\\\"");
            //System.out.println("[Debug] TokenData : \n"+Token+"\n"+ sdf.format(new Date()));
        }

        // signature
        Matcher matcher3 = Pattern.compile("signature&quot;:&quot;(.*)&quot;,&quot;contentId").matcher(HtmlText);
        if (matcher3.find()){
            Signature = matcher3.group(1);
            //System.out.println("[Debug] signature : "+Signature+" "+ sdf.format(new Date()));
        }

        if (SessionId == null && Token == null && Signature == null){
            //System.out.println("[Debug] 情報取得失敗 "+ sdf.format(new Date()));
            LogRedisWrite(AccessCode, "getURL:error","www.nicovideo.jp");
            return resUrl;
        }

        //System.out.println("[Debug] JSON生成開始 "+ sdf.format(new Date()));

        Matcher matcher4 = Pattern.compile("archive_h264_600kbps_360p").matcher(HtmlText);
        Matcher matcher5 = Pattern.compile("archive_h264_300kbps_360p").matcher(HtmlText);
        Matcher matcher_http = Pattern.compile("&quot;http&quot;").matcher(HtmlText);

        String json = "{\"session\":{\"recipe_id\":\"nicovideo-"+id+"\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":["+(matcher5.find() ? "\"archive_h264_300kbps_360p\"" : (matcher4.find() ? "\"archive_h264_600kbps_360p\",\"archive_h264_300kbps_360p\"" : "\"archive_h264_360p\",\"archive_h264_360p_low\""))+"],\"audio_src_ids\":[\"archive_aac_64kbps\"]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"http_output_download_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\"}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\""+Token+"\",\"signature\":\""+Signature+"\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\""+SessionId+"\"},\"client_info\":{\"player_id\":\"nicovideo-"+SessionId+"\"},\"priority\":0"+(id.startsWith("so") ? ".2" : "")+"}}";
        if (!matcher_http.find()){
            Matcher matcher_hls1 = Pattern.compile("hls_encrypted_key\\\\&quot;:\\\\&quot;(.*)\\\\&quot;}&quot;,&quot;signature").matcher(HtmlText);
            Matcher matcher_hls2 = Pattern.compile("&quot;keyUri&quot;:&quot;(.*)&quot;},&quot;movie").matcher(HtmlText);
            Matcher matcher_hls3 = Pattern.compile(",&quot;token&quot;:&quot;(.*)&quot;,&quot;signature&quot;:&quot;").matcher(HtmlText);

            String hls_encrypted_key = "";
            if (matcher_hls1.find()){
                hls_encrypted_key = matcher_hls1.group(1).replaceAll("\\\\","");
            }
            String keyUri = "";
            if (matcher_hls2.find()){
                keyUri = matcher_hls2.group(1).replaceAll("\\\\","").replaceAll("&amp;","&");
            }

            if (matcher_hls3.find()){
                Token = matcher_hls3.group(1).replaceAll("&quot;","\"");
            }

            //System.out.println(hls_encrypted_key);
            //System.out.println(keyUri);
            //System.out.println(SessionId);

            json = "{\"session\":{\"recipe_id\":\"nicovideo-"+id+"\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_360p\",\"archive_h264_360p_low\"],\"audio_src_ids\":[\"archive_aac_64kbps\"]}},{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_360p_low\"],\"audio_src_ids\":[\"archive_aac_64kbps\"]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\",\"segment_duration\":6000,\"encryption\":{\"hls_encryption_v1\":{\"encrypted_key\":\""+hls_encrypted_key+"\",\"key_uri\":\""+keyUri+"\"}}}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\""+Token+"\",\"signature\":\""+Signature+"\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\""+SessionId+"\"},\"client_info\":{\"player_id\":\"nicovideo-"+SessionId+"\"},\"priority\":0.2}}";
        }


        //System.out.println(json);
        //System.out.println("[Debug] JSON生成完了 "+ sdf.format(new Date()));

        //System.out.println("[Debug] 鯖へPost開始 "+ sdf.format(new Date()));
        String ResponseJson;
        RequestBody body = RequestBody.create(json, JSON);

        Request request2 = new Request.Builder()
                .url("https://api.dmc.nico/api/sessions?_format=json")
                .post(body)
                .build();
        try {
            Response response2 = client.newCall(request2).execute();
            ResponseJson = response2.body().string();
            //System.out.println(ResponseJson);
        } catch (IOException e) {
            e.printStackTrace();
            //System.out.println("[Debug] 鯖へPost失敗 "+ sdf.format(new Date()));
            LogRedisWrite(AccessCode, "getURL:error","api.dmc.nico post");
            return resUrl;
        }

        //System.out.println("[Debug] 鯖へPost成功 "+ sdf.format(new Date()));
        //System.out.println(ResponseJson);

        // 送られてきたJSONから動画ファイルのURLとハートビート信号用のセッションを取得する
        String VideoURL = null;
        String HeartBeatSession = null;
        String HeartBeatSessionId = null;

        // 動画URL
        Matcher video_matcher = Pattern.compile("\"content_uri\":\"(.*)\",\"session_operation_auth").matcher(ResponseJson);
        if (video_matcher.find()){
            VideoURL = video_matcher.group(1).replaceAll("\\\\","");
            //System.out.println("[Debug] 動画URL : "+VideoURL+" "+sdf.format(new Date()));
        }

        // ハートビート信号用 セッション
        Matcher heart_session_matcher = Pattern.compile("\\{\"meta\":\\{\"status\":201,\"message\":\"created\"},\"data\":\\{(.*)\\}").matcher(ResponseJson);
        if (heart_session_matcher.find()){
            HeartBeatSession = "{"+heart_session_matcher.group(1); //.replaceAll("\\\\","");
            //System.out.println("[Debug] ハートビート信号用 セッション : \n"+HeartBeatSession+"\n"+sdf.format(new Date()));
        }
        // IDだけ抽出
        Matcher heart_session_matcher2 = Pattern.compile("\"data\":\\{\"session\":\\{\"id\":\"(.*)\",\"recipe_id\"").matcher(ResponseJson);
        if (heart_session_matcher2.find()){
            HeartBeatSessionId = heart_session_matcher2.group(1).replaceAll("\\\\","");
            //System.out.println("[Debug] ハートビート信号用 セッションID : \n"+HeartBeatSessionId+"\n"+sdf.format(new Date()));
        }

        if (VideoURL == null || HeartBeatSession == null || HeartBeatSessionId == null){
            //System.out.println("[Debug] 動画情報 取得失敗 "+ sdf.format(new Date()));
            LogRedisWrite(AccessCode, "getURL:error","PostData");
            return resUrl;
        }
        //System.out.println("[Debug] 動画情報 取得成功\n動画URL : "+VideoURL+" \n"+ sdf.format(new Date()));
        LogRedisWrite(AccessCode, "getURL:success",VideoURL);
        System.gc();
        //System.out.println(VideoURL);

        // 最低限動画の長さ分だけハートビート信号投げつける (40秒起き)
        long finalTime = time;
        String finalHeartBeatSession = HeartBeatSession;
        String finalHeartBeatSessionId = HeartBeatSessionId;

        new Thread(()->{
            Timer timer = new Timer();

            Integer[] i = {0};
            int maxCount = (int)(finalTime / 40L);

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    //System.out.println(finalId +" でのハートビート信号 送信 ("+(i[0]+1)+"回目)");

                    RequestBody body = RequestBody.create(finalHeartBeatSession, JSON);
                    Request request = new Request.Builder()
                            .url("https://api.dmc.nico/api/sessions/"+finalHeartBeatSessionId+"?_format=json&_method=PUT")
                            .post(body)
                            .build();
                    try {
                        Response response = client.newCall(request).execute();
                        String ResponseJson2 = response.body().string();
                        //System.out.println(ResponseJson2);
                    } catch (IOException e) {
                        e.printStackTrace();
                        //System.out.println("[Debug] 鯖へPost失敗 "+ sdf.format(new Date()));
                        LogRedisWrite(AccessCode, "getURL:error","Send HeartBeat");
                        System.gc();
                        return;
                    }
                    System.gc();

                    if (i[0] >= maxCount){
                        timer.cancel();
                    }

                    i[0]++;
                }
            };

            timer.scheduleAtFixedRate(task, 0L, 40000L);
        }).start();

        return VideoURL;
    }

    private static String getLive(String url, String AccessCode) {
        //System.out.println("aa");
        // Proxy読み込み
        List<String> ProxyList = new ArrayList<>();
        File config = new File("./config-proxy.yml");
        YamlMapping ConfigYaml = null;
        try {
            if (config.exists()) {
                ConfigYaml = Yaml.createYamlInput(config).readYamlMapping();
            } else {
                System.out.println("ProxyList is Empty!!!");
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        YamlSequence list = ConfigYaml.yamlSequence("OfficialProxy");
        for (int i = 0; i < list.size(); i++) {
            ProxyList.add(list.string(i));
        }

        System.gc();

        if (!url.startsWith("http")){
            url = "https://";
        }

        // 余計なものは削除
        url = url.replaceAll("http://nextnex.com/\\?url=","").replaceAll("https://nextnex.com/\\?url=","").replaceAll("nextnex.com/\\?url=","");
        url = url.replaceAll("http://nico.7mi.site/proxy/\\?","").replaceAll("https://nico.7mi.site/proxy/\\?","").replaceAll("nico.7mi.site/proxy/\\?","");


        LogRedisWrite(AccessCode, "getURL:request",url);

        // 送られてきたURLを一旦IDだけにする
        String id = url.replaceAll("http://sp.live.nicovideo.jp/watch/","").replaceAll("https://sp.live.nicovideo.jp/watch/","").replaceAll("http://live.nicovideo.jp/watch/","").replaceAll("https://live.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","");
        id = id.split("\\?")[0];

        //System.out.println("[Debug] ID: " + id + " "+sdf.format(new Date()));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        String[] split = ProxyList.get(new SecureRandom().nextInt(0, ProxyList.size())).split(":");
        String ProxyIP = split[0];
        int ProxyPort = Integer.parseInt(split[1]);

        client = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ProxyIP, ProxyPort))).build();
        //client = new OkHttpClient();

        String htmlText = null;
        try {
            Request request = new Request.Builder()
                    .url("https://live.nicovideo.jp/watch/"+id)
                    .build();
            Response response = client.newCall(request).execute();
            htmlText = response.body().string();

        } catch (Exception e) {
            //System.out.println("[Debug] 動画情報 取得失敗 "+sdf.format(new Date()));
            e.printStackTrace();
            LogRedisWrite(AccessCode, "getURL:error","live.nicovideo.jp");
            //System.out.println("live.nicovideo.jp html error");
            return null;
        }

        final String[] resUrl = {null};

        Matcher matcher  = Pattern.compile("webSocketUrl&quot;:&quot;wss://(.*)&quot;,&quot;csrfToken").matcher(htmlText);
        //Matcher matcher2  = Pattern.compile("webSocketUrl&quot;:&quot;wss://(.*)&quot;,&quot;csrfToken").matcher(htmlText);
        //Matcher matcher1 = Pattern.compile("webSocketUrl&quot;:&quot;wss://(.*)&quot;,").matcher(htmlText);
        Matcher matcher_quality = Pattern.compile("\"stream_quality\":\"(.*)\"}}").matcher(htmlText);

        if (!matcher.find()){
            LogRedisWrite(AccessCode, "getURL:error","live.nicovideo.jp");
            //System.out.println("live.nicovideo.jp html error");
            System.out.println(htmlText);

            return null;
        }

        final String quality = matcher_quality.find() ? matcher_quality.group(1) : null;

        Request request = new Request.Builder()
                .url("wss://"+matcher.group(1))
                .build();


        resUrl[0] = "loading...";

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                //System.out.println("---- result text ----");
                //System.out.println(text);
                //System.out.println("---- result text ----");

                if (text.startsWith("{\"type\":\"serverTime\",\"data\":{")){
                    webSocket.send("{\"type\":\"getEventState\",\"data\":{}}");
                    //System.out.println("{\"type\":\"getEventState\",\"data\":{}}");
                }
                if (text.startsWith("{\"type\":\"eventState\",\"data\":{\"commentState\":{\"locked\":false,\"layout\":\"normal\"}}}")){
                    webSocket.send("{\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}");
                    //System.out.println("{\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}");
                }

                if (text.equals("{\"type\":\"ping\"}")){
                    webSocket.send("{\"type\":\"pong\"}");
                }

                if (text.startsWith("{\"type\":\"statistics\",\"data\":{")){
                    webSocket.send("{\"type\":\"keepSeat\"}");
                    //System.out.println("{\"type\":\"keepSeat\"}");
                }

                Matcher matcherData = Pattern.compile("\\{\"type\":\"stream\",\"data\":\\{\"uri\":\"https://").matcher(text);

                if (matcherData.find()) {
                    Matcher matcher = Pattern.compile("\"uri\":\"(.*)\",\"syncUri\":\"").matcher(text);
                    System.out.println("url get");
                    if (matcher.find()){
                        System.out.println("url get ok");
                        resUrl[0] = matcher.group(1);
                        //System.out.println(resUrl[0]);
                    } else {
                        resUrl[0] = "not";
                        LogRedisWrite(AccessCode, "getURL:error","Live Websocket Error");
                        webSocket.cancel();
                    }

                }
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {

            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
               //System.out.println("websocket open");
                if (quality == null){
                    webSocket.send("{\"type\":\"startWatching\",\"data\":{\"stream\":{\"quality\":\"abr\",\"protocol\":\"hls\",\"latency\":\"low\",\"chasePlay\":false},\"room\":{\"protocol\":\"webSocket\",\"commentable\":true},\"reconnect\":false}}");
                } else {
                    webSocket.send("{\"type\":\"startWatching\",\"data\":{\"stream\":{\"quality\":\""+quality+"\",\"protocol\":\"hls\",\"latency\":\"low\",\"chasePlay\":false},\"room\":{\"protocol\":\"webSocket\",\"commentable\":true},\"reconnect\":false}}");
                }

            }
        });

        boolean isFound = false;
        while (!isFound){
            isFound = !resUrl[0].startsWith("loading...");
        }
        //System.out.println("成功 : "+ resUrl[0]);
        LogRedisWrite(AccessCode, "getURL:success", resUrl[0]);

        System.gc();
        return resUrl[0];
    }

    private static void LogRedisWrite(String AccessCode, String Category, String Value){
        new Thread(()->{
            // Redis 読み込み
            final File config = new File("./config-redis.yml");
            final YamlMapping ConfigYml;
            try {
                if (!config.exists()){
                    return;
                } else {
                    ConfigYml = Yaml.createYamlInput(config).readYamlMapping();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.gc();
                return;
            }

            // 書き出し
            JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
            Jedis jedis = jedisPool.getResource();
            jedis.auth(ConfigYml.string("RedisPass"));
            jedis.set("nico-proxy:log:"+Category+":"+AccessCode, Value);
            jedis.close();
            jedisPool.close();
        }).start();
        System.gc();
    }

}