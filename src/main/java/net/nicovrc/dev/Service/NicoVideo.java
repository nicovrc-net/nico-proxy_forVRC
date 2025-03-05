package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.NicoNicoVideo;

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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicoVideo implements ServiceAPI {
    private final Gson gson = Function.gson;
    private final String[] SupportURL = {
            "www.nicovideo.jp",
            "live.nicovideo.jp",
            "nico.ms",
            "cas.nicovideo.jp",
            "sm",
            "nm",
            "am",
            "fz",
            "ut",
            "dm",
            "so",
            "ax",
            "ca",
            "cd",
            "cw",
            "fx",
            "ig",
            "na",
            "om",
            "sd",
            "sk",
            "yk",
            "yo",
            "za",
            "zb",
            "zc",
            "zd",
            "ze",
            "nl",
            "ch"
    };
    private final Pattern NicoID1 = Pattern.compile("(http|https)://(live|www)\\.nicovideo\\.jp/watch/(.+)");
    private final Pattern NicoID2 = Pattern.compile("(http|https)://nico\\.ms/(.+)");
    private final Pattern NicoID3 = Pattern.compile("(http|https)://cas\\.nicovideo\\.jp/user/(.+)");
    private final Pattern NicoID4 = Pattern.compile("^(sm\\d+|nm\\d+|am\\d+|fz\\d+|ut\\d+|dm\\d+|so\\d+|ax\\d+|ca\\d+|cd\\d+|cw\\d+|fx\\d+|ig\\d+|na\\d+|om\\d+|sd\\d+|sk\\d+|yk\\d+|yo\\d+|za\\d+|zb\\d+|zc\\d+|zd\\d+|ze\\d+|nl\\d+|ch\\d+|\\d+)");
    private String URL = null;

    private final Pattern matcher_Json = Pattern.compile("<meta name=\"server-response\" content=\"\\{(.+)}\" />");
    private final Pattern matcher_JsonNico = Pattern.compile("<script id=\"embedded-data\" data-props=\"\\{(.+)\\}\"");

    private final Pattern matcher_videoError1 = Pattern.compile("(この動画は存在しないか、削除された可能性があります。|お探しのページは、すでに削除されたか存在しない可能性があります|errorCode&quot;:&quot;NOT_FOUND&)");
    private final Pattern matcher_videoError2 = Pattern.compile("この動画は(.+)の申立により、著作権侵害として削除されました。");

    private final ConcurrentHashMap<String, NicoNicoVideo> LiveCacheList = new ConcurrentHashMap<>();

    private String Proxy = null;

    private String user_session = null;
    private String user_session_secure = null;

    @Override
    public String[] getCorrespondingURL() {
        return SupportURL;
    }

    @Override
    public void Set(String json) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.URL = jsonElement.getAsJsonObject().get("URL").getAsString();
        }

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("user_session")){
            this.user_session = jsonElement.getAsJsonObject().get("user_session").getAsString();
        }
        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("user_session_secure")){
            this.user_session_secure = jsonElement.getAsJsonObject().get("user_session_secure").getAsString();
        }

        //System.out.println(user_session + " / " + user_session_secure);

    }

    @Override
    public String Get() {
        if (URL == null || URL.isEmpty()){
            return gson.toJson(new ErrorMessage("URLがありません"));
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            Proxy = Function.ProxyList.get(i);
        }

        String url = URL.split("\\?")[0];
        Matcher matcher_normal = NicoID1.matcher(url);
        Matcher matcher_short = NicoID2.matcher(url);
        Matcher matcher_cas = NicoID3.matcher(url);
        Matcher matcher_idOnly = NicoID4.matcher(url);

        boolean isNormal = matcher_normal.find();
        boolean isShort = matcher_short.find();
        boolean isCas = matcher_cas.find();
        boolean isID = matcher_idOnly.find();

        String id = "";

        if (!isNormal && !isShort && !isCas && !isID){
            url = null;
            matcher_normal = null;
            matcher_short = null;
            matcher_cas = null;
            matcher_idOnly = null;
            return gson.toJson(new ErrorMessage("URLが間違っているか対応してないURLです。"));
        }

        String accessUrl = null;
        if (isID){
            id = matcher_idOnly.group(1);
            accessUrl = "https://nico.ms/" + id;
        }
        if (isNormal){
            id = matcher_normal.group(3);
            accessUrl = "https://nico.ms/" + id;
        }

        if (isShort){
            id = matcher_short.group(2);
        }

        if (isShort || isCas){
            accessUrl = url;
        }

        if (isID || isNormal || isShort){
            if (id.startsWith("lv") || id.startsWith("so")){
                if (!Function.JP_ProxyList.isEmpty()){
                    int i = new SecureRandom().nextInt(0, Function.JP_ProxyList.size());
                    Proxy = Function.JP_ProxyList.get(i);
                }
            }
        }

        if (isCas){
            if (!Function.JP_ProxyList.isEmpty()){
                int i = new SecureRandom().nextInt(0, Function.JP_ProxyList.size());
                Proxy = Function.JP_ProxyList.get(i);
            }
        }

        //System.out.println(accessUrl);

        NicoNicoVideo result = new NicoNicoVideo();
        try {
            HttpClient client;
            if (Proxy == null){
                client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
            } else {
                String[] s = Proxy.split(":");
                client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                        .build();
            }

            URI uri = new URI(accessUrl);
            HttpRequest request = user_session != null && user_session_secure != null ? HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Cookie", "user_session="+user_session+"; user_session_secure="+user_session_secure)
                    .GET()
                    .build() :
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .GET()
                            .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (send.statusCode() >= 400){
                uri = null;
                request = null;
                client.close();
                client = null;

                //System.out.println(send.body());

                Matcher matcher = matcher_videoError1.matcher(send.body());
                if (matcher.find()){
                    matcher = null;
                    return gson.toJson(new ErrorMessage("この動画は存在しないか、削除された可能性があります。"));
                }
                matcher = matcher_videoError2.matcher(send.body());
                if (matcher.find()){
                    String str = matcher.group(1);
                    matcher = null;
                    return gson.toJson(new ErrorMessage("この動画は"+str+"の申立により、著作権侵害として削除されました。"));
                }
                matcher = null;
                return gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")"));
            }

            String body = send.body();
            //System.out.println(body);
            Matcher matcher = matcher_Json.matcher(body);
            JsonElement json = null;
            if (matcher.find()){
                json = gson.fromJson("{" + matcher.group(1).replaceAll("&quot;", "\"") + "}", JsonElement.class);
            } else {
                matcher = matcher_JsonNico.matcher(body);
                if (matcher.find()){
                    json = gson.fromJson("{" + matcher.group(1).replaceAll("&quot;", "\"") + "}", JsonElement.class);
                }
            }
            matcher = null;
            body = null;

            //System.out.println(json);
            //return json.toString();

            uri = null;
            request = null;
            client.close();
            client = null;

            if (json != null){
                if (json.isJsonObject() && json.getAsJsonObject().has("data")){
                    String nicosid = json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("client").getAsJsonObject().get("nicosid").getAsString();
                    // 動画
                    result.setURL(json.getAsJsonObject().get("data").getAsJsonObject().get("metadata").getAsJsonObject().get("jsonLds").getAsJsonArray().get(0).getAsJsonObject().get("@id").getAsString());
                    result.setTitle(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("title").getAsString());
                    result.setDescription(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("description").getAsString());

                    JsonArray array = json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("tag").getAsJsonObject().get("items").getAsJsonArray();
                    String[] tags = new String[array.size()];
                    int i = 0;
                    for (JsonElement element : array) {
                        tags[i] = element.getAsJsonObject().get("name").getAsString();
                        i++;
                    }
                    result.setTags(tags);

                    result.setViewCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("view").getAsLong());
                    result.setCommentCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("comment").getAsLong());
                    result.setMyListCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("mylist").getAsLong());
                    result.setLikeCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("like").getAsLong());

                    result.setDuration(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("duration").getAsLong());

                    result.setThumbnail(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("thumbnail").getAsJsonObject().get("player").getAsString());

                    String accessRightKey = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").get("accessRightKey").getAsString();
                    String trackId = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("client").get("watchTrackId").getAsString();

                    StringBuilder videoJson = new StringBuilder();

                    for (JsonElement element : json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("videos")) {
                        //System.out.println(element);
                        if (element.getAsJsonObject().get("isAvailable").getAsBoolean()) {
                            videoJson.append("[\"").append(element.getAsJsonObject().get("id").getAsString()).append("\",\"").append(json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("audios").get(0).getAsJsonObject().get("id").getAsString()).append("\"],");
                        }
                    }

                    String sendJson = "{\"outputs\":["+videoJson.substring(0, videoJson.length() - 1)+"]}";
                    //System.out.println(sendJson);

                    if (Proxy == null){
                        client = HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofSeconds(5))
                                .build();
                    } else {
                        String[] s = Proxy.split(":");
                        client = HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofSeconds(5))
                                .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                                .build();
                    }

                    uri = new URI("https://nvapi.nicovideo.jp/v1/watch/"+id+"/access-rights/hls?actionTrackId="+trackId);
                    request = user_session != null && user_session_secure != null ? HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("Access-Control-Request-Headers", "content-type,x-access-right-key,x-frontend-id,x-frontend-version,x-niconico-language,x-request-with")
                            .headers("X-Access-Right-Key", accessRightKey)
                            .headers("X-Frontend-Id", "6")
                            .headers("X-Frontend-Version", "0")
                            .headers("X-Niconico-Language", "ja-jp")
                            .headers("X-Request-With", "nicovideo")
                            .headers("Cookie", "user_session="+user_session+"; user_session_secure="+user_session_secure+"; nicosid="+nicosid)
                            .headers("User-Agent", Function.UserAgent)
                            .POST(HttpRequest.BodyPublishers.ofString(sendJson))
                            .build() :
                            HttpRequest.newBuilder()
                                    .uri(uri)
                                    .headers("Access-Control-Request-Headers", "content-type,x-access-right-key,x-frontend-id,x-frontend-version,x-niconico-language,x-request-with")
                                    .headers("X-Access-Right-Key", accessRightKey)
                                    .headers("X-Frontend-Id", "6")
                                    .headers("X-Frontend-Version", "0")
                                    .headers("X-Niconico-Language", "ja-jp")
                                    .headers("X-Request-With", "nicovideo")
                                    .headers("Cookie", "nicosid="+nicosid)
                                    .headers("User-Agent", Function.UserAgent)
                                    .POST(HttpRequest.BodyPublishers.ofString(sendJson))
                                    .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (send.statusCode() >= 400){
                        //System.out.println("TEST");
                        uri = null;
                        request = null;
                        client.close();
                        client = null;

                        return gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")"));
                    }
                    body = send.body();
                    //System.out.println(body);
                    json = gson.fromJson(body, JsonElement.class);

                    List<String> cookieText = new ArrayList<>();
                    if (!send.headers().allValues("Set-Cookie").isEmpty()){
                        cookieText = send.headers().allValues("Set-Cookie");
                    }
                    if (!send.headers().allValues("set-cookie").isEmpty()){
                        cookieText = send.headers().allValues("set-cookie");
                    }

                    if (json.isJsonObject() && json.getAsJsonObject().has("data") && json.getAsJsonObject().get("data").getAsJsonObject().has("contentUrl")){
                        result.setVideoURL(json.getAsJsonObject().get("data").getAsJsonObject().get("contentUrl").getAsString());

                        String[] split = cookieText.get(0).split(";");

                        HashMap<String, String> cookie = new HashMap<>();
                        cookie.put("nicosid", nicosid);
                        if (split[0].startsWith("domand_bid=")){
                            cookie.put("domand_bid", split[0].replaceAll("domand_bid=", ""));
                        }
                        result.setVideoAccessCookie(cookie);
                    } else {
                        return gson.toJson(new ErrorMessage("動画取得に失敗しました。"));
                    }

                    return gson.toJson(result);
                } else {
                    // ニコ生
                    NicoNicoVideo liveData = new NicoNicoVideo();

                    if (json.isJsonObject() && json.getAsJsonObject().has("program")){
                        liveData.setURL(json.getAsJsonObject().get("program").getAsJsonObject().get("watchPageUrl").getAsString());
                        liveData.setTitle(json.getAsJsonObject().get("program").getAsJsonObject().get("title").getAsString());
                        liveData.setDescription(json.getAsJsonObject().get("program").getAsJsonObject().get("description").getAsString());
                        JsonArray tags = json.getAsJsonObject().get("program").getAsJsonObject().get("tag").getAsJsonObject().get("list").getAsJsonArray();

                        String[] tagList = new String[tags.size()];
                        int i = 0;
                        for (JsonElement tag : tags) {
                            tagList[i] = tag.getAsJsonObject().get("text").getAsString();
                        }
                        liveData.setTags(tagList);
                        liveData.setViewCount(json.getAsJsonObject().get("program").getAsJsonObject().get("statistics").getAsJsonObject().get("watchCount").getAsLong());
                        liveData.setCommentCount(json.getAsJsonObject().get("program").getAsJsonObject().get("statistics").getAsJsonObject().get("commentCount").getAsLong());
                    }


                    //System.out.println(json);
                    if (json.isJsonObject() && json.getAsJsonObject().has("site") && json.getAsJsonObject().get("site").getAsJsonObject().has("relive")){

                        if (json.getAsJsonObject().get("site").getAsJsonObject().get("relive").getAsJsonObject().has("webSocketUrl")){

                            NicoNicoVideo cacheData = LiveCacheList.get(liveData.getURL());
                            if (cacheData == null){
                                String WebsocketURL = json.getAsJsonObject().get("site").getAsJsonObject().get("relive").getAsJsonObject().get("webSocketUrl").getAsString();

                                if (Proxy == null){
                                    client = HttpClient.newBuilder()
                                            .version(HttpClient.Version.HTTP_2)
                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .build();
                                } else {
                                    String[] s = Proxy.split(":");
                                    client = HttpClient.newBuilder()
                                            .version(HttpClient.Version.HTTP_2)
                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                                            .build();
                                }

                                final String[] resultData = new String[]{"", "", null};
                                final Timer niconamaTimer = new Timer();
                                final HttpClient finalClient = client;
                                final WebSocket.Builder wsb = client.newWebSocketBuilder();
                                final WebSocket.Listener listener = new WebSocket.Listener() {
                                    @Override
                                    public void onOpen(WebSocket webSocket){
                                        // 接続時
                                        webSocket.sendText("{\"type\":\"startWatching\",\"data\":{\"stream\":{\"quality\":\"abr\",\"protocol\":\"hls\",\"latency\":\"low\",\"accessRightMethod\":\"single_cookie\",\"chasePlay\":false},\"room\":{\"protocol\":\"webSocket\",\"commentable\":true},\"reconnect\":false}}", true);
                                        webSocket.request(Integer.MAX_VALUE);
                                    }

                                    @Override
                                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                                        // 切断時
                                        finalClient.close();
                                        niconamaTimer.cancel();
                                        LiveCacheList.remove(liveData.getURL());
                                        return null;
                                    }

                                    @Override
                                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                                        String message = data.toString();
                                        //System.out.println(message);

                                        JsonElement json1 = gson.fromJson(message, JsonElement.class);
                                        //System.out.println("<--- "+json1);

                                        if (json1 != null && json1.isJsonObject() && json1.getAsJsonObject().has("type")){

                                            String type = json1.getAsJsonObject().get("type").getAsString();
                                            if (type.equals("serverTime")){
                                                //System.out.println("---> {\"type\":\"getEventState\",\"data\":{}}");
                                                webSocket.sendText("{\"type\":\"getEventState\",\"data\":{}}", true);
                                            }

                                            if (type.equals("eventState")){
                                                //System.out.println("---> {\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}");
                                                webSocket.sendText("{\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}", true);
                                            }

                                            if (type.equals("ping")){
                                                //System.out.println("---> {\"type\":\"pong\"}");
                                                webSocket.sendText("{\"type\":\"pong\"}", true);
                                            }

                                            if (type.equals("seat")){

                                                niconamaTimer.scheduleAtFixedRate(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        //System.out.println("---> {\"type\":\"keepSeat\"}");
                                                        webSocket.sendText("{\"type\":\"keepSeat\"}", true);
                                                    }
                                                }, 30000L, 30000L);

                                            }

                                            if (type.equals("messageServer")){
                                                //System.out.println("---> {\"type\":\"notifyNewVisit\",\"data\":{}}");
                                                webSocket.sendText("{\"type\":\"notifyNewVisit\",\"data\":{}}", true);
                                                //System.out.println("---> {\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}");
                                                webSocket.sendText("{\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}", true);
                                            }

                                            if (type.equals("stream")){
                                                if (json1.getAsJsonObject().get("data").getAsJsonObject().has("uri")){
                                                    StringBuilder sb = new StringBuilder();
                                                    if (json1.getAsJsonObject().get("data").getAsJsonObject().has("cookies") && !json1.getAsJsonObject().get("data").getAsJsonObject().get("cookies").getAsJsonArray().isEmpty()){
                                                        for (JsonElement jsonElement : json1.getAsJsonObject().get("data").getAsJsonObject().get("cookies").getAsJsonArray()) {
                                                            sb.append(jsonElement.getAsJsonObject().get("name").getAsString()).append("=").append(jsonElement.getAsJsonObject().get("value").getAsString()).append("; ");
                                                        }

                                                        resultData[2] = sb.substring(0, sb.length() - 2);
                                                    }

                                                    resultData[0] = json1.getAsJsonObject().get("data").getAsJsonObject().get("uri").getAsString();
                                                } else {
                                                    resultData[0] = "Error";
                                                    niconamaTimer.cancel();
                                                    finalClient.close();
                                                }
                                            }

                                            if (type.equals("disconnect")){
                                                LiveCacheList.remove(liveData.getURL());
                                                niconamaTimer.cancel();
                                                finalClient.close();
                                            }
                                        }

                                        return null;
                                    }
                                };
                                CompletableFuture<WebSocket> comp = wsb.buildAsync(new URI(WebsocketURL), listener);
                                try {
                                    WebSocket webSocket = comp.get();
                                    webSocket = null;
                                } catch (Exception e) {
                                    return gson.toJson(new ErrorMessage("取得に失敗しました。 ("+e.getMessage()+")"));
                                }

                                while (resultData[1] == null || resultData[1].isEmpty()){
                                    resultData[1] = resultData[0];
                                }

                                if (!resultData[0].equals("Error")){
                                    HashMap<String, String> h = new HashMap<>();
                                    liveData.setLiveURL(resultData[0]);
                                    if (resultData[2] != null){
                                        for (String s : resultData[2].split(";")) {
                                            String[] split = s.split("=");
                                            h.put(split[0], split[1]);
                                        }
                                    } else {
                                        h = null;
                                    }
                                    liveData.setLiveAccessCookie(h);
                                }

                                LiveCacheList.put(liveData.getURL(), liveData);
                            } else {
                                liveData.setLiveURL(cacheData.getLiveURL());
                                liveData.setLiveAccessCookie(cacheData.getLiveAccessCookie());
                            }

                            return gson.toJson(liveData);

                        } else {
                            return gson.toJson(new ErrorMessage("取得に失敗しました。 (Websocket)"));
                        }

                    } else {
                        return gson.toJson(new ErrorMessage("取得に失敗しました。"));
                    }
                    //return gson.toJson(json);
                }
            }

            result = null;
            matcher_normal = null;
            matcher_short = null;
            matcher_cas = null;
            matcher_idOnly = null;
            return "";
        } catch (Exception e){
            e.printStackTrace();
            return gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPアクセスエラー)"));
        }
    }

    @Override
    public String getServiceName() {
        return "ニコニコ";
    }

    @Override
    public String getUseProxy() {
        return Proxy;
    }
}
