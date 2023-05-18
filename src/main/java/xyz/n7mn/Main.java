package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import okhttp3.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import xyz.n7mn.data.VideoInfo;

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

    public static void main(String[] args) {
        // Proxy読み込み
        File config = new File("./config.yml");
        final YamlMapping ConfigYaml;
        final YamlMapping ConfigYml;

        if (!config.exists()){
            YamlMappingBuilder add = Yaml.createYamlMappingBuilder().add("Proxy", Yaml.createYamlSequenceBuilder().add("localhost:3128").add("127.0.0.1:3128").build());
            ConfigYaml = add.build();

            try {
                config.createNewFile();
                PrintWriter writer = new PrintWriter(config);
                writer.print(ConfigYaml.toString());
                writer.close();

                System.out.println("[Error] ProxyList is Empty!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // Redis設定読み込み
        File config2 = new File("./config-redis.yml");

        try {
            if (!config2.exists()){
                config2.createNewFile();

                YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
                ConfigYml = builder.add(
                        "RedisServer", "127.0.0.1"
                ).add(
                        "RedisPort", String.valueOf(Protocol.DEFAULT_PORT)
                ).add(
                        "RedisPass", ""
                ).build();

                try {
                    PrintWriter writer = new PrintWriter(config2);
                    writer.print(ConfigYml.toString());
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            } else {
                ConfigYml = Yaml.createYamlInput(config2).readYamlMapping();
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.gc();
            return;
        }

        ServerSocket svSock = null;
        try {

            svSock = new ServerSocket(25252);
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
                        // System.out.println("「"+RequestHttp+"」を受信しました。");
                        new Thread(()->{
                            JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
                            //System.out.println(ConfigYml.string("RedisServer") + " / " + ConfigYml.integer("RedisPort"));
                            Jedis jedis = jedisPool.getResource();
                            jedis.auth(ConfigYml.string("RedisPass"));
                            jedis.set("nico-proxy:log:access:"+AccessCode, RequestHttp);
                            jedis.set("nico-proxy:log:access-ip:"+AccessCode, sock.getInetAddress().getHostAddress());
                            jedis.close();
                            jedisPool.close();
                        }).start();
                        String text = new String(data, StandardCharsets.UTF_8);
                        Matcher matcher1 = Pattern.compile("GET /\\?vi=(.*) HTTP/1\\.(\\d+)").matcher(text);
                        Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);
                        String httpResponse;

                        if (matcher1.find() && matcher2.find()){
                            // "https://www.nicovideo.jp/watch/sm10759623"
                            String videoUrl = getVideo(matcher1.group(1), AccessCode);

                            if (videoUrl == null || !videoUrl.startsWith("http")){
                                httpResponse = "HTTP/1."+matcher2.group(1)+" 403 Forbidden\r\n" +
                                        "date: "+ new Date() +"\r\n" +
                                        "content-type: text/plain\r\n\r\n" +
                                        "403\r\n";
                            } else {

                                Matcher matcher = Pattern.compile("Host: (.*)\r\n").matcher(RequestHttp);
                                String host = "localhost:25252";
                                if (matcher.find()){
                                    host = matcher.group(1);
                                }

                                httpResponse = "HTTP/1."+matcher2.group(1)+" 302 Found\n" +
                                        "Host: "+host+"\n" +
                                        "Date: "+new Date()+"\r\n" +
                                        "Connection: close\r\n" +
                                        "X-Powered-By: Java/8\r\n" +
                                        "Location: " + videoUrl + "\r\n" +
                                        "Content-type: text/html; charset=UTF-8\r\n\r\n";
                            }
                        } else {
                            httpResponse = "HTTP/1."+matcher2.group(1)+" 403 Forbidden\r\n" +
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
        // Proxy読み込み
        List<String> ProxyList = new ArrayList<>();
        File config = new File("./config.yml");
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

        List<String> ProxyList1 = new ArrayList<>();
        File config1 = new File("./config-so-proxy.yml");
        YamlMapping ConfigYaml1 = null;
        try {
            if (config1.exists()){
                ConfigYaml1 = Yaml.createYamlInput(config1).readYamlMapping();
            } else {

                System.out.println("ProxyList is Empty!!!");
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        YamlSequence list = ConfigYaml.yamlSequence("Proxy");
        for (int i = 0; i < list.size(); i++){
            ProxyList.add(list.string(i));
        }

        YamlSequence list_so = ConfigYaml1.yamlSequence("Proxy");
        for (int i = 0; i < list_so.size(); i++){
            ProxyList1.add(list_so.string(i));
        }

        // Redis 読み込み
        final File config2 = new File("./config-redis.yml");
        final YamlMapping ConfigYml;
        try {
            if (!config2.exists()){
                config2.createNewFile();

                YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
                ConfigYml = builder.add(
                        "RedisServer", "127.0.0.1"
                ).add(
                        "RedisPort", String.valueOf(Protocol.DEFAULT_PORT)
                ).add(
                        "RedisPass", ""
                ).build();

                try {
                    PrintWriter writer = new PrintWriter(config2);
                    writer.print(ConfigYml.toString());
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            } else {
                ConfigYml = Yaml.createYamlInput(config2).readYamlMapping();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.gc();
            return null;
        }

        System.gc();
        String resUrl = null;

        if (url.startsWith("sp.nicovideo.jp") || url.startsWith("nicovideo.jp") || url.startsWith("www.nicovideo.jp") || url.startsWith("nico.ms")){
            url = "https://"+url;
        }

        String finalUrl = url;
        new Thread(()->{
            JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
            Jedis jedis = jedisPool.getResource();
            jedis.auth(ConfigYml.string("RedisPass"));
            jedis.set("nico-proxy:log:getURL:request:"+AccessCode, finalUrl);
            jedis.close();
            jedisPool.close();
        }).start();

        // 余計なものは削除
        url = url.replaceAll("http://nextnex.com/\\?url=","").replaceAll("https://nextnex.com/\\?url=","").replaceAll("nextnex.com/\\?url=","");
        url = url.replaceAll("http://nico.7mi.site/proxy/\\?","").replaceAll("https://nico.7mi.site/proxy/\\?","").replaceAll("nico.7mi.site/proxy/\\?","");

        // 送られてきたURLを一旦IDだけにする
        String id = url.replaceAll("http://sp.nicovideo.jp/watch/","").replaceAll("https://sp.nicovideo.jp/watch/","").replaceAll("http://nicovideo.jp/watch/","").replaceAll("https://nicovideo.jp/watch/","").replaceAll("http://www.nicovideo.jp/watch/","").replaceAll("https://www.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","");
        id = id.split("\\?")[0];

        //System.out.println("[Debug] ID: " + id + " "+sdf.format(new Date()));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        String[] split = ProxyList.get(new SecureRandom().nextInt(0, ProxyList.size())).split(":");
        String[] split2 = ProxyList1.get(new SecureRandom().nextInt(0, ProxyList1.size())).split(":");
        String ProxyIP = split[0];
        int ProxyPort = Integer.parseInt(split[1]);

        if(id.startsWith("so")){
            ProxyIP = split2[0];
            ProxyPort = Integer.parseInt(split2[1]);
        }

        client = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ProxyIP, ProxyPort))).build();
        //client = new OkHttpClient();

        // 動画情報取得 (無駄にハートビート信号送らないようにするため)
        long time = -1;

        try {
            Request request = new Request.Builder()
                    .url("https://ext.nicovideo.jp/api/getthumbinfo/"+id)
                    .build();
            Response response = client.newCall(request).execute();
            VideoInfo videoInfo = VideoInfo.newInstance(response.body().string());

            time = videoInfo.getVideoLengthBySec();
            if (videoInfo.getVideoId() == null){
                throw new Exception("取得失敗");
            }

        } catch (Exception e) {
            //System.out.println("[Debug] 動画情報 取得失敗 "+sdf.format(new Date()));
            e.printStackTrace();
            new Thread(()->{
                JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
                Jedis jedis = jedisPool.getResource();
                jedis.auth(ConfigYml.string("RedisPass"));
                jedis.set("nico-proxy:log:getURL:error:"+AccessCode, "ext.nicovideo.jp error");
                jedis.close();
                jedisPool.close();
            }).start();
            return resUrl;
        }

        // HTML取得
        //System.out.println("[Debug] HTML取得開始 "+sdf.format(new Date()));
        final String HtmlText;
        Request request = new Request.Builder()
                .url("https://www.nicovideo.jp/watch/"+id)
                .build();

        try {
            Response response = client.newCall(request).execute();
            HtmlText = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return resUrl;
        }
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
            new Thread(()->{
                JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
                Jedis jedis = jedisPool.getResource();
                jedis.auth(ConfigYml.string("RedisPass"));
                jedis.set("nico-proxy:log:getURL:error:"+AccessCode, "www.nicovideo.jp");
                jedis.close();
                jedisPool.close();
            }).start();
            return resUrl;
        }

        //System.out.println("[Debug] JSON生成開始 "+ sdf.format(new Date()));

        Matcher matcher4 = Pattern.compile("archive_h264_600kbps_360p").matcher(HtmlText);
        Matcher matcher5 = Pattern.compile("archive_h264_300kbps_360p").matcher(HtmlText);

        String json = "{\"session\":{\"recipe_id\":\"nicovideo-"+id+"\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":["+(matcher5.find() ? "\"archive_h264_300kbps_360p\"" : (matcher4.find() ? "\"archive_h264_600kbps_360p\",\"archive_h264_300kbps_360p\"" : "\"archive_h264_360p\",\"archive_h264_360p_low\""))+"],\"audio_src_ids\":[\"archive_aac_64kbps\"]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"http_output_download_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\"}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\""+Token+"\",\"signature\":\""+Signature+"\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\""+SessionId+"\"},\"client_info\":{\"player_id\":\"nicovideo-"+SessionId+"\"},\"priority\":0"+(id.startsWith("so") ? ".2" : "")+"}}";



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
            new Thread(()->{
                JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
                Jedis jedis = jedisPool.getResource();
                jedis.auth(ConfigYml.string("RedisPass"));
                jedis.set("nico-proxy:log:getURL:error:"+AccessCode, "api.dmc.nico post");
                jedis.close();
                jedisPool.close();
            }).start();
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
            new Thread(()->{
                JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
                Jedis jedis = jedisPool.getResource();
                jedis.auth(ConfigYml.string("RedisPass"));
                jedis.set("nico-proxy:log:getURL:error:"+AccessCode, "PostData");
                jedis.close();
                jedisPool.close();
            }).start();
            return resUrl;
        }
        //System.out.println("[Debug] 動画情報 取得成功\n動画URL : "+VideoURL+" \n"+ sdf.format(new Date()));
        final String vURL = VideoURL;
        new Thread(()->{
            JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
            Jedis jedis = jedisPool.getResource();
            jedis.auth(ConfigYml.string("RedisPass"));
            jedis.set("nico-proxy:log:getURL:success:"+AccessCode, vURL);
            jedis.close();
            jedisPool.close();
        }).start();
        System.gc();

        // 最低限動画の長さ分だけハートビート信号投げつける (40秒起き)
        long finalTime = time;
        String finalHeartBeatSession = HeartBeatSession;
        String finalHeartBeatSessionId = HeartBeatSessionId;

        String finalId = id;
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

}