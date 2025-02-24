package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.bilibili;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class bilibili_com implements ServiceAPI {

    private String url = null;
    private String Proxy = null;

    private final Pattern Support_URL1 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)/");
    private final Pattern Support_URL2 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{
                "www.bilibili.com"
        };
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
            return "{\"ErrorMessage\": \"URLがありません\"}";
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            Proxy = Function.ProxyList.get(i);
        }

        Matcher matcher1 = Support_URL1.matcher(url);
        Matcher matcher2 = Support_URL2.matcher(url);

        String VideoID = "";
        if (matcher1.find()){
            VideoID = matcher1.group(1);
        } else if (matcher2.find()){
            VideoID = matcher2.group(1);
        } else {
            return "{\"ErrorMessage\": \"サポートされていないURLです。\"}";
        }

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

            URI uri = new URI("https://api.bilibili.com/x/web-interface/view?bvid="+VideoID);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (send.statusCode() >= 400){
                client.close();
                request = null;
                uri = null;
                client = null;
                return "{\"ErrorMessage\": \"取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")\"}";
            }
/*
            client.close();
            request = null;
            uri = null;
            client = null;
            return send.body();
*/
            /*
            *
            * private String URL;
            * private String Title;
            * private String Description;
            * private String Thumbnail;
            * private long ViewCount;
            * private long ReplyCount;
            * private long LikeCount;
            * private long CoinCount;
            * private long FavoriteCount;
            * private long Duration;

            * private String VideoURL;
            * private HashMap<String, String> VideoAccessCookie;
            *
             */

            JsonElement json = Function.gson.fromJson(send.body(), JsonElement.class);

            bilibili result = new bilibili();
            long cid = -1;
            if (json.isJsonObject() && json.getAsJsonObject().has("data")){
                result.setURL("https://www.bilibili.com/video/"+json.getAsJsonObject().get("data").getAsJsonObject().get("bvid").getAsString()+"/");
                result.setTitle(json.getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString());
                result.setDescription(json.getAsJsonObject().get("data").getAsJsonObject().get("desc").getAsString());
                result.setThumbnail(json.getAsJsonObject().get("data").getAsJsonObject().get("pic").getAsString());
                result.setViewCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("view").getAsLong());
                result.setReplyCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("reply").getAsLong());
                result.setLikeCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("like").getAsLong());
                result.setCoinCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("coin").getAsLong());
                result.setFavoriteCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("favorite").getAsLong());
                result.setDuration(json.getAsJsonObject().get("data").getAsJsonObject().get("duration").getAsLong());

                cid = json.getAsJsonObject().get("data").getAsJsonObject().get("cid").getAsLong();
            }

            if (json.getAsJsonObject().has("code")){
                if (json.getAsJsonObject().get("code").getAsLong() == -400) {
                    return "{\"ErrorMessage\": \"動画が存在しません。\"}";
                }
            }

            if (cid == -1) {
                return "{\"ErrorMessage\": \"動画が存在しません。\"}";
            }

            //System.out.println("cid : " + cid);

            uri = new URI("https://api.bilibili.com/x/player/playurl?bvid="+VideoID+"&cid="+cid);
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            return send.body();

            //return Function.gson.toJson(result);
        } catch (Exception e){
            e.printStackTrace();
            return "{\"ErrorMessage\": \"内部エラーです。 ("+e.getMessage()+")\"}";
        }

    }

    @Override
    public String getServiceName() {
        return "bilibili.com";
    }

    @Override
    public String getUseProxy() {
        return "";
    }
}
