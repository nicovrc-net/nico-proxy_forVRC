package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetVideo implements Runnable, NicoVRCHTTP {

    private String httpRequest = null;
    private String URL = null;
    private Socket sock = null;
    private String proxy = null;

    private final Pattern matcher_url1 = Pattern.compile("/https/cookie:\\[(.*)\\]/(.+)");
    private final Pattern matcher_url2 = Pattern.compile("/https/referer:\\[(.*)\\]/(.+)");
    private final Pattern matcher_url3 = Pattern.compile("/https/referer:\\[(.*)\\]/cookie:\\[(.*)\\]/(.+)");

    private final Pattern matcher_twitcasting = Pattern.compile("twitcasting\\.tv");
    private final Pattern matcher_abema = Pattern.compile("(.+)-abematv\\.akamaized\\.net");
    private final Pattern matcher_vimeo = Pattern.compile("vimeocdn\\.com");
    private final Pattern matcher_fc2 = Pattern.compile("(.+)\\.live\\.fc2\\.com");

    private final String http = "https://";

    @Override
    public void run() {
        try {
            // Proxy
            if (!Function.ProxyList.isEmpty()){
                int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
                proxy = Function.ProxyList.get(i);
            }

            //System.out.println("/https/");
            //System.out.println(URL);

            URL = URLDecoder.decode(URL, StandardCharsets.UTF_8);

            Matcher matcher = matcher_url1.matcher(URL);
            Matcher matcher2 = matcher_url2.matcher(URL);
            Matcher matcher3 = matcher_url3.matcher(URL);

            String method = Function.getMethod(httpRequest);
            String httpVersion = Function.getHTTPVersion(httpRequest);

            httpRequest = null;

            String CookieText = null;
            String Referer = null;
            String URL = null;
            if (matcher.find()){
                CookieText = matcher.group(1);
                URL = http+matcher.group(2);
            }
            if (matcher2.find()) {
                Referer = matcher2.group(1);
                URL = http+matcher2.group(2);
            }
            if (matcher3.find()) {
                Referer = matcher3.group(1);
                CookieText = matcher3.group(2);
                URL = http+matcher3.group(3);
            }

            if (CookieText == null && Referer == null && URL == null) {
                //System.out.println("debug : " + CookieText + " / " + Referer + " / " + URL);
                Function.sendHTTPRequest(sock, httpVersion, 404, "text/plain; charset=utf-8","Video Not Found".getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                method = null;
                httpVersion = null;

                return;
            }

            if (CookieText != null && CookieText.equals("null")){
                CookieText = null;
            }
            if (Referer != null && Referer.equals("null")){
                Referer = null;
            }

            //System.out.println("debug : " + CookieText + " / " + Referer + " / " + URL);

            Matcher matcher_fc2url = matcher_fc2.matcher(URL);
            Matcher matcher_twit = matcher_twitcasting.matcher(URL);
            Matcher matcher_abematv = matcher_abema.matcher(URL);
            Matcher matcher_vimeourl = matcher_vimeo.matcher(URL);

            try {

                HttpClient client = proxy == null ? HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build() :
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofSeconds(5))
                                .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                .build();

                HttpRequest request;
                if (matcher_fc2url.find()){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Cookie", "live_media_session=JD052LLx2GF0CXAJuPdlT")
                            .headers("Origin", "https://live.fc2.com")
                            .headers("Referer", "https://live.fc2.com/")
                            .GET()
                            .build();
                } else {
                    if (CookieText == null || CookieText.isEmpty()){
                        if (Referer == null || Referer.isEmpty()){
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(URL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .GET()
                                    .build();
                        } else {
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(URL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Referer", Referer)
                                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .GET()
                                    .build();
                        }
                    } else {
                        //System.out.println(URL);
                        if (Referer == null || Referer.isEmpty()){
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(URL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Cookie", CookieText)
                                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .GET()
                                    .build();
                        } else {
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(URL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Cookie", CookieText)
                                    .headers("Referer", Referer)
                                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .GET()
                                    .build();
                        }
                    }
                }


                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String contentType = send.headers().firstValue("Content-Type").isPresent() ? send.headers().firstValue("Content-Type").get() : send.headers().firstValue("content-type").isPresent() ? send.headers().firstValue("content-type").get() : "";
/*
                System.out.println("----");
                send.headers().map().forEach((name, value)->{
                    System.out.println(name + " : ");
                    value.forEach((v)->{
                        System.out.println("   " + v);
                    });
                });
                System.out.println("----");*/

                //System.out.println("a");
                byte[] body = send.body();

                client.close();
                client = null;

                if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")){
                    String s = new String(body, StandardCharsets.UTF_8);
                    if (matcher_twit.find()) {
                        s = s.replaceAll(http, "/https/referer:[" + Referer + "]/");
                        s = s.replaceAll("\"/tc\\.vod\\.v2", "\"/https/referer:[" + Referer + "]/" + request.uri().getHost() + "/tc.vod.v2");

                        StringBuilder sb = new StringBuilder();
                        for (String str : s.split("\n")) {
                            if (!str.startsWith("/mpegts") && !str.startsWith("/tc.vod.v2")) {
                                sb.append(str).append("\n");
                                continue;
                            }

                            sb.append("/https/referer:[").append(Referer).append("]/").append(request.uri().getHost()).append(str).append("\n");

                        }

                        s = sb.toString();
                        sb.setLength(0);
                        sb = null;

                    } else if (matcher_abematv.find()) {
                        //System.out.println("!!!!");
                        s = s.replaceAll(http, "/https/cookie:[]/");

                        StringBuilder sb = new StringBuilder();
                        for (String str : s.split("\n")) {
                            if (str.startsWith("/tsad")){
                                sb.append("/https/referer:[]/").append(request.uri().getHost()).append(str).append("\n");
                                continue;
                            }
                            if (!str.startsWith("/preview")) {
                                sb.append(str.replaceAll(http, "/https/cookie:[]/")).append("\n");
                                continue;
                            }

                            sb.append("/https/referer:[]/").append(request.uri().getHost()).append(str).append("\n");

                        }

                        s = sb.toString();
                        sb.setLength(0);
                        sb = null;
                    } else if (matcher_vimeourl.find()) {

                        StringBuilder sb = new StringBuilder();
                        String[] split = URL.split("/");
                        for (int i = 0; i < split.length - 6; i++) {
                            sb.append(split[i]).append("/");
                        }

                        s = s.replaceAll("\\.\\./\\.\\./\\.\\./\\.\\./\\.\\./", sb.toString());
                        s = s.replaceAll(http, "/https/cookie:[]/");

                        sb.setLength(0);
                        sb = null;

                    } else {
                        if (CookieText != null && !CookieText.isEmpty()){
                            if (Referer == null || Referer.isEmpty()){
                                s = s.replaceAll(http, "/https/cookie:["+CookieText+"]/");
                            } else {
                                s = s.replaceAll(http, "/https/referer:["+Referer+"]/cookie:["+CookieText+"]/");
                            }
                        } else {
                            if (Referer == null || Referer.isEmpty()){
                                s = s.replaceAll(http, "/https/cookie:[]/");
                            } else {
                                s = s.replaceAll(http, "/https/referer:["+Referer+"]/");
                            }
                        }
                    }

                    body = s.getBytes(StandardCharsets.UTF_8);
                    s = null;
                }
                //System.out.println("b");
                Function.sendHTTPRequest(sock, httpVersion, send.statusCode(), contentType, body, method != null && method.equals("HEAD"));

                body = null;
                method = null;
                httpVersion = null;
                send = null;
                request = null;
                contentType = null;
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
                     ex.printStackTrace();
                }
            }
            matcher_fc2url = null;
            matcher_twit = null;
            matcher_abematv = null;
            matcher_vimeourl = null;

        } catch (Exception e){
             e.printStackTrace();
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
