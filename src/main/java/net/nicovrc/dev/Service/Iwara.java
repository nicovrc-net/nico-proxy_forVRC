package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.IwaraResult;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;

public class Iwara implements ServiceAPI {

    private String url = null;
    private String proxy = null;

    private final Gson gson = Function.gson;

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.iwara.tv"};
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
            return gson.toJson(new ErrorMessage("URLがありません"));
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            proxy = Function.ProxyList.get(i);
        }

        String[] split = url.split("/");

        if (split.length < 5){
            return gson.toJson(new ErrorMessage("対応していないURLです。"));
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
                    .uri(new URI("https://api.iwara.tv/video/" + split[4]))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .GET()
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            String jsonText = "{}";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                jsonText = new String(bytes, StandardCharsets.UTF_8);
            } else {
                jsonText = new String(send.body(), StandardCharsets.UTF_8);
            }
            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
/*
    private String Title;
    private String Description;
    private Long LikeCount;
    private Long ViewCount;

    private String VideoURL;
 */
            IwaraResult result = new IwaraResult();
            result.setTitle(json.getAsJsonObject().get("title").getAsString());
            result.setDescription(json.getAsJsonObject().get("body").getAsString());
            result.setLikeCount(json.getAsJsonObject().get("numLikes").getAsLong());
            result.setViewCount(json.getAsJsonObject().get("numViews").getAsLong());

            String baseUrl = json.getAsJsonObject().get("fileUrl").getAsString();

            //System.out.println(baseUrl + "&download="+URLEncoder.encode("Iwara - "+result.getTitle()+" ["+json.getAsJsonObject().get("id").getAsString()+"].mp4", StandardCharsets.UTF_8));
            request = HttpRequest.newBuilder()
                    .uri(new URI(baseUrl + "&download="+URLEncoder.encode("Iwara - "+result.getTitle()+" ["+json.getAsJsonObject().get("id").getAsString()+"].mp4", StandardCharsets.UTF_8)))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    // いつかこのX-Versionを取れるようにする
                    // .headers("X-Version","3f8ce8c9518993ed46b9f388988b4ad0781eff7d")
                    .GET()
                    .build();
            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            jsonText = "{}";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                jsonText = new String(bytes, StandardCharsets.UTF_8);
            } else {
                jsonText = new String(send.body(), StandardCharsets.UTF_8);
            }
            json = new Gson().fromJson(jsonText, JsonElement.class);

            result.setVideoURL("https:"+json.getAsJsonArray().get(0).getAsJsonObject().get("src").getAsJsonObject().get("view").getAsString());

            return gson.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorMessage("取得に失敗しました。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "iwara.tv";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
