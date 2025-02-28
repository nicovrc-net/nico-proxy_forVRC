package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

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

public class piapro implements ServiceAPI {
    private String proxy = null;
    private String url = null;

    private final Pattern matcher_url1 = Pattern.compile("https://piapro\\.jp/t/(.+)");
    private final Pattern matcher_url = Pattern.compile("\"url\": \"(.+)\",");
    private final Pattern matcher_title = Pattern.compile("<h1 class=\"contents_title\">(.+)</h1>");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"piapro.jp"};
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

        if (!matcher_url1.matcher(url).find()){
            return "{\"ErrorMessage\": \"対応していないURLです。\"}";
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

            Matcher matcher1 = matcher_url.matcher(send.body());
            Matcher matcher2 = matcher_title.matcher(send.body());
            String Title = "";
            String url = "";
            if (matcher1.find()){
                url = matcher1.group(1);
            } else {
                return "{\"ErrorMessage\": \"取得に失敗しました。\"}";
            }
            if (matcher2.find()){
                Title = matcher2.group(1);
            }

            return "{\"Title\": \""+Title+"\", \"AudioURL\": \""+url+"\"}";

        } catch (Exception e){
            e.printStackTrace();
            return "{\"ErrorMessage\": \"内部エラーです。 ("+e.getMessage().replaceAll("\"","\\\\\"")+"\"}";
        }
    }

    @Override
    public String getServiceName() {
        return "piapro";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
