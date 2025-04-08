package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public class TVer implements GetContent {

    private final Gson gson = Function.gson;
    private final Pattern dummy_url = Pattern.compile("dummy=true");
    private final Pattern vlc_ua = Pattern.compile("(VLC/(.+) LibVLC/(.+)|LibVLC)");

    @Override
    public ContentObject run(Socket sock, HttpClient client, String httpRequest, String URL, String json) {

        final String method = Function.getMethod(httpRequest);
        String dummy_hlsText = null;
        String hlsText = null;
        JsonElement element = gson.fromJson(json, JsonElement.class);

        try {

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

            if (!dummy_url.matcher(URL).find() && !vlc_ua.matcher(httpRequest).find()) {
                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, dummy_hlsText.getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
            } else {
                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, hlsText.getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
            }

            send = null;
            request = null;

        } catch (Exception e){
            // e.printStackTrace();
            try {
                byte[] content = null;
                File file = new File("./error-video/error_000.mp4");
                if (file.exists()){
                    FileInputStream stream = new FileInputStream(file);
                    content = stream.readAllBytes();
                    stream.close();
                    stream = null;
                }

                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                content = null;
            } catch (Exception ex){
                // ex.printStackTrace();
            }
        }

        ContentObject object = new ContentObject();
        object.setHLSText(hlsText != null ? hlsText.getBytes(StandardCharsets.UTF_8) : null);
        object.setDummyHLSText(dummy_hlsText != null ? dummy_hlsText.getBytes(StandardCharsets.UTF_8) : null);
        return object;
    }
}
