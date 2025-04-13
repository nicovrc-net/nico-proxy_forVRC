package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.TikTokResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TikTok implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        byte[] content = null;
        TikTokResult result = gson.fromJson(json, TikTokResult.class);

        URI uri = new URI(result.getVideoURL());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                //.uri(new URI("http://localhost:25555/?url="+result.getVideoURL()))
                .headers("User-Agent", Function.UserAgent)
                .headers("Cookie", result.getVideoAccessCookie())
                .headers("Referer", "https://www.tiktok.com/")
                .GET()
                .build();

        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        //System.out.println(send.uri());
        content = send.body();

        send = null;
        request = null;

        ContentObject object = new ContentObject();
        object.setContentObject(content);
        object.setCookieText(result.getVideoAccessCookie());
        object.setRefererText("https://www.tiktok.com/");
        return object;
    }
}
