package net.nicovrc.dev.server;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlInput;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import net.nicovrc.dev.api.*;
import net.nicovrc.dev.data.OutputJson;
import net.nicovrc.dev.test.SiteCheck;
import okhttp3.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPServer extends Thread {
    private final int Port;

    private final CacheAPI CacheAPI;
    private final ProxyAPI ProxyAPI;
    private final ServerAPI ServerAPI;
    private final JinnnaiSystemURL_API JinnnaiAPI;
    private final ConversionAPI ConversionAPI;

    private final OkHttpClient HttpClient;

    private final Gson gson = new Gson();

    private final boolean isWebhook;
    private final String WebhookURL;

    private final ArrayList<String> WebhookList = new ArrayList<>();
    private Boolean isStop;

    private final Pattern matcher_HTTPVer = Pattern.compile("HTTP/(\\d)\\.(\\d)");
    private final Pattern matcher_Request = Pattern.compile("(GET|HEAD) /\\?vi=(.*) HTTP");
    private final Pattern matcher_CheckServer = Pattern.compile("check_server=(.+)");
    private final Pattern matcher_ForceQueue = Pattern.compile("force_queue=(.+)");
    private final Pattern matcher_DiscordURL = Pattern.compile("https://discordapp\\.com");

    private final Timer timer = new Timer();
    private final Timer timer2 = new Timer();

    public HTTPServer(CacheAPI cacheAPI, ProxyAPI proxyAPI, ServerAPI serverAPI, JinnnaiSystemURL_API jinnnaiAPI, OkHttpClient client, int Port, Boolean isStop){
        this.Port = Port;

        this.CacheAPI = cacheAPI;
        this.ProxyAPI = proxyAPI;
        this.ServerAPI = serverAPI;
        this.JinnnaiAPI = jinnnaiAPI;
        this.ConversionAPI = new ConversionAPI(ProxyAPI);

        this.HttpClient = client;
        this.isStop = isStop;

        String tWebhookURL;
        boolean tWebhook;
        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            tWebhook = yamlMapping.string("DiscordWebhook").toLowerCase(Locale.ROOT).equals("true");
            tWebhookURL = yamlMapping.string("DiscordWebhookURL");
        } catch (Exception e){
            tWebhookURL = "";
            tWebhook = false;
        }
        WebhookURL = tWebhookURL;
        isWebhook = tWebhook;

        if (!isWebhook){
            return;
        }

        if (WebhookURL.isEmpty()){
            return;
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                WebhookSendAll();
            }
        }, 0L, 60000L);
    }

    @Override
    public void run() {
        try {
            ServerSocket svSock = new ServerSocket(Port);
            System.out.println("[Info] TCP Port "+Port+"で 処理受付用HTTPサーバー待機開始");

            final boolean[] temp = {true};
            while (temp[0]) {
                try {
                    //System.gc();
                    Socket sock = svSock.accept();
                    new Thread(() -> {
                        try {
                            final InputStream in = sock.getInputStream();
                            final OutputStream out = sock.getOutputStream();

                            // リクエスト来てるなら取ってくる
                            final String httpRequest = getHttpRequest(in);
                            if (httpRequest == null){
                                in.close();
                                out.close();
                                sock.close();
                                return;
                            }

                            // HTTPバージョンがちゃんと取れているか
                            final String httpVersion = getHttpVersion(httpRequest);
                            if (httpVersion.equals("unknown")){
                                SendResult(out, "HTTP/1.1 502 Bad Gateway\nContent-Type: text/plain; charset=utf-8\n\nbad gateway");
                                in.close();
                                out.close();
                                sock.close();
                                return;
                            }


                            // 指定のURLじゃない場合はBad Requestを返す
                            final String RequestURL = getRequestURL(httpRequest);
                            if (RequestURL == null){
                                SendResult(out, "HTTP/" + httpVersion + " 400 Bad Request\nContent-Type: text/plain; charset=utf-8\n\nbad request");
                                out.close();
                                in.close();
                                sock.close();
                                return;
                            }

                            // Discord Bot(Mozilla/5.0 (compatible; Discordbot/2.0; +https://discordapp.com))
                            if (matcher_DiscordURL.matcher(httpRequest).find() && RequestURL.startsWith("http")){
                                SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: "+RequestURL+"\n\n");
                                out.close();
                                in.close();
                                sock.close();
                                return;
                            } else if (matcher_DiscordURL.matcher(httpRequest).find() && !RequestURL.startsWith("http")){
                                SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: https://nico.ms/"+RequestURL+"\n\n");
                                out.close();
                                in.close();
                                sock.close();
                                return;
                            }


                            if  (RequestURL.startsWith("check") || RequestURL.startsWith("get") || RequestURL.startsWith("force")){
                                if (RequestURL.equals("check_queue")){
                                    SendResult(out, "HTTP/" + httpVersion + " 404 Not Found\nContent-Type: text/plain; charset=utf-8\n\n404");
                                    out.close();
                                    in.close();
                                    sock.close();
                                    return;
                                }

                                // どれだけ溜まっているかのチェック用
                                if (RequestURL.equals("check_cache")) {
                                    SendResult(out, "HTTP/" + httpVersion + " 200 OK\nContent-Type: application/json; charset=utf-8\n\n" + gson.toJson(CacheAPI.getList()));
                                    out.close();
                                    in.close();
                                    sock.close();
                                    return;
                                }

                                // 生死確認
                                if (RequestURL.equals("check_health")) {
                                    String Result = "HTTP/" + httpVersion + " 200 OK\nContent-Type: text/plain; charset=utf-8\n\nへるすちぇっくー！ (Ver "+ Constant.getVersion() +")\n\n"+ProxyAPI.getListCount()+"\n\n" + ServerAPI.getListCount() + "\n\n" + CacheAPI.getListCount() + "\n\nLogQueueCount : " + ConversionAPI.getLogDataListCount();

                                    out.write(Result.getBytes(StandardCharsets.UTF_8));
                                    out.flush();
                                    out.close();
                                    in.close();
                                    sock.close();
                                    return;
                                }

                                // 生死確認その2
                                if (RequestURL.startsWith("check_server=")) {
                                    Matcher matcher = matcher_CheckServer.matcher(RequestURL);
                                    final String Result;
                                    if (matcher.find()){
                                        boolean check = ServerAPI.isCheck(matcher.group(1));
                                        Result = "HTTP/" + httpVersion + " 200 OK\nContent-Type: application/json; charset=utf-8\n\n{\"check\": \"" + (check ? "OK" : "NG") + "\"}";
                                    } else {
                                        Result = "HTTP/" + httpVersion + " 404 Not Found\nContent-Type: text/plain; charset=utf-8\n\n404";
                                    }
                                    SendResult(out, Result);
                                    out.close();
                                    in.close();
                                    sock.close();
                                    return;
                                }

                                // いろいろデータをjsonで書き出す
                                if (RequestURL.equals("get_data")){
                                    String json = new Gson().toJson(new OutputJson(ServerAPI.getList().size(), ProxyAPI.getMainProxyList().size(), ProxyAPI.getJPProxyList().size(), CacheAPI.getList().size(), WebhookList.size(), ConversionAPI.getLogDataListCount(), ConversionAPI.getServiceURLList()));

                                    String Result = "HTTP/" + httpVersion + " 200 OK\nContent-Type: application/json; charset=utf-8\n\n"+json;

                                    SendResult(out, Result);
                                    out.close();
                                    in.close();
                                    sock.close();
                                    return;
                                }

                                // サイトチェック
                                if (RequestURL.equals("check_site")){
                                    String json = new Gson().toJson(SiteCheck.Run(ProxyAPI.getMainProxyList(), ProxyAPI.getJPProxyList()));
                                    String Result = "HTTP/" + httpVersion + " 200 OK\nContent-Type: application/json; charset=utf-8\n\n"+json;

                                    SendResult(out, Result);
                                    out.close();
                                    in.close();
                                    sock.close();
                                    return;
                                }

                                // ログ強制書き出し
                                String LogWritePass = null;
                                MessageDigest md = MessageDigest.getInstance("SHA-256");
                                if (RequestURL.startsWith("force_queue")){
                                    try {

                                        YamlInput yamlInput = Yaml.createYamlInput(new File("./config.yml"));
                                        YamlMapping yamlMapping = yamlInput.readYamlMapping();
                                        byte[] digest = md.digest(yamlMapping.string("WriteLogPass").getBytes(StandardCharsets.UTF_8));
                                        yamlMapping = null;
                                        yamlInput = null;
                                        LogWritePass = HexFormat.of().withLowerCase().formatHex(digest);
                                        System.gc();
                                    } catch (Exception e){
                                        LogWritePass = null;
                                    }
                                }

                                if (RequestURL.startsWith("force_queue") && LogWritePass != null){
                                    Matcher matcher = matcher_ForceQueue.matcher(RequestURL);
                                    if (matcher.find()){
                                        String inputP = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                                        //System.out.println(inputP);
                                        byte[] digest = md.digest(inputP.getBytes(StandardCharsets.UTF_8));

                                        //System.out.println(LogWritePass + " : " + HexFormat.of().withLowerCase().formatHex(digest));
                                        if (HexFormat.of().withLowerCase().formatHex(digest).equals(LogWritePass)){
                                            //System.out.println("ok");
                                            ConversionAPI.ForceLogDataWrite();
                                            WebhookSendAll();
                                        }
                                        digest = md.digest((RequestURL+new Date().getTime()+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
                                        LogWritePass = HexFormat.of().withLowerCase().formatHex(digest);
                                    }
                                    String Result = "HTTP/" + httpVersion + " 200 OK\nContent-Type: application/json; charset=utf-8\n\n[]";
                                    SendResult(out, Result);
                                    out.close();
                                    in.close();
                                    sock.close();
                                    return;
                                }
                            }

                            try {
                                ServerExecute.run(CacheAPI, ConversionAPI, ServerAPI, JinnnaiAPI, HttpClient, in, out, sock, null, null, httpRequest, httpVersion, RequestURL, isWebhook, WebhookURL, WebhookList);
                            } catch (Exception e){
                                String Result = "HTTP/" + httpVersion + " 503 Service Unavailable\nContent-Type: text/plain; charset=utf-8\n\n503";
                                SendResult(out, Result);

                                PrintWriter io = new PrintWriter("./error-"+new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date())+".txt");
                                e.printStackTrace(io);
                                io.close();
                                temp[0] = false;
                                isStop = true;
                            }

                            in.close();
                            out.close();
                            sock.close();
                        } catch (Exception e){
                            e.printStackTrace();
                            temp[0] = false;
                            isStop = true;
                        }
                    }).start();
                } catch (Exception e){
                    e.printStackTrace();
                    temp[0] = false;
                    isStop = true;
                }

                if (isStop){
                    temp[0] = false;
                }
            }
            svSock.close();

            WebhookSendAll();
            timer.cancel();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private String getHttpRequest(InputStream in){
        try {
            byte[] data = new byte[1000000];
            int readSize = in.read(data);
            if (readSize <= 0) {
                data = null;
                return null;
            }
            data = Arrays.copyOf(data, readSize);

            //System.out.println(new String(data, StandardCharsets.UTF_8));
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e){
            return null;
        }
    }

    private String getHttpVersion(String HttpRequest){

        Matcher matcher1 = matcher_HTTPVer.matcher(HttpRequest);

        if (matcher1.find()){
            return matcher1.group(1) + "." + matcher1.group(2);
        }

        return "unknown";
    }


    private String getRequestURL(String HttpRequest){
        Matcher requestMatch = matcher_Request.matcher(HttpRequest);
        if (requestMatch.find()){
            return requestMatch.group(2);
        }
        return null;
    }

    private void SendResult(OutputStream out, String Result){
        try {
            out.write(Result.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void WebhookSendAll() {
        File file = new File("./http-webhook-lock.txt");
        if (file.exists()){
            return;
        }
        try {
            file.createNewFile();
        } catch (Exception e){
            //e.printStackTrace();
        }
        ArrayList<String> list = new ArrayList<>(WebhookList);
        WebhookList.clear();
        if (list.isEmpty()){
            file.delete();
            return;
        }

        final File finalFile = file;
        new Thread(()->{
            System.out.println("[Info] Webhook Send Start");
            list.forEach(json -> {
                try {
                    RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(WebhookURL)
                            .post(body)
                            .build();

                    Response response = HttpClient.newCall(request).execute();
                    response.close();
                } catch (Exception e){
                    //e.printStackTrace();
                }
            });
            System.out.println("[Info] Webhook Send End ("+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +")");

            finalFile.delete();
        }).start();
    }
}
