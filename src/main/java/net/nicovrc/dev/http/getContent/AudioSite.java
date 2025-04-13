package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public class AudioSite implements GetContent {

    private final Gson gson = Function.gson;
    private final Pattern matcher_sonicbowl = Pattern.compile("sonicbowl\\.cloud");


    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        String hlsText = null;
        JsonElement element = gson.fromJson(json, JsonElement.class);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(element.getAsJsonObject().get("AudioURL").getAsString()))
                .headers("User-Agent", Function.UserAgent)
                .GET()
                .build();

        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (matcher_sonicbowl.matcher(element.getAsJsonObject().get("AudioURL").getAsString()).find() && send.statusCode() == 302){
            String location = send.headers().firstValue("location").get();
            request = HttpRequest.newBuilder()
                    .uri(new URI(location))
                    .headers("User-Agent", Function.UserAgent)
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }


        //System.out.println(send.uri());
        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
        byte[] body = send.body();

        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
            hlsText = new String(body, StandardCharsets.UTF_8);
            hlsText = hlsText.replaceAll("https://", "/https/referer:[]/");
            body = hlsText.getBytes(StandardCharsets.UTF_8);

            //System.out.println(s);
        }

        ContentObject object = new ContentObject();
        object.setDummyHLSText(null);
        object.setHLSText(hlsText);
        return object;
    }
}
