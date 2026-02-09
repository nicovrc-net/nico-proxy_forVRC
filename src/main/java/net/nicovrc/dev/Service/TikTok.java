package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.TikTokResult;
import net.nicovrc.dev.Service.Result.TwitterResult;

import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikTok implements ServiceAPI {

    private String url = null;
    private String Proxy = null;
    private HttpClient client = null;

    private final Gson gson = Function.gson;

    private final Pattern matcher_DataJson = Pattern.compile("<script id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\" type=\"application/json\">\\{(.+)\\}</script>");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.tiktok.com", "*.tiktok.com"};
    }

    @Override
    public void Set(String json, HttpClient client) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.url = jsonElement.getAsJsonObject().get("URL").getAsString();
        }
        this.client = client;
    }

    @Override
    public String Get() {

        if (url == null || url.isEmpty()){
            return Function.gson.toJson(new ErrorMessage("URLがありません"));
        }

        try {

            //System.out.println(Proxy);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .GET()
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            String text = "{}";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                text = new String(bytes, StandardCharsets.UTF_8);
            } else {
                text = new String(send.body(), StandardCharsets.UTF_8);
            }
            if (send.statusCode() >= 400){
                request = null;
                return Function.gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")"));
            }

            HashMap<String, String> cookieList = new HashMap<>();

            //send.headers().
            List<String> list = send.headers().allValues("Set-Cookie");

            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                //System.out.println(s);

                String s1 = s.split(";")[0];
                //System.out.println(s1);

                sb.append(s1).append("; ");
            }
            //System.out.println(sb.substring(0, sb.length() - 2));

            Matcher matcher = matcher_DataJson.matcher(text);
            String jsonText = "{}";
            if (matcher.find()){
                jsonText = "{" + matcher.group(1) + "}";
            }
            JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);

            if (json.isJsonObject() && json.getAsJsonObject().has("__DEFAULT_SCOPE__") && json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().has("webapp.video-detail") && json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().has("itemInfo")){

                TikTokResult result = new TikTokResult();
                result.setURL("https://" + json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.app-context").getAsJsonObject().get("host").getAsString() + "/@" + json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("author").getAsJsonObject().get("uniqueId").getAsString() + "/video/" + json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("id").getAsString());
                result.setDescription(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("desc").getAsString());
                result.setDiggCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("diggCount").getAsString()));
                result.setCommentCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("commentCount").getAsString()));
                result.setCollectCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("collectCount").getAsString()));
                result.setShareCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("shareCount").getAsString()));
                result.setPlayCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("playCount").getAsString()));
                result.setDuration(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("video").getAsJsonObject().get("duration").getAsLong());
                result.setVideoURL(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("video").getAsJsonObject().get("downloadAddr").getAsString());
                if (result.getVideoURL().isEmpty()){
                    result.setVideoURL(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("video").getAsJsonObject().get("playAddr").getAsString());
                }
                result.setVideoAccessCookie(sb.substring(0, sb.length() - 2));
                //return json.toString();
                return gson.toJson(result);
            } else {
                return Function.gson.toJson(new ErrorMessage("存在しない動画です。"));
            }

        } catch (Exception e){
            e.printStackTrace();
            return Function.gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }

        //return "";

    }

    @Override
    public String getServiceName() {
        return "TikTok";
    }

    @Override
    public String getUseProxy() {
        return Proxy;
    }
}
