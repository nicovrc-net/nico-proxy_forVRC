package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.AbemaResult;
import net.nicovrc.dev.Service.Result.ErrorMessage;

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

public class Abema implements ServiceAPI {

    private String url = null;
    private String Proxy = null;

    private final Gson gson = Function.gson;

    private final Pattern SupportURL_Video1 = Pattern.compile("https://abema\\.tv/video/episode/(.+)");
    private final Pattern SupportURL_Video2 = Pattern.compile("https://abema\\.tv/channels/(.+)/slots/(.+)");
    private final Pattern SupportURL_Live1 = Pattern.compile("https://abema\\.tv/now-on-air/(.+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"abema.tv", "abema.app", "abema.go.link"};
    }

    @Override
    public void Set(String json) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.url = jsonElement.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {

        if (url == null || url.isEmpty()){
            return gson.toJson(new ErrorMessage("URLがありません"));
        }

        if (url.startsWith("https://abema.app") || url.startsWith("https://abema.go.link")){
            try (HttpClient client = Proxy == null ? HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()
                    :
                    HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .connectTimeout(Duration.ofSeconds(5))
                            .proxy(ProxySelector.of(new InetSocketAddress(Proxy.split(":")[0], Integer.parseInt(Proxy.split(":")[1]))))
                            .build()
            ) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .headers("User-Agent", Function.UserAgent)
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                url = send.uri().toURL().toString();

            } catch (Exception e){
                return gson.toJson(new ErrorMessage("取得に失敗しました。"));
            }
        }


        Matcher matcher = SupportURL_Video1.matcher(url);
        Matcher matcher1 = SupportURL_Video2.matcher(url);
        Matcher matcher2 = SupportURL_Live1.matcher(url);

        boolean video = matcher.find();
        boolean archive = matcher1.find();
        boolean live = matcher2.find();

        if (!video && !archive && !live){
            //System.out.println(url);
            return gson.toJson(new ErrorMessage("対応していないURLです"));
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            Proxy = Function.ProxyList.get(i);
        }

        try (HttpClient client = Proxy == null ? HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build()
                :
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .proxy(ProxySelector.of(new InetSocketAddress(Proxy.split(":")[0], Integer.parseInt(Proxy.split(":")[1]))))
                        .build()
        ) {

            if (video){
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://api.p-c3-e.abema-tv.com/v1/video/programs/"+matcher.group(1)+"?division=0&include=tvod"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                        .headers("Referer", "https://abema.tv/")
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                //System.out.println(send.body());

                JsonElement json;
                try {
                    json = gson.fromJson(send.body(), JsonElement.class);
                } catch (Exception e){
                    return gson.toJson(new ErrorMessage("対応していないURLです"));
                }

                if (!json.getAsJsonObject().has("playback")){
                    return gson.toJson(new ErrorMessage("対応していない動画です"));
                }

                /*
    private String URL;
    private String Title;
    private String Content;
    private String Thumbnail;

    private String VideoURL;
    private String LiveURL;
                 */

                AbemaResult result = new AbemaResult();
                result.setURL(url);
                StringBuilder sb = new StringBuilder();
                if (json.getAsJsonObject().has("series") && json.getAsJsonObject().get("series").getAsJsonObject().has("title")){
                    sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("title").getAsString());
                }
                if (json.getAsJsonObject().has("episode") && json.getAsJsonObject().get("episode").getAsJsonObject().has("title")){
                    sb.append(" ").append(json.getAsJsonObject().get("episode").getAsJsonObject().get("title").getAsString());
                }
                result.setTitle(sb.toString());
                sb.setLength(0);
                if (json.getAsJsonObject().has("episode") && json.getAsJsonObject().get("episode").getAsJsonObject().has("content")){
                    result.setContent(json.getAsJsonObject().get("episode").getAsJsonObject().get("content").getAsString());
                }
                if (json.getAsJsonObject().has("series") && json.getAsJsonObject().get("series").getAsJsonObject().has("thumbComponent")){
                    sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("thumbComponent").getAsJsonObject().get("urlPrefix").getAsString()).append("/");
                    sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("thumbComponent").getAsJsonObject().get("filename").getAsString()).append("?");
                    sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("thumbComponent").getAsJsonObject().get("query").getAsString());
                }
                if (!sb.isEmpty()){
                    result.setThumbnail(sb.toString());
                }
                sb.setLength(0);
                sb = null;

                if (json.getAsJsonObject().getAsJsonObject("playback").has("hlsPreview")){
                    result.setVideoURL(json.getAsJsonObject().getAsJsonObject("playback").get("hlsPreview").getAsString());
                }

                return gson.toJson(result);

            }

            if (archive){
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://api.p-c3-e.abema-tv.com/v1/media/slots/"+matcher1.group(2)+"?include=payperview"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                        .headers("Referer", "https://abema.tv/")
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                JsonElement json;
                try {
                    json = gson.fromJson(send.body(), JsonElement.class);
                } catch (Exception e){
                    return gson.toJson(new ErrorMessage("対応していないURLです"));
                }

                if (!json.getAsJsonObject().has("slot")){
                    return gson.toJson(new ErrorMessage("対応していない配信アーカイブです"));
                }

                AbemaResult result = new AbemaResult();
                result.setURL(url);
                if (json.getAsJsonObject().has("slot") && json.getAsJsonObject().get("slot").getAsJsonObject().has("title")){
                    result.setTitle(json.getAsJsonObject().get("slot").getAsJsonObject().get("title").getAsString());
                }
                if (json.getAsJsonObject().has("slot") && json.getAsJsonObject().get("slot").getAsJsonObject().has("content")){
                    result.setContent(json.getAsJsonObject().get("slot").getAsJsonObject().get("content").getAsString());
                }

                if (json.getAsJsonObject().get("slot").getAsJsonObject().has("playback") && json.getAsJsonObject().get("slot").getAsJsonObject().get("playback").getAsJsonObject().has("hlsPreview")){
                    result.setVideoURL(json.getAsJsonObject().get("slot").getAsJsonObject().get("playback").getAsJsonObject().get("hlsPreview").getAsString());
                }
                return gson.toJson(result);
            }

            if (live){
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://api.abema.io/v1/channels"))
                        .headers("User-Agent", Function.UserAgent)
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                JsonElement json;
                try {
                    json = gson.fromJson(send.body(), JsonElement.class);
                } catch (Exception e){
                    return gson.toJson(new ErrorMessage("対応していないURLです"));
                }

                if (json.getAsJsonObject().has("channels")){

                    AbemaResult result = new AbemaResult();

                    String id = matcher2.group(1);
                    for (JsonElement element : json.getAsJsonObject().get("channels").getAsJsonArray()) {
                        if (element.getAsJsonObject().get("id").getAsString().equals(id)){
                            result.setURL("https://abema.tv/now-on-air/"+id);
                            result.setTitle(element.getAsJsonObject().get("name").getAsString());
                            result.setLiveURL(element.getAsJsonObject().get("playback").getAsJsonObject().get("hlsPreview").getAsString());
                            return gson.toJson(result);
                        }
                    }

                }

                return gson.toJson(new ErrorMessage("取得に失敗しました。 (存在しないチャンネル)"));
            }

            return "{}";

        } catch (Exception e){
            e.printStackTrace();
            return gson.toJson(new ErrorMessage("取得に失敗しました。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "Abema";
    }

    @Override
    public String getUseProxy() {
        return Proxy;
    }
}
