package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetVideo implements Runnable, NicoVRCHTTP {

    private String httpRequest = null;
    private String URL = null;
    private Socket sock = null;

    private final Pattern matcher_url = Pattern.compile("/https/cookie:\\[(.+)\\]/(.+)");

    @Override
    public void run() {
        try {
            Matcher matcher = matcher_url.matcher(URL);

            String method = Function.getMethod(httpRequest);
            String httpVersion = Function.getHTTPVersion(httpRequest);

            String CookieText = null;
            String URL = null;
            if (matcher.find()){
                CookieText = matcher.group(1);
                URL = matcher.group(2);
            } else {
                Function.sendHTTPRequest(sock, httpVersion, 404, "text/plain; charset=utf-8","Video Not Found".getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                method = null;
                httpVersion = null;

                return;
            }
            try (HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()) {

                HttpRequest request;
                if (CookieText == null || CookieText.isEmpty()){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();
                } else {
                    System.out.println(URL);
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Cookie", CookieText)
                            .GET()
                            .build();
                }

                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                Function.sendHTTPRequest(sock, httpVersion, send.statusCode(), send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get(), send.body(), method != null && method.equals("HEAD"));

                method = null;
                httpVersion = null;


            } catch (Exception e){
                e.printStackTrace();
                try {
                    File file = new File("./error-video/error_000.mp4");
                    if (file.exists()){
                        FileInputStream stream = new FileInputStream(file);
                        byte[] content = stream.readAllBytes();
                        stream.close();
                        stream = null;

                        Function.sendHTTPRequest(sock, httpVersion, 200, "video/mp4", content, method != null && method.equals("HEAD"));

                        content = null;

                    }
                    file = null;
                } catch (Exception ex){
                    // ex.printStackTrace();
                }
            }

        } catch (Exception e){
            // e.printStackTrace();
        }
    }

    @Override
    public String getStartURI() {
        return "/https/";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public void setURL(String URL) {
        this.URL = URL;
    }

    @Override
    public void setHTTPSocket(Socket sock) {
        this.sock = sock;
    }
}
