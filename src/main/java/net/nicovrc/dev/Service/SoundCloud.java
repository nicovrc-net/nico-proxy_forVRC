package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.SoundCloudResult;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundCloud implements ServiceAPI {

    private String url = null;
    private String proxy = null;

    private final Pattern clientId = Pattern.compile("client_id:\"(.+)\",env:\"production\"");
    private final Pattern jsonData = Pattern.compile("window\\.__sc_hydration = \\[(.+)\\];");
    private final Pattern CheckQuestion = Pattern.compile("\\?");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"soundcloud.com"};
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
        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            proxy = Function.ProxyList.get(i);
        }

        if (url == null || url.isEmpty()){
            return "{\"ErrorMessage\": \"URLが入力されていません。\"}";
        }

        HttpClient client;
        if (proxy == null){
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        } else {
            String[] s = proxy.split(":");
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                    .build();
        }

        try {

            // https://soundcloud.com/kysn/5-hyperflip-67?access=ex_chrome
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Matcher matcher1 = jsonData.matcher(send.body());

            JsonElement json = null;
            if (matcher1.find()){
                try {
                    json = Function.gson.fromJson("["+matcher1.group(1)+"]", JsonElement.class);
                } catch (Exception e){
                    return "{\"ErrorMessage\": \"対応していないURLです。\"}";
                }
            }

            if (json == null){
                return "{\"ErrorMessage\": \"対応していないURLです。\"}";
            }

            request = HttpRequest.newBuilder()
                    .uri(new URI("https://a-v2.sndcdn.com/assets/50-a0fa7b81.js"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            final String ClientId;
            Matcher matcher2 = clientId.matcher(send.body());
            if (matcher2.find()){
                ClientId = matcher2.group(1);
            } else {
                ClientId = null;
            }

            String TrackAuthorization = null;
            String BaseURL = null;

            String permalink_url = null;
            String title = null;
            Long duration = null;
            String description = null;

            for (int i = 0; i < json.getAsJsonArray().size(); i++) {
                if (json.getAsJsonArray().get(i).getAsJsonObject().get("hydratable").getAsString().equals("sound")){
                    if (BaseURL == null){
                        BaseURL = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    }

                    if (TrackAuthorization == null){
                        TrackAuthorization = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("track_authorization").getAsString();
                    }

                    if (permalink_url == null){
                        permalink_url = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("permalink_url").getAsString();
                    }
                    if (title == null){
                        title = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString();
                    }
                    if (duration == null){
                        duration = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("full_duration").getAsLong();
                    }
                    if (description == null){
                        description = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("description").getAsString();
                    }
                }

            }

            SoundCloudResult result = new SoundCloudResult();
            result.setURL(permalink_url);
            result.setTitle(title);
            result.setDescription(description);
            result.setDuration(duration);

            String hlsUrl = BaseURL + "?client_id=" + ClientId + "&track_authorization=" + TrackAuthorization;

            request = HttpRequest.newBuilder()
                    .uri(new URI(hlsUrl))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            json = Function.gson.fromJson(send.body(), JsonElement.class);

            if (json != null){
                result.setAudioURL(json.getAsJsonObject().get("url").getAsString());
            } else {
                String ClientID = "3WIthHrmko3NUQ6wbfCSRvFcDexHgswc";
                request = HttpRequest.newBuilder()
                        .uri(new URI("https://api-v2.soundcloud.com/resolve?url="+ URLEncoder.encode(url, StandardCharsets.UTF_8)+"&client_id="+ClientID))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);

                if (json == null){
                    ClientID = "YHtBnq6bxM7DhJkIfzrGq3gYrueyLDMM";
                    request = HttpRequest.newBuilder()
                            .uri(new URI("https://api-v2.soundcloud.com/resolve?url="+ URLEncoder.encode(url, StandardCharsets.UTF_8)+"&client_id="+ClientID))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    json = Function.gson.fromJson(send.body(), JsonElement.class);

                    hlsUrl = json.getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    //System.out.println(hlsUrl);
                    request = CheckQuestion.matcher(hlsUrl).find() ? HttpRequest.newBuilder()
                            .uri(new URI(hlsUrl + "&client_id="+ClientID))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .GET()
                            .build() : HttpRequest.newBuilder()
                            .uri(new URI(hlsUrl + "?client_id="+ClientID))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .GET()
                            .build();
                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    json = Function.gson.fromJson(send.body(), JsonElement.class);

                    result.setAudioURL(json.getAsJsonObject().get("url").getAsString());
                } else {
                    result.setAudioURL(json.getAsJsonObject().get("url").getAsString());
                }
            }

            return Function.gson.toJson(result);
        } catch (Exception e){
            e.printStackTrace();
            return "{\"ErrorMessage\": \"内部エラーです。 ("+e.getMessage().replaceAll("\"","\\\\\"")+"\"}";
        }

    }

    @Override
    public String getServiceName() {
        return "SoundCloud";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
