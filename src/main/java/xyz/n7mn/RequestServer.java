package xyz.n7mn;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.data.UDPJsonRequest;
import xyz.n7mn.data.VideoRequest;
import xyz.n7mn.data.VideoResult;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestServer extends Thread{

    private final int Port;

    private List<String> VideoProxy = new ArrayList<>();
    private List<String> VideoProxy2 = new ArrayList<>();

    private String TwitcastClientId;
    private String TwitcastClientSecret;

    public RequestServer(int port) {
        this.Port = port;

        // プロキシチェック
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
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

                        VideoProxy = temp;
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

                        VideoProxy2 = temp;
                    }

                } catch (Exception e){
                    //e.printStackTrace();
                }
            }
        }, 0L, 5000L);

        try {
            YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            TwitcastClientId = yamlMapping.string("ClientID");
            TwitcastClientSecret = yamlMapping.string("ClientSecret");
        } catch (Exception e){
            TwitcastClientId = "";
            TwitcastClientSecret = "";
        }
    }

    @Override
    public void run() {

        System.out.println("[Info] UDP Port "+Port+"で 処理受付用UDPサーバー待機開始");
        while (true){
            DatagramSocket sock = null;
            try {
                sock = new DatagramSocket(Port);

                byte[] data = new byte[100000];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                sock.receive(packet);

                if (packet.getLength() == 0){
                    sock.close();
                    continue;
                }

                String s = new String(Arrays.copyOf(packet.getData(), packet.getLength()));

                //System.out.println("受信 : \n"+s);
                InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());

                if (s.equals("{\"check\"}")){

                    byte[] bytes = ("{\"OK\": \""+ UUID.randomUUID()+"\"}").getBytes(StandardCharsets.UTF_8);

                    sock.send(new DatagramPacket(bytes, bytes.length, address));
                    sock.close();

                    System.gc();

                } else {
                    try {
                        UDPJsonRequest udpJsonRequest = new Gson().fromJson(s, UDPJsonRequest.class);
                        byte[] bytes;

                        if (udpJsonRequest.getRequestURL().isEmpty() || udpJsonRequest.getTempRequestURL().isEmpty()){
                            bytes = "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8);
                        } else {
                            VideoRequest request = new VideoRequest(udpJsonRequest.getRequestCode(), udpJsonRequest.getHTTPRequest(), udpJsonRequest.getRequestServerIP(), udpJsonRequest.getRequestURL(), udpJsonRequest.getTempRequestURL(), VideoProxy, VideoProxy2, TwitcastClientId, TwitcastClientSecret);

                            Matcher matcher = Pattern.compile("(x-nicovrc-titleget: yes|user-agent: unityplayer/)").matcher(udpJsonRequest.getHTTPRequest().toLowerCase(Locale.ROOT));

                            final VideoResult result;
                            if (matcher.find()){
                                result = RequestFunction.getTitle(request, true);
                            } else {
                                result = RequestFunction.getURL(request, true);
                            }

                            bytes = new Gson().toJson(result).getBytes(StandardCharsets.UTF_8);
                        }

                        sock.send(new DatagramPacket(bytes, bytes.length, address));
                        sock.close();

                        System.gc();
                    } catch (Exception e){
                        byte[] bytes = "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8);

                        sock.send(new DatagramPacket(bytes, bytes.length, address));
                        sock.close();

                        System.gc();
                    }
                }
            } catch (Exception e){
                if (sock != null){
                    sock.close();
                }
                e.printStackTrace();
                return;
            }
        }

    }
}
