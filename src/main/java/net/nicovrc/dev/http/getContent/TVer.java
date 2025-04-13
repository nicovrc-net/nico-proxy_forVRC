package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TVer implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        final String method = Function.getMethod(httpRequest);
        String dummy_hlsText = null;
        String hlsText = null;
        JsonElement element = gson.fromJson(json, JsonElement.class);

        String url = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers("User-Agent", Function.UserAgent)
                .headers("Origin", "https://tver.jp")
                .headers("Referer", "https://tver.jp/")
                .GET()
                .build();

        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        //System.out.println(send.uri());
        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
        byte[] body = send.body();

        hlsText = new String(body, StandardCharsets.UTF_8);
        hlsText = hlsText.replaceAll("https://", "/https/referer:[https://tver.jp/]/");
        //body = hlsText.getBytes(StandardCharsets.UTF_8);
        dummy_hlsText = new String(body, StandardCharsets.UTF_8).replaceAll("https://", "/https/referer:[https://tver.jp/]/") + "\n/dummy.m3u8?url=" + URL + "&dummy=true";

        send = null;
        request = null;

        ContentObject object = new ContentObject();
        object.setHLSText(hlsText);
        object.setDummyHLSText(dummy_hlsText);
        object.setRefererText("https://tver.jp/");
        return object;
    }
}
