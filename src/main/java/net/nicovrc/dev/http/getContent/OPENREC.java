package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.OPENREC_Result;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class OPENREC implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(Socket sock, HttpClient client, String httpRequest, String URL, String json) {

        final String method = Function.getMethod(httpRequest);
        String hlsText = null;
        OPENREC_Result result = gson.fromJson(json, OPENREC_Result.class);

        try {
            String url = result.isLive() ? result.getLiveURL() : result.getVideoURL();
            URI uri = new URI(url);
            //System.out.println(s);
            //System.out.println(url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Referer", URL)
                    .GET()
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            //System.out.println(send.uri());
            String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
            byte[] body = send.body();
            //System.out.println(contentType.toLowerCase(Locale.ROOT));
            if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl")) {
                //System.out.println("!!!");
                StringBuilder sb = new StringBuilder();
                hlsText = new String(send.body(), StandardCharsets.UTF_8);
                String[] split = url.split("/");
                boolean isEnd = false;
                for (String s : hlsText.split("\n")) {
                    if (!isEnd){
                        if (s.startsWith("#")){
                            sb.append(s).append("\n");
                            continue;
                        }
                        sb.append(url.replaceAll(split[split.length - 1], "")).append(s).append("\n");
                        isEnd = true;
                    }
                }
                hlsText = sb.toString();
                hlsText = hlsText.replaceAll("https://", "/https/referer:["+URL+"]/");
                body = hlsText.getBytes(StandardCharsets.UTF_8);
            }

            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));

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
