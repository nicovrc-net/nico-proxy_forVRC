package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.bandcampResult;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class bandcamp implements ServiceAPI {

    private String url = null;
    private String proxy = null;

    private final Gson gson = Function.gson;

    private final Pattern matcher_json = Pattern.compile("data-tralbum=\"\\{(.+)}\" data-payment=");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"*.bandcamp.com"};
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

        final HttpClient client;
        if (proxy != null){
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                    .build();
        } else {
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
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

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String result = response.body();
            //client.close();

            //System.out.println(result);

            Matcher matcher = matcher_json.matcher(result);
            if (!matcher.find()){
                return gson.toJson(new ErrorMessage("対応してないURLです。"));
            }

            String s = "{" + matcher.group(1).replaceAll("&quot;", "\"") + "}";
            //System.out.println(s);

            JsonElement json = new Gson().fromJson(s, JsonElement.class);
            //System.out.println(json);

            JsonArray trackinfo = json.getAsJsonObject().get("trackinfo").getAsJsonArray();
            String[] audio = {"", ""};
            Map<String, JsonElement> file = trackinfo.get(0).getAsJsonObject().get("file").getAsJsonObject().asMap();
            file.forEach((name, value)->{
                if (audio[0].isEmpty()){
                    audio[0] = value.getAsString();
                    audio[1] = trackinfo.get(0).getAsJsonObject().get("title").getAsString();
                }
            });

            bandcampResult result1 = new bandcampResult();
            result1.setTitle(audio[1]);
            result1.setAudioURL(audio[0]);

            client.close();
            return gson.toJson(result1);

        } catch (Exception e){
            e.printStackTrace();
            return gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "bandcamp";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
