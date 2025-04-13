package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Vimeo implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        String dummy_hlsText = null;
        String hlsText = null;
        JsonElement element = gson.fromJson(json, JsonElement.class);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(element.getAsJsonObject().get("VideoURL").getAsString()))
                .headers("User-Agent", Function.UserAgent)
                .GET()
                .build();

        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        //System.out.println(send.uri());
        byte[] body = send.body();

        hlsText = new String(body, StandardCharsets.UTF_8);

        String[] split = element.getAsJsonObject().get("VideoURL").getAsString().split("/");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length - 4; i++){
            sb.append(split[i]).append("/");
        }

        hlsText = hlsText.replaceAll("\\.\\./\\.\\./\\.\\./", sb.toString());
        hlsText = hlsText.replaceAll("https://", "/https/referer:[]/");
        dummy_hlsText = new String(body, StandardCharsets.UTF_8) + "\n/dummy.m3u8?url=" + URL + "&dummy=true";

        send = null;
        request = null;

        ContentObject object = new ContentObject();
        object.setHLSText(hlsText);
        object.setDummyHLSText(dummy_hlsText);
        return object;
    }
}
