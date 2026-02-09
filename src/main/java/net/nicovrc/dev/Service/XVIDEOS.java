package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.XvideoResult;

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

public class XVIDEOS implements ServiceAPI {

    private String url = null;
    private String proxy = null;
    private HttpClient client = null;

    private final Gson gson = Function.gson;

    private final Pattern matcher_duration = Pattern.compile("<meta property=\"og:duration\" content=\"(\\d+)\" />");
    private final Pattern matcher_hlsURL = Pattern.compile("html5player\\.setVideoHLS\\('(.+)'\\)");
    private final Pattern matcher_Title = Pattern.compile("html5player\\.setVideoTitle\\('(.+)'\\)");
    private final Pattern matcher_ThumbUrl = Pattern.compile("html5player\\.setThumbUrl\\('(.+)'\\);");
    private final Pattern matcher_Description = Pattern.compile("\"description\": \"(.+)\",");
    private final Pattern matcher_playCount = Pattern.compile("\"userInteractionCount\": (\\d+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"xvideos.com", "www.xvideos.com"};
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

        if (url  == null || url.isEmpty()){
            return Function.gson.toJson(new ErrorMessage("URLが入力されていません。"));
        }

        try {

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

            XvideoResult result = new XvideoResult();

            Matcher matcher1 = matcher_Title.matcher(text);
            Matcher matcher2 = matcher_Description.matcher(text);
            Matcher matcher3 = matcher_ThumbUrl.matcher(text);
            Matcher matcher4 = matcher_playCount.matcher(text);
            Matcher matcher5 = matcher_duration.matcher(text);
            Matcher matcher6 = matcher_hlsURL.matcher(text);

            if (matcher1.find()){
                result.setTitle(matcher1.group(1));
            }
            if (matcher2.find()){
                result.setDescription(matcher2.group(1));
            }
            if (matcher3.find()){
                result.setThumbUrl(matcher3.group(1));
            }
            if (matcher4.find()){
                result.setPlayCount(Long.parseLong(matcher4.group(1)));
            }
            if (matcher5.find()){
                result.setDuration(Long.parseLong(matcher5.group(1)));
            }
            if (matcher6.find()){
                result.setVideoURL(matcher6.group(1));
            } else {
                return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
            }

            return gson.toJson(result);

        } catch (Exception e){
            e.printStackTrace();
            return Function.gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "XVIDEOS.COM";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
