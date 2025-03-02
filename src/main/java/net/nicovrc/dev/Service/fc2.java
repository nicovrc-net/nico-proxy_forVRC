package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.fc2Result;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class fc2 implements ServiceAPI {

    private String proxy = null;
    private String url = null;

    private Pattern matcher_description = Pattern.compile("<meta name=\"description\" content=\"(.+)\" />");
    private final ConcurrentHashMap<String, fc2Result> LiveCacheList = new ConcurrentHashMap<>();

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"video.fc2.com", "live.fc2.com"};
    }

    @Override
    public void Set(String json) {
        JsonElement json_object = Function.gson.fromJson(json, JsonElement.class);

        if (json_object.isJsonObject() && json_object.getAsJsonObject().has("URL")){
            this.url = json_object.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {
        if (url == null || url.isEmpty()){
            return Function.gson.toJson(new ErrorMessage("URLがありません"));
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            proxy = Function.ProxyList.get(i);
        }

        try {
            HttpClient client;
            if (proxy == null){
                client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
            } else {
                String[] s = proxy.split(":");
                client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                        .build();
            }

            if (url.startsWith("http://video.fc2.com") || url.startsWith("https://video.fc2.com")){
                String id = "";
                int i = 0;
                for (String s : url.split("/")) {
                    if (s.equals("content")) {
                        id = url.split("/")[i + 1];
                        break;
                    }

                    i++;
                }

                fc2Result result = new fc2Result();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                String description = null;
                Matcher matcher = matcher_description.matcher(send.body());
                if (matcher.find()){
                    description = matcher.group(1);
                }
                result.setDescription(description);

                // https://video.fc2.com/api/v3/videoplayer/20250225sLDdvdLD?70a5fa0d77c2089069744c049482fef4=1&tk=&fs=0
                request = HttpRequest.newBuilder()
                        .uri(new URI("https://video.fc2.com/api/v3/videoplayer/"+id+"?70a5fa0d77c2089069744c049482fef4=1&tk=&fs=0"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (send.statusCode() != 200){
                    client.close();
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }

                JsonElement json = Function.gson.fromJson(send.body(), JsonElement.class);

                if (json.getAsJsonObject().has("videoURL")){
                    result.setURL(json.getAsJsonObject().get("videoURL").getAsString());
                }
                if (json.getAsJsonObject().has("title")){
                    result.setTitle(json.getAsJsonObject().get("title").getAsString());
                }
                if (json.getAsJsonObject().has("duration")){
                    result.setDuration(json.getAsJsonObject().get("duration").getAsLong());
                }
                if (json.getAsJsonObject().has("poster")){
                    result.setThumbnail(json.getAsJsonObject().get("poster").getAsString());
                }

                request = HttpRequest.newBuilder()
                        .uri(new URI("https://video.fc2.com/api/v3/videoplaylist/"+id+"?sh=1&fs=0"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);

                String uri = "";
                if (json.getAsJsonObject().get("playlist").getAsJsonObject().has("hq")){
                    uri = json.getAsJsonObject().get("playlist").getAsJsonObject().get("hq").getAsString();
                } else if (json.getAsJsonObject().get("playlist").getAsJsonObject().has("nq")){
                    uri = json.getAsJsonObject().get("playlist").getAsJsonObject().get("nq").getAsString();
                } else if (json.getAsJsonObject().get("playlist").getAsJsonObject().has("lq")){
                    uri = json.getAsJsonObject().get("playlist").getAsJsonObject().get("lq").getAsString();
                } else {
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }

                result.setVideoURL("https://video.fc2.com" + uri);

                client.close();
                return Function.gson.toJson(result);
            } else {

                fc2Result cache = LiveCacheList.get(url);
                if (cache != null){
                    client.close();
                    return Function.gson.toJson(cache);
                }

                String id = url.split("/")[url.split("/").length - 1];
                fc2Result result = new fc2Result();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://live.fc2.com/api/memberApi.php"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString("channel=1&profile=1&user=1&streamid="+id))
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonElement json = Function.gson.fromJson(send.body(), JsonElement.class);

                if (json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("image").getAsString().isEmpty()){
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }

                String Version = json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("version").getAsString();
                String channelid = json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("channelid").getAsString();
                result.setURL("https://live.fc2.com/"+id+"/");
                result.setTitle(json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("title").getAsString());
                result.setDescription(json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("info").getAsString());
                result.setThumbnail(json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("image").getAsString());

                String ip = null;
                boolean isIpv4 = false;
                try {
                    request = HttpRequest.newBuilder()
                            .uri(new URI("http://v6.ipv6-test.com/api/myip.php"))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    ip = send.body();

                } catch (Exception e){
                    // e.printStackTrace();
                }

                if (ip == null){
                    request = HttpRequest.newBuilder()
                            .uri(new URI("https://ipinfo.io/ip"))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    //System.out.println(send.body());
                    ip = send.body();
                    isIpv4 = true;

                }

                request = HttpRequest.newBuilder()
                        .uri(new URI("https://live.fc2.com/api/getControlServer.php"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .headers("X-Requested-With", "XMLHttpRequest")
                        .POST(HttpRequest.BodyPublishers.ofString("channel_id="+channelid+"&mode=play&orz=&channel_version="+Version+"&client_version=2.5.0++%5B1%5D&client_type=pc&client_app=browser_hls&ipv"+(isIpv4 ? "4" : "6")+"="+ip+"&comment=2"+id))
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);

                if (json.isJsonObject() && json.getAsJsonObject().has("status") && json.getAsJsonObject().get("status").getAsInt() != 11){

                    // ;
                    HttpClient client2 = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NORMAL)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build();
                    final String[] resultData = new String[]{"", "", null};
                    final Timer fc2LiveTimer = new Timer();
                    final StringBuilder sb = new StringBuilder();
                    final WebSocket.Builder wsb = client2.newWebSocketBuilder();
                    final WebSocket.Listener listener = new WebSocket.Listener(){
                        @Override
                        public void onOpen(WebSocket webSocket){
                            // 接続時
                            webSocket.sendText("", true);
                            webSocket.request(Long.MAX_VALUE);
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            // 切断時
                            fc2LiveTimer.cancel();
                            client2.close();
                            resultData[0] = "Error";
                            LiveCacheList.remove(result.getURL());
                            return null;
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            String message = data.toString();
                            sb.append(message);

                            if (!sb.toString().endsWith("}")){
                                return null;
                            }

                            message = sb.toString();
                            //System.out.println("<---- " + message);
                            sb.setLength(0);

                            try {
                                JsonElement json1 = Function.gson.fromJson(message, JsonElement.class);
                                if (json1.getAsJsonObject().get("name").getAsString().equals("connect_data")){
                                    //System.out.println("----> {\"name\":\"get_hls_information\",\"arguments\":{},\"id\":1}");
                                    webSocket.sendText("{\"name\":\"get_hls_information\",\"arguments\":{},\"id\":1}", true);

                                    final long[] count = {2};
                                    fc2LiveTimer.scheduleAtFixedRate(new TimerTask() {
                                        @Override
                                        public void run() {
                                            //System.out.println("----> {\"name\":\"heartbeat\",\"arguments\":{},\"id\":"+count[0]+"}");
                                            webSocket.sendText("{\"name\":\"heartbeat\",\"arguments\":{},\"id\":"+count[0]+"}", true);
                                            count[0]++;
                                        }
                                    }, 30000L, 30000L);

                                    return null;
                                }

                                if (json1.getAsJsonObject().get("name").getAsString().equals("control_disconnection")){
                                    resultData[0] = "Error";
                                    client2.close();
                                    fc2LiveTimer.cancel();

                                    return null;
                                }

                                if (json1.getAsJsonObject().get("name").getAsString().equals("_response_")){
                                    //resultData[0] = message;

                                    if (json1.getAsJsonObject().get("arguments").getAsJsonObject().has("playlists_high_latency")){
                                        resultData[0] = json1.getAsJsonObject().get("arguments").getAsJsonObject().get("playlists_high_latency").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                                    } else if (json1.getAsJsonObject().get("arguments").getAsJsonObject().has("playlists_middle_latency")){
                                        resultData[0] = json1.getAsJsonObject().get("arguments").getAsJsonObject().get("playlists_middle_latency").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                                    } else if (json1.getAsJsonObject().get("arguments").getAsJsonObject().has("playlists")){
                                        resultData[0] = json1.getAsJsonObject().get("arguments").getAsJsonObject().get("playlists").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                                    } else {
                                        resultData[0] = "";
                                    }

                                    return null;
                                }
                            } catch (Exception e){
                                e.printStackTrace();
                            }

                            return null;
                        }
                    };

                    //System.out.println(json.getAsJsonObject().get("url").getAsString() + "?control_token=" + json.getAsJsonObject().get("control_token").getAsString());
                    CompletableFuture<WebSocket> comp = wsb.buildAsync(new URI(json.getAsJsonObject().get("url").getAsString() + "?control_token=" + json.getAsJsonObject().get("control_token").getAsString()), listener);
                    try {
                        WebSocket webSocket = comp.get();
                        webSocket = null;
                    } catch (Exception e) {
                        client2.close();
                        client.close();
                        return Function.gson.toJson(new ErrorMessage("取得に失敗しました。("+e.getMessage()+")"));
                    }

                    //System.out.println("aaa");

                    while (resultData[1] == null || resultData[1].isEmpty()){
                        resultData[1] = resultData[0];
                        //System.out.println("debug : " + resultData[1]);
                    }

                    if (!resultData[0].equals("Error")){
                        result.setLiveURL(resultData[0]);
                    } else {
                        client2.close();
                        return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                    }

                    //System.out.println("aaaa");
                    LiveCacheList.put(result.getURL(), result);

                    client.close();
                    return Function.gson.toJson(result);

                } else {
                    client.close();
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }

            }
        } catch (Exception e){
            e.printStackTrace();
            return Function.gson.toJson(new ErrorMessage("内部エラーです。("+e.getMessage()+")"));
        }

    }

    @Override
    public String getServiceName() {
        return "fc2";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
