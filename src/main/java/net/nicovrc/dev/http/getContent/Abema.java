package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Abema implements GetContent {

    private final Gson gson = Function.gson;


    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        String hlsText = null;
        JsonElement element = gson.fromJson(json, JsonElement.class);

        String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(targetURL))
                .headers("User-Agent", Function.UserAgent)
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        hlsText = send.body();
        hlsText = hlsText.replaceAll("180/", "/https/referer:[]/cookie:[]/"+targetURL.replaceAll("https://", "").replaceAll("playlist\\.m3u8", "")+"180/");
        hlsText = hlsText.replaceAll("240/", "/https/referer:[]/cookie:[]/"+targetURL.replaceAll("https://", "").replaceAll("playlist\\.m3u8", "")+"240/");
        hlsText = hlsText.replaceAll("480/", "/https/referer:[]/cookie:[]/"+targetURL.replaceAll("https://", "").replaceAll("playlist\\.m3u8", "")+"480/");
        hlsText = hlsText.replaceAll("720/", "/https/referer:[]/cookie:[]/"+targetURL.replaceAll("https://", "").replaceAll("playlist\\.m3u8", "")+"720/");
        hlsText = hlsText.replaceAll("1080/", "/https/referer:[]/cookie:[]/"+targetURL.replaceAll("https://", "").replaceAll("playlist\\.m3u8", "")+"1080/");

        ContentObject object = new ContentObject();
        object.setHLSText(hlsText);
        object.setDummyHLSText(hlsText);

        return object;
    }
}
