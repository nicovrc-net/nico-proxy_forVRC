package net.nicovrc.dev.server;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlInput;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import net.nicovrc.dev.api.*;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.data.OutputJson;
import net.nicovrc.dev.data.UDPPacket;
import net.nicovrc.dev.test.SiteCheck;
import okhttp3.*;

import java.io.File;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UDPServer extends Thread {

    private final CacheAPI CacheAPI;
    private final ProxyAPI ProxyAPI;
    private final ServerAPI ServerAPI;
    private final JinnnaiSystemURL_API JinnnaiAPI;
    private final ConversionAPI ConversionAPI;

    private final OkHttpClient Client;
    private final int Port;

    private final ArrayList<String> WebhookList = new ArrayList<>();

    private final boolean isWebhook;
    private final String WebhookURL;

    private Boolean isStop;

    private final HashMap<String, String> RequestIdList = new HashMap<>();

    private final Pattern matcher_ForceQueue = Pattern.compile("force_queue=(.+)");

    private final Timer timer = new Timer();

    public UDPServer(CacheAPI cacheAPI, ProxyAPI proxyAPI, ServerAPI serverAPI, JinnnaiSystemURL_API jinnnaiAPI, OkHttpClient client, int Port, Boolean isStop){
        this.CacheAPI = cacheAPI;
        this.ProxyAPI = proxyAPI;
        this.ServerAPI = serverAPI;
        this.JinnnaiAPI = jinnnaiAPI;
        this.ConversionAPI = new ConversionAPI(ProxyAPI);

        this.Client = client;
        this.Port = Port;

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
                RequestIdList.clear();
            }
        }, 0L, 60000L);
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(Port);
            System.out.println("[Info] UDP Port "+Port+"で 処理受付用UDPサーバー待機開始");

            boolean[] isTrue = {true};
            while (isTrue[0]){

                if (isStop) {
                    return;
                }

                try {
                    byte[] data = new byte[1000000];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    if (packet.getLength() == 0){
                        continue;
                    }
                    final InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());

                    String packetText = new String(Arrays.copyOf(packet.getData(), packet.getLength()));

                    //System.out.println("UDP : "+packetText);

                    final UDPPacket json;
                    try {
                        json = new Gson().fromJson(packetText, UDPPacket.class);
                    } catch (Exception e){
                        socket.send(new DatagramPacket("{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8), "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8).length, address));
                        continue;
                    }


                    String Request = json.getHTTPRequest();
                    if (Request == null || Request.isEmpty()){
                        Request = packetText;
                    }

                    if (json.getRequestID() != null){
                        if (RequestIdList.get(json.getRequestID()) != null) {
                            socket.send(new DatagramPacket("{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8), "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8).length, address));
                            continue;
                        }

                        RequestIdList.put(json.getRequestID(), json.getRequestID());
                    }


                    final String RequestURL = json.getRequestURL();

                    if (RequestURL == null){
                        socket.send(new DatagramPacket("{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8), "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8).length, address));
                        continue;
                    }

                    if (RequestURL.startsWith("get") || RequestURL.startsWith("check") || RequestURL.startsWith("force")){
                        if (RequestURL.equals("check")){
                            OutputJson outputJson = new OutputJson(0, ProxyAPI.getMainProxyList().size(), ProxyAPI.getJPProxyList().size(), CacheAPI.getList().size(), WebhookList.size(), ConversionAPI.getLogDataListCount(), ConversionAPI.getServiceURLList());
                            socket.send(new DatagramPacket(new Gson().toJson(outputJson).getBytes(StandardCharsets.UTF_8), new Gson().toJson(outputJson).getBytes(StandardCharsets.UTF_8).length, address));
                            continue;
                        }

                        if (RequestURL.equals("health")){
                            json.setResultURL("OK");
                            socket.send(new DatagramPacket(new Gson().toJson(json).getBytes(StandardCharsets.UTF_8), new Gson().toJson(json).getBytes(StandardCharsets.UTF_8).length, address));
                            continue;
                        }

                        if (RequestURL.equals("get_cache")){
                            HashMap<String, CacheData> list = CacheAPI.getList();
                            socket.send(new DatagramPacket(new Gson().toJson(list).getBytes(StandardCharsets.UTF_8), new Gson().toJson(list).getBytes(StandardCharsets.UTF_8).length, address));
                            continue;
                        }
                        // サイトチェック
                        if (RequestURL.equals("check_site")){
                            String json1 = new Gson().toJson(SiteCheck.Run(ProxyAPI.getMainProxyList(), ProxyAPI.getJPProxyList()));
                            socket.send(new DatagramPacket(new Gson().toJson(json1).getBytes(StandardCharsets.UTF_8), new Gson().toJson(json1).getBytes(StandardCharsets.UTF_8).length, address));
                            continue;
                        }

                        try {
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

                                    Matcher matcher = matcher_ForceQueue.matcher(RequestURL);
                                    if (matcher.find()){
                                        String inputP = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                                        //System.out.println(inputP);
                                        byte[] digest1 = md.digest(inputP.getBytes(StandardCharsets.UTF_8));

                                        //System.out.println(LogWritePass + " : " + HexFormat.of().withLowerCase().formatHex(digest));
                                        if (HexFormat.of().withLowerCase().formatHex(digest1).equals(LogWritePass)){
                                            //System.out.println("ok");
                                            ConversionAPI.ForceLogDataWrite();
                                            WebhookSendAll();
                                        }
                                        digest1 = md.digest((RequestURL+new Date().getTime()+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
                                        LogWritePass = HexFormat.of().withLowerCase().formatHex(digest1);
                                    }

                                    json.setResultURL("");
                                    socket.send(new DatagramPacket(new Gson().toJson(json).getBytes(StandardCharsets.UTF_8), new Gson().toJson(json).getBytes(StandardCharsets.UTF_8).length, address));
                                    continue;
                                } catch (Exception e){
                                    LogWritePass = null;
                                }
                            }
                        } catch (Exception e){
                            // e.printStackTrace();
                        }
                    }

                    //System.out.println(packetText);
                    //System.out.println(isGetTitle);

                    ServerExecute.run(CacheAPI, ConversionAPI, ServerAPI, JinnnaiAPI, Client, null, null, null, socket, address, Request, null, RequestURL, isWebhook, WebhookURL, WebhookList);

                } catch (Exception e){
                    isTrue[0] = false;
                    isStop = true;
                    e.printStackTrace();
                }


                if (isStop){
                    isTrue[0] = false;
                }
            }
            socket.close();

            WebhookSendAll();
            RequestIdList.clear();
            timer.cancel();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void WebhookSendAll() {
        File file = new File("./udp-webhook-lock.txt");
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

                    Response response = Client.newCall(request).execute();
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
