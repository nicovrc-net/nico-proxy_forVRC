package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
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
    private HttpClient client = null;

    private final Gson gson = Function.gson;
    private final Pattern Support_URL = Pattern.compile("https://(.+)\\.pornhub\\.com/view_video\\.php\\?viewkey=(.+)");
    private final Pattern matcher_json = Pattern.compile("var flashvars_(\\d+) = \\{(.*)\\}\\;");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"*.pornhub.com"};
    }

    @Override
    public void Set(String json, HttpClient client) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.url = jsonElement.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {
        if (url  == null || url.isEmpty()){
            return gson.toJson(new ErrorMessage("URLが入力されていません。"));
        }
        Matcher matcher = Support_URL.matcher(url);

        if (!matcher.find()){
            return gson.toJson(new ErrorMessage("対応していないURLです。"));
        }

        String id = matcher.group(2);

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://jp.pornhub.com/view_video.php?viewkey="+id))
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
            Matcher matcher1 = matcher_json.matcher(text);
            if (!matcher1.find()){
                return gson.toJson(new ErrorMessage("取得に失敗しました。"));
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
            return gson.toJson(new ErrorMessage("取得に失敗しました。 ("+e.getMessage()+")"));
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
