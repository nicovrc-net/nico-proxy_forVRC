package net.nicovrc.dev.Service;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.Twitcas;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Twitcasting implements ServiceAPI {

    private String url = null;
    private String ClientID = null;
    private String ClientSecret = null;
    private String Proxy = null;

    private final Gson gson = Function.gson;

    private final Pattern matcher_videoUrl = Pattern.compile("playsinline\n {16}src=\"(.+)\"");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"twitcasting.tv"};
    }

    @Override
    public void Set(String json) {

        JsonElement element = gson.fromJson(json, JsonElement.class);

        //System.out.println(element);

        if (element.isJsonObject() && element.getAsJsonObject().has("URL")){
            this.url = element.getAsJsonObject().get("URL").getAsString();
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("ClientID")){
            this.ClientID = element.getAsJsonObject().get("ClientID").getAsString();
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("ClientSecret")){
            this.ClientSecret = element.getAsJsonObject().get("ClientSecret").getAsString();
        }

    }

    @Override
    public String Get() {
        if (url == null || url.isEmpty()){
            return "{\"ErrorMessage\": \"URLがありません\"}";
        }

        if (ClientID == null || ClientID.isEmpty() || ClientSecret == null || ClientSecret.isEmpty()){
            return "{\"ErrorMessage\": \"ツイキャス APIキーがありません\"}";
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            Proxy = Function.ProxyList.get(i);
        }

        String base64 = new String(Base64.getEncoder().encode((ClientID+":"+ClientSecret).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        String[] split = url.split("/");
        String userId = "";
        String videoId = "";
        int i = 0;
        for (String str : split){
            if (str.equals("twitcasting.tv")){
                userId = split[i + 1];
                i++;
                continue;
            }
            if (str.equals("movie")){
                videoId = split[i + 1];
                i++;
                continue;
            }
            i++;
        }

        //
        try (HttpClient client = Proxy == null ? HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build() :
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .proxy(ProxySelector.of(new InetSocketAddress(Proxy.split(":")[0], Integer.parseInt(Proxy.split(":")[1]))))
                        .build()){

            //System.out.println(userId);
            URI uri = videoId.isEmpty() ? new URI("https://apiv2.twitcasting.tv/users/"+userId+"/current_live") : new URI("https://apiv2.twitcasting.tv/movies/"+videoId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .headers("X-Api-Version", "2.0")
                    .headers("Authorization", "Basic "+base64)
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            //System.out.println(send.statusCode());

            JsonElement json = gson.fromJson(send.body(), JsonElement.class);

            if (json.isJsonObject() && json.getAsJsonObject().has("movie")){
                Twitcas result = new Twitcas();
                result.setURL(json.getAsJsonObject().get("movie").getAsJsonObject().get("link").getAsString());
                result.setTitle(json.getAsJsonObject().get("movie").getAsJsonObject().get("title").getAsString());
                result.setSubTitle(json.getAsJsonObject().get("movie").getAsJsonObject().get("subtitle").isJsonNull() ? null : json.getAsJsonObject().get("movie").getAsJsonObject().get("subtitle").getAsString());
                result.setThumbnail(json.getAsJsonObject().get("movie").getAsJsonObject().get("large_thumbnail").getAsString());
                result.setDuration(json.getAsJsonObject().get("movie").getAsJsonObject().get("duration").getAsLong());
                if (!json.getAsJsonObject().get("movie").getAsJsonObject().get("is_live").getAsBoolean()){
                    result.setMax_viewCount(json.getAsJsonObject().get("movie").getAsJsonObject().get("max_view_count").getAsLong());
                } else {
                    result.setCurrent_viewCount(json.getAsJsonObject().get("movie").getAsJsonObject().get("current_view_count").getAsLong());
                }
                result.setTotal_viewCount(json.getAsJsonObject().get("movie").getAsJsonObject().get("total_view_count").getAsLong());

                if (json.getAsJsonObject().get("movie").getAsJsonObject().get("hls_url").isJsonNull()){
                    uri = new URI(result.getURL());
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("X-Api-Version", "2.0")
                            .headers("Authorization", "Basic "+base64)
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    Matcher matcher = matcher_videoUrl.matcher(send.body());
                    if (matcher.find()){
                        result.setVideoURL(matcher.group(1));
                    }
                } else {
                    result.setLiveURL(json.getAsJsonObject().get("movie").getAsJsonObject().get("hls_url").getAsString());
                }

                return gson.toJson(result);
            } else {
                return "{\"ErrorMessage\": \"取得に失敗しました。 ("+json.getAsJsonObject().get("error").getAsJsonObject().get("message").getAsString()+")\"";
            }

        } catch (Exception e){
            e.printStackTrace();
            return "{\"ErrorMessage\": \"取得に失敗しました。 ("+e.getMessage()+")\"";
        }
    }

    @Override
    public String getServiceName() {
        return "ツイキャス";
    }

    @Override
    public String getUseProxy() {
        return Proxy;
    }
}
