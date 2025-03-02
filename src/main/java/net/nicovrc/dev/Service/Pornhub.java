package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.PornhubResult;

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

public class Pornhub implements ServiceAPI {

    private String url = null;
    private String proxy = null;

    private final Gson gson = Function.gson;
    private final Pattern Support_URL = Pattern.compile("https://(.+)\\.pornhub\\.com/view_video\\.php\\?viewkey=(.+)");
    private final Pattern matcher_json = Pattern.compile("var flashvars_(\\d+) = \\{(.*)\\}\\;");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"*.pornhub.com"};
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
        if (url  == null || url.isEmpty()){
            return "{\"ErrorMessage\": \"URLがありません\"}";
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            proxy = Function.ProxyList.get(i);
        }

        Matcher matcher = Support_URL.matcher(url);

        if (!matcher.find()){
            return "{\"ErrorMessage\": \"対応していないURLです。\"}";
        }

        String id = matcher.group(2);

        try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build() :
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                        .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://jp.pornhub.com/view_video.php?viewkey="+id))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Matcher matcher1 = matcher_json.matcher(send.body());
            if (!matcher1.find()){
                return "{\"ErrorMessage\": \"取得に失敗しました。\"}";
            }
            String s = "{" + matcher1.group(2) + "}";
            JsonElement json = gson.fromJson(s, JsonElement.class);

            PornhubResult result = new PornhubResult();

            result.setTitle(json.getAsJsonObject().get("video_title").getAsString());
            result.setThumbnail(json.getAsJsonObject().get("image_url").getAsString());
            result.setDuration(json.getAsJsonObject().get("video_duration").getAsLong());

            String hlsURL = "";
            int width = -1;
            int height = -1;
            for (JsonElement element : json.getAsJsonObject().get("mediaDefinitions").getAsJsonArray()) {

                if (element.getAsJsonObject().get("format").getAsString().equals("hls")){
                    int height1 = element.getAsJsonObject().get("height").getAsInt();
                    int width1 = element.getAsJsonObject().get("width").getAsInt();

                    if (width1 >= width || height1 >= height){
                        width = width1;
                        height = height1;

                        hlsURL = element.getAsJsonObject().get("videoUrl").getAsString();
                    }
                }
            }

            result.setVideoURL(hlsURL);

            return gson.toJson(result);

        } catch (Exception e){
            e.printStackTrace();
            return "{\"ErrorMessage\": \"取得に失敗しました。 ("+e.getMessage().replaceAll("\"","\\\\\"")+")\"";
        }
    }

    @Override
    public String getServiceName() {
        return "Pornhub";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
