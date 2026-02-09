package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.SonicbowlResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sonicbowl implements ServiceAPI {

    private String proxy = null;
    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_Audio = Pattern.compile("<meta property=\"og:audio\" content=\"(.+)\">");
    private final Pattern matcher_Title = Pattern.compile("<title>(.+)</title>");
    private final Pattern matcher_Description = Pattern.compile("<meta name=\"description\" content=\"(.+)\">");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"player.sonicbowl.cloud"};
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

        if (url == null || url.isEmpty()){
            return Function.gson.toJson(new ErrorMessage("URLが入力されていません。"));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
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

            SonicbowlResult result = new SonicbowlResult();

            Matcher matcher1 = matcher_Title.matcher(text);
            if (matcher1.find()){
                result.setTitle(matcher1.group(1));
            }
            Matcher matcher2 = matcher_Description.matcher(text);
            if (matcher2.find()){
                result.setDescription(matcher2.group(1));
            }
            Matcher matcher3 = matcher_Audio.matcher(text);
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
