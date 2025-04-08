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

public class XVIDEOS implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(Socket sock, HttpClient client, String httpRequest, String URL, String json) {

        final String method = Function.getMethod(httpRequest);
        String hlsText = null;
        JsonElement element = gson.fromJson(json, JsonElement.class);

        try {

            String targetURL = element.getAsJsonObject().get("VideoURL").getAsString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(targetURL))
                    .headers("User-Agent", Function.UserAgent)
                    .GET()
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            //System.out.println(send.uri());
            String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
            byte[] body = send.body();

            if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                hlsText = new String(body, StandardCharsets.UTF_8);

                StringBuilder sb = new StringBuilder();

                for (String str : hlsText.split("\n")){
                    if (str.startsWith("#")){
                        sb.append(str).append("\n");
                        continue;
                    }

                    String s1 = targetURL.split("/")[targetURL.split("/").length - 1];
                    sb.append(targetURL.replaceAll(s1, "")).append(str).append("\n");
                    break;
                }

                hlsText = sb.toString();
                hlsText = hlsText.replaceAll("https://", "/https/cookie:[]/");

                body = hlsText.getBytes(StandardCharsets.UTF_8);
                sb.setLength(0);
                //System.out.println(s);
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

        ContentObject object = new ContentObject();
        object.setHLSText(hlsText != null ? hlsText.getBytes(StandardCharsets.UTF_8) : null);
        object.setDummyHLSText(null);
        return object;
    }
}
