package xyz.n7mn;

import com.amihaiemil.eoyaml.*;
import okhttp3.*;
import xyz.n7mn.data.VideoInfo;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
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
        List<String> ProxyList = new ArrayList<>();
        File config = new File("./config.yml");
        YamlMapping ConfigYaml = null;

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

        ServerSocket svSock = null;
        try {
            svSock = new ServerSocket(25252);
            while (true){
                Socket sock = svSock.accept();
                new Thread(()->{
                    try {
                        byte[] data = new byte[100000000];
                        InputStream in = sock.getInputStream();
                        OutputStream out = sock.getOutputStream();

                        int readSize = in.read(data);
                        data = Arrays.copyOf(data, readSize);
                        String RequestHttp = new String(data, StandardCharsets.UTF_8);
                        System.out.println("「"+RequestHttp+"」を受信しました。");
                        String text = new String(data, StandardCharsets.UTF_8);
                        Matcher matcher1 = Pattern.compile("GET /\\?vi=(.*) HTTP/1.(\\d)").matcher(text);
                        String httpResponse;

                        if (matcher1.find()){
                            // "https://www.nicovideo.jp/watch/sm10759623"
                            String videoUrl = getVideo(matcher1.group(1));

                            if (videoUrl == null || !videoUrl.startsWith("http")){
                                httpResponse = "HTTP/1.1 403 Forbidden\r\n" +
                                        "date: "+ new Date() +"\r\n" +
                                        "content-type: text/plain\r\n\r\n" +
                                        "403\r\n";
                            } else {

                                Matcher matcher = Pattern.compile("Host: (.*)\r\n").matcher(RequestHttp);
                                String host = "localhost:25252";
                                if (matcher.find()){
                                    host = matcher.group(1);
                                }

                                httpResponse = "HTTP/1."+matcher1.group(2)+" 302 Found\n" +
                                        "Host: "+host+"\n" +
                                        "Date: "+new Date()+"\r\n" +
                                        "Connection: close\r\n" +
                                        "X-Powered-By: Java/8\r\n" +
                                        "Location: " + videoUrl + "\r\n" +
                                        "Content-type: text/html; charset=UTF-8\r\n\r\n";
                            }
                        } else {
                            httpResponse = "HTTP/1."+matcher1.group(2)+" 403 Forbidden\r\n" +
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



    private static String getVideo(String url){

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

        YamlSequence list = ConfigYaml.yamlSequence("Proxy");
        for (int i = 0; i < list.size(); i++){
            ProxyList.add(list.string(i));
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        String[] split = ProxyList.get(new SecureRandom().nextInt(0, ProxyList.size())).split(":");
        String ProxyIP = split[0];
        int ProxyPort = Integer.parseInt(split[1]);
        client = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ProxyIP, ProxyPort))).build();

        System.gc();
        String resUrl = null;

        if (url.startsWith("nicovideo.jp") || url.startsWith("www.nicovideo.jp") || url.startsWith("nico.ms")){
            url = "https://"+url;
        }

        System.out.println("[Debug] 処理するURL: " + url + " "+sdf.format(new Date()));

        if (!new File("./log/").exists()){
            new File("./log/").mkdir();
        }

        try {


            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM");
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            if (!new File("./log/log-"+sdf1.format(new Date())+".txt").exists()){
                new File("./log/log-"+sdf1.format(new Date())+".txt").createNewFile();
            }

            FileWriter file = new FileWriter("./log/log-"+sdf1.format(new Date())+".txt", true);
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));
            pw.println("["+ sdf2.format(new Date())+"] 処理URL : "+url);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 送られてきたURLを一旦IDだけにする
        String id = url.replaceAll("http://nicovideo.jp/watch/","").replaceAll("https://nicovideo.jp/watch/","").replaceAll("http://www.nicovideo.jp/watch/","").replaceAll("https://www.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","");
        id = id.split("\\?")[0];

        System.out.println("[Debug] ID: " + id + " "+sdf.format(new Date()));

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
            System.out.println("[Debug] 動画情報 取得失敗 "+sdf.format(new Date()));
            e.printStackTrace();
            return resUrl;
        }

        // HTML取得
        System.out.println("[Debug] HTML取得開始 "+sdf.format(new Date()));
        final String HtmlText;
        Request request;
        if (!id.startsWith("so")){
            request = new Request.Builder()
                    .url("https://nico.ms/"+id)
                    .build();
        } else {
            request = new Request.Builder()
                    .url("https://www.nicovideo.jp/watch/"+id)
                    .build();


        }
        try {
            Response response = client.newCall(request).execute();
            HtmlText = response.body().string();
            if (id.startsWith("so")){
                System.out.println(HtmlText);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return resUrl;
        }
        System.out.println("[Debug] HTML取得完了 "+sdf.format(new Date()));



        // いろいろ必要なものを取ってくる
        String SessionId = null;
        String Token = null;
        String Signature = null;

        // セッションID
        Matcher matcher1   = Pattern.compile("playerId&quot;:&quot;nicovideo-(.*)&quot;,&quot;videos").matcher(HtmlText);
        Matcher matcher1_2 = Pattern.compile("playerId&quot;:&quot;nicovideo-(.*)&quot;,&quot;videos").matcher(HtmlText);
        if (matcher1.find()){

            SessionId = matcher1.group(1);
            System.out.println("[Debug] セッションID : "+SessionId+" "+sdf.format(new Date()));
        }

        // Tokenデータ
        Matcher matcher2 = Pattern.compile("\\{\\\\&quot;service_id\\\\&quot;:\\\\&quot;nicovideo\\\\&quot;(.*)\\\\&quot;transfer_presets\\\\&quot;:\\[\\]\\}").matcher(HtmlText);
        if (matcher2.find()){
            Token = matcher2.group().replaceAll("\\\\","").replaceAll("&quot;","\"").replaceAll("\"","\\\\\"");
            System.out.println("[Debug] TokenData : \n"+Token+"\n"+ sdf.format(new Date()));
        }

        // signature
        Matcher matcher3 = Pattern.compile("signature&quot;:&quot;(.*)&quot;,&quot;contentId").matcher(HtmlText);
        if (matcher3.find()){
            Signature = matcher3.group(1);
            System.out.println("[Debug] signature : "+Signature+" "+ sdf.format(new Date()));
        }

        if (SessionId == null || Token == null || Signature == null){
            System.out.println("[Debug] 情報取得失敗 "+ sdf.format(new Date()));
            return resUrl;
        }

        System.out.println("[Debug] JSON生成開始 "+ sdf.format(new Date()));
        String json = "{\n" +
                "\t\"session\": {\n" +
                "\t\t\"recipe_id\": \"nicovideo-"+id+"\",\n" +
                "\t\t\"content_id\": \"out1\",\n" +
                "\t\t\"content_type\": \"movie\",\n" +
                "\t\t\"content_src_id_sets\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"content_src_ids\": [\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"src_id_to_mux\": {\n" +
                "\t\t\t\t\t\t\t\"video_src_ids\": [\n" +
                "\t\t\t\t\t\t\t\t\"archive_h264_360p\",\n" +
                "\t\t\t\t\t\t\t\t\"archive_h264_360p_low\"\n" +
                "\t\t\t\t\t\t\t],\n" +
                "\t\t\t\t\t\t\t\"audio_src_ids\": [\n" +
                "\t\t\t\t\t\t\t\t\"archive_aac_64kbps\"\n" +
                "\t\t\t\t\t\t\t]\n" +
                "\t\t\t\t\t\t}\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t]\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"timing_constraint\": \"unlimited\",\n" +
                "\t\t\"keep_method\": {\n" +
                "\t\t\t\"heartbeat\": {\n" +
                "\t\t\t\t\"lifetime\": 120000\n" +
                "\t\t\t}\n" +
                "\t\t},\n" +
                "\t\t\"protocol\": {\n" +
                "\t\t\t\"name\": \"http\",\n" +
                "\t\t\t\"parameters\": {\n" +
                "\t\t\t\t\"http_parameters\": {\n" +
                "\t\t\t\t\t\"parameters\": {\n" +
                "\t\t\t\t\t\t\"http_output_download_parameters\": {\n" +
                "\t\t\t\t\t\t\t\"use_well_known_port\": \"yes\",\n" +
                "\t\t\t\t\t\t\t\"use_ssl\": \"yes\",\n" +
                "\t\t\t\t\t\t\t\"transfer_preset\": \"\"\n" +
                "\t\t\t\t\t\t}\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t}\n" +
                "\t\t},\n" +
                "\t\t\"content_uri\": \"\",\n" +
                "\t\t\"session_operation_auth\": {\n" +
                "\t\t\t\"session_operation_auth_by_signature\": {\n" +
                "\t\t\t\t\"token\": \""+Token+"\",\n" +
                "\t\t\t\t\"signature\": \""+Signature+"\"\n" +
                "\t\t\t}\n" +
                "\t\t},\n" +
                "\t\t\"content_auth\": {\n" +
                "\t\t\t\"auth_type\": \"ht2\",\n" +
                "\t\t\t\"content_key_timeout\": 600000,\n" +
                "\t\t\t\"service_id\": \"nicovideo\",\n" +
                "\t\t\t\"service_user_id\": \""+SessionId+"\"\n" +
                "\t\t},\n" +
                "\t\t\"client_info\": {\n" +
                "\t\t\t\"player_id\": \"nicovideo-"+SessionId+"\"\n" +
                "\t\t},\n" +
                "\t\t\"priority\": 0\n" +
                "\t}\n" +
                "}";

        if (id.startsWith("so")){
            json = "{\n" +
                    "    \"session\": {\n" +
                    "        \"recipe_id\": \"nicovideo-"+id+"\",\n" +
                    "        \"content_id\": \"out1\",\n" +
                    "        \"content_type\": \"movie\",\n" +
                    "        \"content_src_id_sets\": [\n" +
                    "            {\n" +
                    "                \"content_src_ids\": [\n" +
                    "                    {\n" +
                    "                        \"src_id_to_mux\": {\n" +
                    "                            \"video_src_ids\": [\n" +
                    "                                \"archive_h264_360p\",\n" +
                    "                                \"archive_h264_360p_low\"\n" +
                    "                            ],\n" +
                    "                            \"audio_src_ids\": [\n" +
                    "                                \"archive_aac_64kbps\"\n" +
                    "                            ]\n" +
                    "                        }\n" +
                    "                    },\n" +
                    "                    {\n" +
                    "                        \"src_id_to_mux\": {\n" +
                    "                            \"video_src_ids\": [\n" +
                    "                                \"archive_h264_360p_low\"\n" +
                    "                            ],\n" +
                    "                            \"audio_src_ids\": [\n" +
                    "                                \"archive_aac_64kbps\"\n" +
                    "                            ]\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                ]\n" +
                    "            }\n" +
                    "        ],\n" +
                    "        \"timing_constraint\": \"unlimited\",\n" +
                    "        \"keep_method\": {\n" +
                    "            \"heartbeat\": {\n" +
                    "                \"lifetime\": 120000\n" +
                    "            }\n" +
                    "        },\n" +
                    "        \"protocol\": {\n" +
                    "            \"name\": \"http\",\n" +
                    "            \"parameters\": {\n" +
                    "                \"http_parameters\": {\n" +
                    "                    \"parameters\": {\n" +
                    "                        \"hls_parameters\": {\n" +
                    "                            \"use_well_known_port\": \"yes\",\n" +
                    "                            \"use_ssl\": \"yes\",\n" +
                    "                            \"transfer_preset\": \"\",\n" +
                    "                            \"segment_duration\": 6000\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            }\n" +
                    "        },\n" +
                    "        \"content_uri\": \"\",\n" +
                    "        \"session_operation_auth\": {\n" +
                    "            \"session_operation_auth_by_signature\": {\n" +
                    "                \"token\": \""+Token+"\",\n" +
                    "                \"signature\": \""+Signature+"\"\n" +
                    "            }\n" +
                    "        },\n" +
                    "        \"content_auth\": {\n" +
                    "            \"auth_type\": \"ht2\",\n" +
                    "            \"content_key_timeout\": 600000,\n" +
                    "            \"service_id\": \"nicovideo\",\n" +
                    "            \"service_user_id\": \""+SessionId+"\"\n" +
                    "        },\n" +
                    "        \"client_info\": {\n" +
                    "            \"player_id\": \"nicovideo-"+SessionId+"\"\n" +
                    "        },\n" +
                    "        \"priority\": 0.2\n" +
                    "    }\n" +
                    "}";
        }

        System.out.println("[Debug] JSON生成完了 "+ sdf.format(new Date()));

        System.out.println("[Debug] 鯖へPost開始 "+ sdf.format(new Date()));
        String ResponseJson;
        RequestBody body = RequestBody.create(json, JSON);
        Request request2 = new Request.Builder()
                .url("https://api.dmc.nico/api/sessions?_format=json")
                .post(body)
                .build();
        try {
            Response response2 = client.newCall(request2).execute();
            ResponseJson = response2.body().string();
            //System.out.println(response2.body().string());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[Debug] 鯖へPost失敗 "+ sdf.format(new Date()));

            return resUrl;
        }

        System.out.println("[Debug] 鯖へPost成功 "+ sdf.format(new Date()));
        System.out.println(ResponseJson);

        // 送られてきたJSONから動画ファイルのURLとハートビート信号用のセッションを取得する
        String VideoURL = null;
        String HeartBeatSession = null;
        String HeartBeatSessionId = null;

        // 動画URL
        Matcher video_matcher = Pattern.compile("\"content_uri\":\"(.*)\",\"session_operation_auth").matcher(ResponseJson);
        if (video_matcher.find()){
            VideoURL = video_matcher.group(1).replaceAll("\\\\","");
            System.out.println("[Debug] 動画URL : "+VideoURL+" "+sdf.format(new Date()));
        }

        // ハートビート信号用 セッション
        Matcher heart_session_matcher = Pattern.compile("\\{\"meta\":\\{\"status\":201,\"message\":\"created\"},\"data\":\\{(.*)\\}").matcher(ResponseJson);
        if (heart_session_matcher.find()){
            HeartBeatSession = "{"+heart_session_matcher.group(1); //.replaceAll("\\\\","");
            System.out.println("[Debug] ハートビート信号用 セッション : \n"+HeartBeatSession+"\n"+sdf.format(new Date()));
        }
        // IDだけ抽出
        Matcher heart_session_matcher2 = Pattern.compile("\"data\":\\{\"session\":\\{\"id\":\"(.*)\",\"recipe_id\"").matcher(ResponseJson);
        if (heart_session_matcher2.find()){
            HeartBeatSessionId = heart_session_matcher2.group(1).replaceAll("\\\\","");
            System.out.println("[Debug] ハートビート信号用 セッションID : \n"+HeartBeatSessionId+"\n"+sdf.format(new Date()));
        }

        if (VideoURL == null || HeartBeatSession == null || HeartBeatSessionId == null){
            System.out.println("[Debug] 動画情報 取得失敗 "+ sdf.format(new Date()));
            return resUrl;
        }
        System.out.println("[Debug] 動画情報 取得成功\n動画URL : "+VideoURL+" \n"+ sdf.format(new Date()));
        System.gc();

        // 最低限動画の長さ分だけハートビート信号投げつける (40秒起き)
        long finalTime = time;
        String finalHeartBeatSession = HeartBeatSession;
        String finalHeartBeatSessionId = HeartBeatSessionId;
        new Thread(()->{

            for (int i = 0; i < (int)(finalTime / 40L); i++){

                System.out.println("ハートビート信号 送信 ("+(i+1)+"回目)");

                RequestBody body2 = RequestBody.create(finalHeartBeatSession, JSON);
                Request request3 = new Request.Builder()
                        .url("https://api.dmc.nico/api/sessions/"+finalHeartBeatSessionId+"?_format=json&_method=PUT")
                        .post(body2)
                        .build();
                try {
                    Response response3 = client.newCall(request3).execute();
                    String ResponseJson2 = response3.body().string();
                    System.out.println(ResponseJson2);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("[Debug] 鯖へPost失敗 "+ sdf.format(new Date()));

                    return;
                }

                System.gc();
                try {
                    Thread.sleep(40000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();


        return VideoURL;
    }

}