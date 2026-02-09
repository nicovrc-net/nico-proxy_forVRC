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
    private HttpClient client = null;

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"youtu.be", "www.youtube.com"};
    }

    @Override
    public void Set(String json, HttpClient client) {
        JsonElement element = Function.gson.fromJson(json, JsonElement.class);
        if (element.isJsonObject() && element.getAsJsonObject().has("URL")){
            url = element.getAsJsonObject().get("URL").getAsString();
        }
        this.client = client;
    }

    @Override
    public String Get() {
        try {
            URI uri = new URI("https://yt.8uro.net/r?v="+url+"&o=nicovrc");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", "UnityPlayer/2022.3.22f1-DWR (UnityWebRequest/1.0, libcurl/8.5.0-DEV) nicovrc-net/"+Function.Version)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String title = send.body();
            client.close();

            return "{\"Title\": \""+title+"\",\"VideoURL\": \"https://yt.8uro.net/r?v="+url+"&o=nicovrc\"}";
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
