package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.OPENREC_Result;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;

public class OPENREC implements ServiceAPI {

    private String url = null;
    private String Proxy = null;

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.openrec.tv"};
    }

    @Override
    public void Set(String json) {
        JsonElement element = Function.gson.fromJson(json, JsonElement.class);
        if (element.isJsonObject() && element.getAsJsonObject().has("URL")){
            url = element.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {
        try {
            // Proxy
            if (!Function.ProxyList.isEmpty()){
                int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
                Proxy = Function.ProxyList.get(i);
            }

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

            String[] split = url.split("/");
            String id = split[split.length - 1];

            URI uri = new URI("https://public.openrec.tv/external/api/v5/movies/"+id);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
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
            JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);
            jsonText = null;
            /*
    private String URL;
    private String Title;
    private String Introduction;
    private String Thumbnail;
    private Long LiveViews;
    private Long TotalViews;

    private boolean isLive;

    private String VideoURL;
    private String LiveURL;
             */
            OPENREC_Result result = new OPENREC_Result();
            if (json.getAsJsonObject().has("id")){
                if (json.getAsJsonObject().get("is_live").getAsBoolean()){
                    result.setURL("https://www.openrec.tv/live/"+id);
                } else {
                    result.setURL("https://www.openrec.tv/movie/"+id);
                }
                result.setTitle(json.getAsJsonObject().get("title").getAsString());
                result.setIntroduction(json.getAsJsonObject().get("introduction").getAsString());
                result.setThumbnail(json.getAsJsonObject().get("l_thumbnail_url").getAsString());
                if (json.getAsJsonObject().get("is_live").getAsBoolean()){
                    result.setLiveViews(json.getAsJsonObject().get("live_views").getAsLong());
                    result.setTotalViews(json.getAsJsonObject().get("total_views").getAsLong());
                }
                result.setLive(json.getAsJsonObject().get("is_live").getAsBoolean());
                if (result.isLive()){
                    result.setLiveURL(json.getAsJsonObject().get("media").getAsJsonObject().get("url").getAsString());
                } else {
                    result.setVideoURL(json.getAsJsonObject().get("media").getAsJsonObject().get("url").getAsString());
                }
            } else {
                client.close();
                return Function.gson.toJson(new ErrorMessage("存在しない 配信 または 動画 です"));
            }

            client.close();
            return Function.gson.toJson(result);
        } catch (Exception e){
            return Function.gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "OPENREC";
    }

    @Override
    public String getUseProxy() {
        return Proxy;
    }
}
