package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.SonicbowlResult;

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

public class Sonicbowl implements ServiceAPI {

    private String proxy = null;
    private String url = null;

    private final Pattern matcher_Audio = Pattern.compile("<meta property=\"og:audio\" content=\"(.+)\">");
    private final Pattern matcher_Title = Pattern.compile("<title>(.+)</title>");
    private final Pattern matcher_Description = Pattern.compile("<meta name=\"description\" content=\"(.+)\">");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"player.sonicbowl.cloud"};
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
            return Function.gson.toJson(new ErrorMessage("URLが入力されていません。"));
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            SonicbowlResult result = new SonicbowlResult();

            Matcher matcher1 = matcher_Title.matcher(send.body());
            if (matcher1.find()){
                result.setTitle(matcher1.group(1));
            }
            Matcher matcher2 = matcher_Description.matcher(send.body());
            if (matcher2.find()){
                result.setDescription(matcher2.group(1));
            }
            Matcher matcher3 = matcher_Audio.matcher(send.body());
            if (matcher3.find()){
                result.setAudioURL(matcher3.group(1));
            }

            client.close();
            return Function.gson.toJson(result);

        } catch (Exception e){
            e.printStackTrace();
            client.close();
            return Function.gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }

    }

    @Override
    public String getServiceName() {
        return "Sonicbowl";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
