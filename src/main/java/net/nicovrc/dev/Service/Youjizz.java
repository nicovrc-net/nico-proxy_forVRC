package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.YoujizzResult;

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

public class Youjizz implements ServiceAPI {

    private String url = null;
    private String proxy = null;

    private final Gson gson = Function.gson;
    private final Pattern matcher_Json = Pattern.compile("var dataEncodings = \\[(.+)\\];");
    private final Pattern matcher_hls = Pattern.compile("_hls");
    private final Pattern matcher_title = Pattern.compile("<title>(.+)</title>");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.youjizz.com"};
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

            Matcher matcher = matcher_Json.matcher(text);
            if (!matcher.find()){
                return gson.toJson(new ErrorMessage("取得に失敗しました。"));
            }
            JsonElement json = new Gson().fromJson("["+matcher.group(1)+"]", JsonElement.class);

            int quality = -1;
            int quality2 = -1;
            String url = "";
            String url2 = "";
            for (JsonElement element : json.getAsJsonArray()) {
                if (matcher_hls.matcher(element.getAsJsonObject().get("filename").getAsString()).find()){
                    if (quality <= Integer.parseInt(element.getAsJsonObject().get("quality").getAsString().equals("Auto") ? "0" : element.getAsJsonObject().get("quality").getAsString())){
                        quality = Integer.parseInt(element.getAsJsonObject().get("quality").getAsString().equals("Auto") ? "0" : element.getAsJsonObject().get("quality").getAsString());
                        url = "https:"+element.getAsJsonObject().get("filename").getAsString();
                    }
                } else {
                    if (quality2 <= Integer.parseInt(element.getAsJsonObject().get("quality").getAsString().equals("Auto") ? "0" : element.getAsJsonObject().get("quality").getAsString())){
                        quality2 = Integer.parseInt(element.getAsJsonObject().get("quality").getAsString().equals("Auto") ? "0" : element.getAsJsonObject().get("quality").getAsString());
                        url2 = "https:"+element.getAsJsonObject().get("filename").getAsString();
                    }
                }
            }

            YoujizzResult result = new YoujizzResult();

            Matcher matcher1 = matcher_title.matcher(text);
            if (matcher1.find()){
                result.setTitle(matcher1.group(1));
            }
            if (!url.isEmpty()){
                result.setVideoURL(url);
            } else {
                result.setVideoURL(url2);
            }

            return gson.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();

            return gson.toJson(new ErrorMessage("取得に失敗しました。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "Youjizz";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
