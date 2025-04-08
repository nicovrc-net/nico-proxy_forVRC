package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.TikTokResult;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TikTok implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(Socket sock, HttpClient client, String httpRequest, String URL, String json) {

        final String method = Function.getMethod(httpRequest);
        byte[] content = null;
        TikTokResult result = gson.fromJson(json, TikTokResult.class);

        try {
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
            String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
            content = send.body();
            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, content, method != null && method.equals("HEAD"));

            send = null;
            request = null;

        } catch (Exception e){
            // e.printStackTrace();
            try {
                byte[] errorContent = null;
                File file = new File("./error-video/error_000.mp4");
                if (file.exists()){
                    FileInputStream stream = new FileInputStream(file);
                    errorContent = stream.readAllBytes();
                    stream.close();
                    stream = null;
                }

                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", errorContent, method != null && method.equals("HEAD"));
                errorContent = null;
            } catch (Exception ex){
                // ex.printStackTrace();
            }
        }

        ContentObject object = new ContentObject();
        object.setContentObject(content);
        return object;
    }
}
