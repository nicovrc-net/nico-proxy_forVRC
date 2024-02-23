package net.nicovrc.dev.server;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import net.nicovrc.dev.api.*;
import net.nicovrc.dev.data.OutputJson;
import net.nicovrc.dev.data.UDPPacket;
import okhttp3.*;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public UDPServer(CacheAPI cacheAPI, ProxyAPI proxyAPI, ServerAPI serverAPI, JinnnaiSystemURL_API jinnnaiAPI, OkHttpClient client, int Port){
        this.CacheAPI = cacheAPI;
        this.ProxyAPI = proxyAPI;
        this.ServerAPI = serverAPI;
        this.JinnnaiAPI = jinnnaiAPI;
        this.ConversionAPI = new ConversionAPI(ProxyAPI);

        this.Client = client;
        this.Port = Port;

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

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ArrayList<String> list = new ArrayList<>(WebhookList);
                WebhookList.clear();
                if (list.isEmpty()){
                    return;
                }

                System.out.println("[Info] Webhook Send Start");
                list.forEach(json -> {
                    new Thread(()->{
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
                    }).start();
                });
                System.out.println("[Info] Webhook Send End ("+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +")");
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
                try {
                    byte[] data = new byte[1000000];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    if (packet.getLength() == 0){
                        continue;
                    }
                    final InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());

                    String packetText = new String(Arrays.copyOf(packet.getData(), packet.getLength()));
                    final UDPPacket json;
                    try {
                        json = new Gson().fromJson(packetText, UDPPacket.class);
                    } catch (Exception e){
                        socket.send(new DatagramPacket("{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8), "{\"Bad Request\"}".getBytes(StandardCharsets.UTF_8).length, address));
                        continue;
                    }

                    //System.out.println(packetText);

                    String Request = json.getHTTPRequest();
                    if (Request == null || Request.isEmpty()){
                        Request = packetText;
                    }

                    final String RequestURL = json.getRequestURL();
                    String tempRequestURL = json.getTempRequestURL();
                    final boolean isGetTitle = json.isGetTitle();

                    if (RequestURL == null){
                        socket.send(new DatagramPacket("{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8), "{\"Bad Request\"}".getBytes(StandardCharsets.UTF_8).length, address));
                        continue;
                    }

                    if (RequestURL.equals("check")){
                        OutputJson outputJson = new OutputJson(0, ProxyAPI.getMainProxyList().size(), ProxyAPI.getJPProxyList().size(), CacheAPI.getList().size(), WebhookList.size(), 0);
                        socket.send(new DatagramPacket(new Gson().toJson(outputJson).getBytes(StandardCharsets.UTF_8), new Gson().toJson(outputJson).getBytes(StandardCharsets.UTF_8).length, address));
                        continue;
                    }

                    if (RequestURL.equals("health")){
                        json.setResultURL("OK");
                        socket.send(new DatagramPacket(new Gson().toJson(json).getBytes(StandardCharsets.UTF_8), new Gson().toJson(json).getBytes(StandardCharsets.UTF_8).length, address));
                        continue;
                    }

                    //System.out.println(packetText);
                    //System.out.println(isGetTitle);

                    ServerExecute.run(CacheAPI, ConversionAPI, ServerAPI, JinnnaiAPI, Client, null, null, null, socket, address, Request, null, RequestURL, isWebhook, WebhookURL, WebhookList);

                } catch (Exception e){
                    isTrue[0] = false;
                    e.printStackTrace();
                }
            }
            socket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }
}
