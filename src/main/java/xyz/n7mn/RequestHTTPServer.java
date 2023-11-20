package xyz.n7mn;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.data.*;
import xyz.n7mn.data.Queue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.n7mn.RequestFunction.LogWrite;

public class RequestHTTPServer extends Thread{

    private final int Port;

    public RequestHTTPServer(int port) {
        this.Port = port;
    }

    @Override
    public void run() {
        System.out.println("[Info] TCP Port "+Port+"で 処理受付用HTTPサーバー待機開始");

        // サーバーリストの構築
        final List<String> ServerList = new ArrayList<>();
        final ConcurrentHashMap<String, String> queueList = new ConcurrentHashMap<>();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File config = new File("./config.yml");
                if (!config.exists()){
                    return;
                }

                List<String> temp = new ArrayList<>();
                try {
                    YamlMapping yamlMapping = Yaml.createYamlInput(config).readYamlMapping();
                    YamlSequence nodes = yamlMapping.yamlSequence("ServerList");

                    if (nodes != null){
                        for (int i = 0; i < nodes.size(); i++){

                            try {
                                //System.out.println(nodes.string(i));
                                DatagramSocket udp_sock = new DatagramSocket();

                                String jsonText = "{\"check\"}";
                                DatagramPacket udp_packet = new DatagramPacket(jsonText.getBytes(StandardCharsets.UTF_8), jsonText.getBytes(StandardCharsets.UTF_8).length,new InetSocketAddress(nodes.string(i).split(":")[0],Integer.parseInt(nodes.string(i).split(":")[1])));
                                udp_sock.send(udp_packet);

                                byte[] temp1 = new byte[100000];
                                DatagramPacket udp_packet2 = new DatagramPacket(temp1, temp1.length);
                                udp_sock.setSoTimeout(100);
                                udp_sock.receive(udp_packet2);

                                String result = new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength()));
                                //System.out.println("受信 : " + result);

                                JsonElement json = new Gson().fromJson(result, JsonElement.class);
                                String string = json.getAsJsonObject().get("OK").getAsString();
                                UUID uuid = UUID.fromString(string);
                                udp_sock.close();
                            } catch (Exception e){
                                continue;
                            }

                            temp.add(nodes.string(i));
                        }
                    }

                } catch (IOException e) {
                    //e.printStackTrace();
                }

                ServerList.clear();
                ServerList.addAll(temp);

            }
        }, 0L, 5000L);

        // キャッシュ掃除
        Timer timer2 = new Timer();
        timer2.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                HashMap<String, String> temp = new HashMap<>(queueList);
                temp.forEach((req, res)->{
                    try {
                        OkHttpClient client = new OkHttpClient();

                        Request request_html = new Request.Builder()
                                .url(res)
                                .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0")
                                .build();
                        Response response = client.newCall(request_html).execute();
                        if (response.code() >= 200 && response.code() <= 399){
                            response.close();
                            return;
                        }
                        queueList.remove(req);
                        response.close();
                    } catch (Exception e){
                        queueList.remove(req);
                    }
                });
            }
        }, 0L, 10000L);

        // HTTP通信受け取り
        try {
            ServerSocket svSock = new ServerSocket(Port);

            boolean[] t = {true};
            while (t[0]){
                Socket sock = svSock.accept();
                new Thread(()->{
                    try {
                        final InputStream in = sock.getInputStream();
                        final OutputStream out = sock.getOutputStream();

                        byte[] data = new byte[1000000];
                        int readSize = in.read(data);
                        if (readSize <= 0){
                            sock.close();
                            return;
                        }
                        data = Arrays.copyOf(data, readSize);

                        final String httpRequest = new String(data, StandardCharsets.UTF_8);
                        String httpResult = "";

                        Matcher matcher1 = Pattern.compile("(GET|HEAD) /\\?vi=(.*) HTTP").matcher(httpRequest);
                        Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(httpRequest);

                        final String httpVersion = "1." + (matcher2.find() ? matcher2.group(1) : "1");

                        //System.out.println(httpRequest);

                        if (matcher1.find()){

                            final String RequestURL = matcher1.group(2);
                            String tempURL = RequestURL;

                            // どれだけ溜まっているかのチェック用
                            if (RequestURL.equals("check_queue")){
                                List<Queue> temp = new ArrayList<>();
                                queueList.forEach((req, res)-> temp.add(new Queue(req, res)));

                                httpResult = "HTTP/"+httpVersion+" 200 OK\nContent-Type: application/json; charset=utf-8\n\n"+new Gson().toJson(temp);

                                out.write(httpResult.getBytes(StandardCharsets.UTF_8));
                                out.flush();
                                out.close();
                                in.close();
                                sock.close();
                                return;
                            }

                            // 死活監視用
                            if (RequestURL.equals("check_health")){
                                httpResult = "HTTP/"+httpVersion+" 200 OK\nContent-Type: text/plain; charset=utf-8\n\nへるすちぇっくー！";

                                out.write(httpResult.getBytes(StandardCharsets.UTF_8));
                                out.flush();
                                out.close();
                                in.close();
                                sock.close();
                                return;
                            }

                            //System.out.println(tempURL);
                            // URL変換サービスのURLは取り除く
                            if (tempURL.startsWith("http://yt.8uro.net") || tempURL.startsWith("https://yt.8uro.net")){
                                tempURL = URLDecoder.decode(tempURL, StandardCharsets.UTF_8);
                            }
                            String[] list = {
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

                            String[] list_tube = {
                                    "http://shay.loan/",
                                    "https://shay.loan/",
                                    "http://questing.thetechnolus.com/watch?v=",
                                    "https://questing.thetechnolus.com/watch?v=",
                                    "http://questing.thetechnolus.com/v/",
                                    "https://questing.thetechnolus.com/v/",
                                    "http://youtube.irunu.co/watch?v=",
                                    "https://youtube.irunu.co/watch?v="
                            };
                            String[] list_nico = {
                                    "http://www.nicovideo.life/watch?v=",
                                    "https://www.nicovideo.life/watch?v=",
                                    "http://live.nicovideo.life/watch?v=",
                                    "https://live.nicovideo.life/watch?v=",
                            };

                            for (String str : list){
                                Matcher matcher = Pattern.compile(str.replaceAll("\\?", "\\\\?").replaceAll("\\.", "\\\\.") + "(.*)").matcher(tempURL);
                                if (matcher.find()){
                                    tempURL = matcher.group(1);
                                }
                            }
                            for (String str : list_tube){
                                Matcher matcher = Pattern.compile(str.replaceAll("\\?", "\\\\?").replaceAll("\\.", "\\\\.") + "(.*)").matcher(tempURL);
                                if (matcher.find()){
                                    tempURL = "https://youtu.be/"+matcher.group(1);
                                }
                            }
                            for (String str : list_nico){
                                Matcher matcher = Pattern.compile(str.replaceAll("\\?", "\\\\?").replaceAll("\\.", "\\\\.") + "(.*)").matcher(tempURL);
                                if (matcher.find()){
                                    tempURL = matcher.group(1);
                                }
                            }

                            if (tempURL.startsWith("sm") || tempURL.startsWith("nm") || tempURL.startsWith("so")){
                                tempURL = "https://www.nicovideo.jp/watch/"+tempURL;
                            }

                            if (tempURL.startsWith("lv")){
                                //System.out.println("lv");
                                tempURL = "https://live.nicovideo.jp/watch/"+tempURL;
                            }

                            Matcher matcher_1 = Pattern.compile("api\\.nicoad\\.nicovideo\\.jp").matcher(tempURL);
                            Matcher matcher_2 = Pattern.compile("b23\\.tv").matcher(tempURL);
                            Matcher matcher_3 = Pattern.compile("nico\\.ms").matcher(tempURL);
                            Matcher matcher_4 = Pattern.compile("https://shinchan\\.biz/player\\.html\\?video_id=(.*)").matcher(tempURL);
                            Matcher matcher_5 = Pattern.compile("(ext|commons)\\.nicovideo\\.jp").matcher(tempURL);

                            if (matcher_1.find() || matcher_2.find() || matcher_3.find()){
                                OkHttpClient build = new OkHttpClient();
                                Request request = new Request.Builder()
                                        .url(tempURL)
                                        .build();
                                Response response = build.newCall(request).execute();
                                if (response.body() != null){
                                    tempURL = response.request().url().toString();
                                }
                                response.close();
                            }

                            if (matcher_4.find()){
                                tempURL = "https://www.nicovideo.jp/watch/"+matcher_3.group(1);
                            }

                            if (matcher_5.find()){
                                tempURL = tempURL.replaceAll("ext","www").replaceAll("commons","www").replaceAll("thumb","watch").replaceAll("works", "watch");
                            }

                            // debug
                            //ServerList.add("localhost:25252");
                            //queueList.put("http://www.nicovideo.jp/watch/sm9", "https://nico.ms/sm9");
                            // URLを処理鯖に投げる

                            boolean ServerListEmpty = ServerList.isEmpty();

                            Matcher queueUrl = Pattern.compile("(nico\\.ms|nicovideo\\.jp|bilibili|tver\\.jp)").matcher(tempURL);
                            boolean isQueue = queueUrl.find();

                            String s = queueList.get(tempURL.split("\\?")[0]);
                            if (isQueue){
                                if (s != null){
                                    if (!s.equals("pre")){
                                        System.out.println("[Info] リクエスト(キャッシュ) : "+ tempURL + " ---> " + s +" ("+sdf.format(new Date())+")");

                                        httpResult = "HTTP/"+httpVersion+" 302 Found\nLocation: "+s+"\nDate: "+new Date()+"\n\n";

                                        out.write(httpResult.getBytes(StandardCharsets.UTF_8));
                                        out.flush();
                                        out.close();
                                        in.close();
                                        sock.close();

                                        final LogData logData = new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), httpRequest, sock.getInetAddress().getHostAddress(), RequestURL, s, null);
                                        final boolean isRedis;
                                        boolean isRedis1;

                                        try {
                                            YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                                            isRedis1 = yamlMapping.string("LogToRedis").equals("true");
                                        } catch (Exception e){
                                            isRedis1 = false;
                                        }
                                        isRedis = isRedis1;
                                        new Thread(()-> LogWrite(logData, isRedis)).start();
                                        return;
                                    }

                                    while (s == null || s.equals("pre")){
                                        s = queueList.get(tempURL);
                                    }

                                    System.out.println("[Info] リクエスト(キャッシュ) : "+ tempURL + " ---> " + s +" ("+sdf.format(new Date())+")");

                                    httpResult = "HTTP/"+httpVersion+" 302 Found\nLocation: "+s+"\nDate: "+new Date()+"\n\n";

                                    out.write(httpResult.getBytes(StandardCharsets.UTF_8));
                                    out.flush();
                                    out.close();
                                    in.close();
                                    sock.close();

                                    final LogData logData = new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), httpRequest, sock.getInetAddress().getHostAddress(), RequestURL, s, null);
                                    final boolean isRedis;
                                    boolean isRedis1;
                                    try {
                                        YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                                        isRedis1 = yamlMapping.string("LogToRedis").equals("true");
                                    } catch (Exception e){
                                        isRedis1 = false;
                                    }
                                    isRedis = isRedis1;
                                    new Thread(()-> LogWrite(logData, isRedis)).start();
                                    return;

                                }

                                queueList.put(tempURL.split("\\?")[0], "pre");
                            }

                            List<String> tempList = new ArrayList<>(ServerList);
                            String te = !tempList.isEmpty() ? tempList.get(new SecureRandom().nextInt(0, tempList.size() - 1)) : "";

                            String resultURL = "";
                            String title = "";
                            if (!ServerListEmpty){
                                while (!tempList.isEmpty()){
                                    try {

                                        JsonRequest request = new JsonRequest();
                                        request.setRequestCode(new String(Base64.getEncoder().encode((UUID.randomUUID() + Long.toString(new Date().getTime())).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
                                        request.setHTTPRequest(httpRequest);
                                        request.setRequestURL(RequestURL);
                                        request.setTempRequestURL(tempURL);
                                        request.setRequestServerIP(te.split(":")[0]);

                                        String jsonText = new Gson().toJson(request);

                                        DatagramSocket udp_sock = new DatagramSocket();
                                        DatagramPacket udp_packet = new DatagramPacket(jsonText.getBytes(StandardCharsets.UTF_8), jsonText.getBytes(StandardCharsets.UTF_8).length,new InetSocketAddress(te.split(":")[0],Integer.parseInt(te.split(":")[1])));
                                        udp_sock.send(udp_packet);

                                        byte[] temp = new byte[100000];
                                        DatagramPacket udp_packet2 = new DatagramPacket(temp, temp.length);
                                        udp_sock.setSoTimeout(5000);
                                        udp_sock.receive(udp_packet2);

                                        String jsonT = new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength()));
                                        //System.out.println("受信 : " + jsonT);
                                        udp_sock.close();
                                        tempList.clear();


                                        try {
                                            JsonElement json = new Gson().fromJson(jsonT, JsonElement.class);

                                            //System.out.println(json.isJsonObject());

                                            if (tempURL.startsWith("http") && json.getAsJsonObject().get("ResultURL") == null && json.getAsJsonObject().get("ErrorMessage") != null){
                                                resultURL = "https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=" + URLEncoder.encode(json.getAsJsonObject().get("ErrorMessage").getAsString(), StandardCharsets.UTF_8);
                                            } else if (!tempURL.startsWith("http")){
                                                resultURL = "https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=Not+Found";
                                            } else if (json.getAsJsonObject().get("ResultURL") != null){
                                                resultURL = json.getAsJsonObject().get("ResultURL").getAsString();
                                            } else if (json.getAsJsonObject().get("Title") != null){
                                                title = json.getAsJsonObject().get("Title").getAsString();
                                            }
                                        } catch (Exception e){
                                            // ここに来ることはないはず。
                                            resultURL = "https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);

                                            e.printStackTrace();
                                        }

                                        if (resultURL.startsWith("http://") || resultURL.startsWith("https://")){
                                            httpResult = "HTTP/"+httpVersion+" 302 Found\nLocation: "+resultURL+"\nDate: "+new Date()+"\n\n";
                                            //System.out.println(httpResult);
                                        } else {
                                            httpResult = "HTTP/"+httpVersion+" 200 OK\nContent-Type: text/plain; charset=utf-8\n\n"+title;
                                        }


                                    } catch (Exception e){
                                        tempList.remove(te);
                                        try {
                                            if (tempList.size() - 1 > 0){
                                                te = tempList.get(new SecureRandom().nextInt(0, tempList.size() - 1));
                                            } else {
                                                te = tempList.get(0);
                                            }

                                        } catch (Exception ex){

                                        }

                                    }
                                }
                            } else {
                                // String requestCode, String HTTPRequest, String serverIP, String requestURL, String tempRequestURL, List<String> proxyListVideo, List<String> proxyListOfficial, String twitcastClientId, String twitcastClientSecret
                                final List<String> proxyList = new ArrayList<>();
                                final List<String> proxyList2 = new ArrayList<>();
                                String twitcastClientID = "";
                                String twitcastSecret = "";
                                boolean isRedis;

                                try {
                                    YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                                    isRedis = yamlMapping.string("LogToRedis").equals("true");
                                } catch (Exception e){
                                    isRedis = false;
                                }

                                try {
                                    YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                                    twitcastClientID = yamlMapping.string("ClientID");
                                    twitcastSecret = yamlMapping.string("ClientSecret");
                                } catch (Exception e){
                                    // e.printStackTrace();
                                }

                                // プロキシチェック
                                Timer timer3 = new Timer();
                                timer3.scheduleAtFixedRate(new TimerTask() {
                                    @Override
                                    public void run() {
                                        List<String> temp = new ArrayList<>();

                                        try {
                                            YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config-proxy.yml")).readYamlMapping();
                                            YamlSequence list = yamlMapping.yamlSequence("VideoProxy");
                                            YamlSequence list2 = yamlMapping.yamlSequence("OfficialProxy");
                                            if (list != null){
                                                for (int i = 0; i < list.size(); i++){
                                                    String[] s = list.string(i).split(":");
                                                    try {
                                                        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                                                        OkHttpClient build = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(s[0], Integer.parseInt(s[1])))).build();
                                                        Request request_html = new Request.Builder()
                                                                .url("https://nicovrc.net/")
                                                                .build();
                                                        Response response = build.newCall(request_html).execute();
                                                        response.close();
                                                    } catch (Exception e){
                                                        continue;
                                                    }
                                                    temp.add(list.string(i));
                                                }

                                                proxyList.clear();
                                                proxyList.addAll(temp);
                                            }

                                            if (list2 != null){
                                                temp.clear();
                                                for (int i = 0; i < list2.size(); i++){
                                                    String[] s = list2.string(i).split(":");
                                                    try {
                                                        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                                                        OkHttpClient build = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(s[0], Integer.parseInt(s[1])))).build();
                                                        Request request_html = new Request.Builder()
                                                                .url("https://nicovrc.net/")
                                                                .build();
                                                        Response response = build.newCall(request_html).execute();
                                                        response.close();
                                                    } catch (Exception e){
                                                        continue;
                                                    }
                                                    temp.add(list2.string(i));
                                                }

                                                proxyList2.clear();
                                                proxyList2.addAll(temp);
                                            }

                                        } catch (Exception e){
                                            //e.printStackTrace();
                                        }
                                    }
                                }, 0L, 60000L);

                                VideoRequest request = new VideoRequest(UUID.randomUUID().toString(), httpRequest, sock.getInetAddress().getHostAddress(), RequestURL, tempURL, proxyList, proxyList2, twitcastClientID, twitcastSecret);

                                Matcher matcher = Pattern.compile("(x-nicovrc-titleget: yes|user-agent: unityplayer/)").matcher(httpRequest.toLowerCase(Locale.ROOT));

                                final VideoResult result;
                                if (!matcher.find()){
                                    result = RequestFunction.getURL(request, isRedis);
                                } else {
                                    result = RequestFunction.getTitle(request, isRedis);
                                }

                                String url = "";
                                if (result.getErrorMessage() != null && !result.getErrorMessage().isEmpty()){
                                    url = "https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=" + URLEncoder.encode(result.getErrorMessage(), StandardCharsets.UTF_8);
                                } else if (result.getResultURL() != null && (result.getResultURL().startsWith("http://") || result.getResultURL().startsWith("https://"))){
                                    url = result.getResultURL();
                                } else if (result.getTitle() != null && !result.getTitle().isEmpty()){
                                    url = result.getTitle();
                                }

                                if (url.startsWith("http://") || url.startsWith("https://")){
                                    httpResult = "HTTP/"+httpVersion+" 302 Found\nLocation: "+url+"\nDate: "+new Date()+"\n\n";
                                    //System.out.println(httpResult);
                                } else {
                                    httpResult = "HTTP/"+httpVersion+" 200 OK\nContent-Type: text/plain; charset=utf-8\n\n"+url;
                                }

                                resultURL = url;
                                //System.out.println("debug "+resultURL);
                            }

                            //System.out.println(httpResult);

                            if (isQueue){
                                queueList.remove(tempURL.split("\\?")[0]);
                                if (resultURL.startsWith("http://") || resultURL.startsWith("https://")){
                                    queueList.put(tempURL.split("\\?")[0], resultURL);
                                    if (resultURL.startsWith("https://i2v.nicovrc.net")){
                                        // エラー表示の場合は20秒で同期対象から削除
                                        String finalTempURL = tempURL.split("\\?")[0];
                                        new Thread(()->{
                                            try {
                                                Thread.sleep(20000);
                                            } catch (InterruptedException e) {
                                                //e.printStackTrace();
                                            }

                                            queueList.remove(finalTempURL);
                                        }).start();
                                    }
                                }
                            }
                            System.out.println("[Info] リクエスト : "+ tempURL + " ---> " + resultURL +" ("+sdf.format(new Date())+")");


                            out.write(httpResult.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                            out.close();
                            in.close();
                            sock.close();

                            return;
                        }

                        if (Pattern.compile("(GET|HEAD)").matcher(httpRequest).find()){
                            httpResult = "HTTP/"+httpVersion+" 404 Not Found\nContent-Type: text/plain; charset=utf-8\n\n404";
                        } else {
                            httpResult = "HTTP/"+httpVersion+" 405 Method Not Allowed\nContent-Type: text/plain; charset=utf-8\n\n405";
                        }

                        out.write(httpResult.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        out.close();
                        in.close();
                        sock.close();

                    } catch (Exception e){
                        e.printStackTrace();
                        t[0] = false;
                    }
                }).start();
            }
            svSock.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
