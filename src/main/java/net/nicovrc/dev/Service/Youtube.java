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

public class Youtube implements ServiceAPI {

    private String url = null;
    private String Proxy = null;

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"youtu.be", "www.youtube.com"};
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

            URI uri = new URI("https://yt.8uro.net/r?v="+url+"&o=nicovrc");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", "UnityPlayer/2022.3.22f1-DWR (UnityWebRequest/1.0, libcurl/8.5.0-DEV) nicovrc-net/"+Function.Version)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            return "{\"Title\": \""+send.body()+"\",\"VideoURL\": \"https://yt.8uro.net/r?v="+url+"&o=nicovrc\"}";
        } catch (Exception e){
            return "{\"VideoURL\": \"https://yt.8uro.net/r?v="+url+"&o=nicovrc\"}";
        }
    }

    @Override
    public String getServiceName() {
        return "Youtube";
    }

    @Override
    public String getUseProxy() {
        return Proxy;
    }
}
