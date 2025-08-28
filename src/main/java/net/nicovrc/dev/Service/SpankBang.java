package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.SpankBangResult;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class SpankBang implements ServiceAPI {

    private String url = null;
    private String proxy = null;

    private final Gson gson = Function.gson;
    private final Pattern matcher_json = Pattern.compile("var stream_data = \\{(.+)};");
    private final Pattern matcher_Title = Pattern.compile("<h1 class=\"main_content_title\" title=\"(.+)\">(.+)</h1>");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"*.spankbang.com"};
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

        CookieManager manager = new CookieManager();
        try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(SSLContext.getDefault())
                .cookieHandler(manager)
                .connectTimeout(Duration.ofSeconds(5))
                .build() :
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .cookieHandler(manager)
                        .sslContext(SSLContext.getDefault())
                        .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                        .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .headers("DNT", "1")
                    .headers("Priority","u=0, i")
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

            Matcher matcher = matcher_json.matcher(text);
            if (!matcher.find()){
                //System.out.println(s);
                return gson.toJson(new ErrorMessage("取得に失敗しました。"));
            }
            JsonElement json = gson.fromJson("{" + matcher.group(1) + "}", JsonElement.class);

            SpankBangResult result = new SpankBangResult();
            Matcher matcher1 = matcher_Title.matcher(text);
            if (matcher1.find()){
                result.setTitle(matcher1.group(1));
            }

            String videoUrl = "";
            if (!json.getAsJsonObject().get("m3u8_4k").getAsJsonArray().isEmpty()){
                videoUrl = json.getAsJsonObject().get("m3u8_4k").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("m3u8_1080p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("m3u8_1080p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("m3u8_720p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("m3u8_720p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("m3u8_480p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("m3u8_480p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("m3u8_320p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("m3u8_320p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("m3u8_240p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("m3u8_240p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("4k").getAsJsonArray().isEmpty()){
                videoUrl = json.getAsJsonObject().get("4k").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("1080p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("1080p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("720p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("720p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("480p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("480p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("320p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("320p").getAsJsonArray().get(0).getAsString();
            }
            if (!json.getAsJsonObject().get("240p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
                videoUrl = json.getAsJsonObject().get("240p").getAsJsonArray().get(0).getAsString();
            }

            result.setVideoURL(videoUrl);

            return Function.gson.toJson(result);

        } catch (Exception e){
            e.printStackTrace();

            return gson.toJson(new ErrorMessage("取得に失敗しました。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "SpankBang";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
