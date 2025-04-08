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

public class Vimeo implements GetContent {

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

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(element.getAsJsonObject().get("VideoURL").getAsString()))
                    .headers("User-Agent", Function.UserAgent)
                    .GET()
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            //System.out.println(send.uri());
            String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
            byte[] body = send.body();

            if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                String s = new String(body, StandardCharsets.UTF_8);

                String[] split = element.getAsJsonObject().get("VideoURL").getAsString().split("/");

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < split.length - 4; i++){
                    sb.append(split[i]).append("/");
                }

                s = s.replaceAll("\\.\\./\\.\\./\\.\\./", sb.toString());
                s = s.replaceAll("https://", "/https/referer:[]/");
                body = s.getBytes(StandardCharsets.UTF_8);

                //System.out.println(s);
            }

            if (vlc_ua.matcher(httpRequest).find()) {
                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                send = null;
                request = null;

                return null;
            }

            if (!dummy_url.matcher(httpRequest).find()) {
                if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                    body = (new String(body, StandardCharsets.UTF_8) + "\n/dummy.m3u8?url=" + URL + "&dummy=true").getBytes(StandardCharsets.UTF_8);
                }
            }

            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
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

        return null;
    }
}
