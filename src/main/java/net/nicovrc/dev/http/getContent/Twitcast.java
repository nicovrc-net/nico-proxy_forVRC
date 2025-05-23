package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Twitcast implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        String hlsText = null;
        JsonElement element = gson.fromJson(json, JsonElement.class);

        String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(targetURL))
                .headers("User-Agent", Function.UserAgent)
                .headers("Referer", "https://twitcasting.tv/")
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        hlsText = send.body();

        hlsText = hlsText.replaceAll("/tc\\.livehls/", "/https/referer:[https://twitcasting.tv/]/cookie:[]/"+request.uri().getHost()+"/tc.livehls/");
        hlsText = hlsText.replaceAll("/tc\\.vod\\.v2/", "/https/referer:[https://twitcasting.tv/]/cookie:[]/"+request.uri().getHost()+"/tc.vod.v2/");

        ContentObject object = new ContentObject();
        object.setHLSText(hlsText);
        object.setDummyHLSText(null);
        object.setRefererText("https://twitcasting.tv/");
        return object;
    }
}
