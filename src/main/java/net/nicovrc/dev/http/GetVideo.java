package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetVideo implements Runnable, NicoVRCHTTP {

    private String httpRequest = null;
    private String URL = null;
    private Socket sock = null;
    private String proxy = null;

    private final Pattern matcher_cookie = Pattern.compile("cookie:\\[(.*)\\]");
    private final Pattern matcher_referer = Pattern.compile("referer:\\[(.*)\\]");
    private final Pattern matcher_url1 = Pattern.compile("^/https/(cookie|referer):\\[(.*)\\]/(.*)");
    private final Pattern matcher_url2 = Pattern.compile("^/https/(cookie|referer):\\[(.*)\\]/(cookie|referer):\\[(.*)\\]/(.*)");

    private final Pattern matcher_twitcasting = Pattern.compile("twitcasting\\.tv");
    private final Pattern matcher_abema = Pattern.compile("(.+)-abematv\\.akamaized\\.net");
    private final Pattern matcher_vimeo = Pattern.compile("vimeocdn\\.com");
    private final Pattern matcher_fc2 = Pattern.compile("(.+)\\.live\\.fc2\\.com");
    private final Pattern matcher_tiktok = Pattern.compile("tiktok\\.com");
    private final Pattern matcher_bilicom = Pattern.compile("bilibili\\.com");

    private final String http = "https://";

    private final byte[] content;

    public GetVideo(){
        byte[] content = Function.zeroByte;
        try {
            File file = new File("./error-video/error_000.mp4");
            if (file.exists()){
                FileInputStream stream = new FileInputStream(file);
                content = stream.readAllBytes();
                stream.close();
                stream = null;
            }
            file = null;
        } catch (Exception e){
            // e.printStackTrace();
        }
        this.content = content;
    }

    @Override
    public void run() {
        try {
            // Proxy
            if (!Function.ProxyList.isEmpty()){
                int i = Function.ProxyList.size() > 1 ? new SecureRandom().nextInt(0, Function.ProxyList.size()) : 0;
                proxy = Function.ProxyList.get(i);
            }

            //System.out.println("/https/");
            //System.out.println(URL);

            URL = URLDecoder.decode(URL, StandardCharsets.UTF_8);

            String method = Function.getMethod(httpRequest);
            String httpVersion = Function.getHTTPVersion(httpRequest);

            String ContentEncoding = Function.getContentEncoding(httpRequest);

            httpRequest = null;

            Matcher matcher = matcher_cookie.matcher(URL);
            Matcher matcher2 = matcher_referer.matcher(URL);
            String CookieText = null;
            String Referer = null;
            String URL = null;
            if (matcher.find()){
                CookieText = matcher.group(1);
            }
            if (matcher2.find()) {
                Referer = matcher2.group(1);
            }
            if (CookieText != null && Referer != null){
                Matcher matcher3 = matcher_url2.matcher(this.URL);
                //System.out.println("debug1");

                if (matcher3.find()){
                    URL = http+matcher3.group(5);
                }
            } else {
                Matcher matcher3 = matcher_url1.matcher(this.URL);
                //System.out.println("debug2");

                if (matcher3.find()){
                    URL = http+matcher3.group(3);
                }
            }

            System.out.println("Cookie : " + CookieText);
            System.out.println("Referer : " + Referer);

            //System.out.println("debug : " + CookieText + " / " + Referer + " / " + URL);
            if (URL == null) {
                //System.out.println("debug : " + CookieText + " / " + Referer + " / " + URL);
                Function.sendHTTPRequest(sock, httpVersion, 404, "text/plain; charset=utf-8", null,"Video Not Found".getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
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
            Matcher matcher_tiktok = this.matcher_tiktok.matcher(URL);
            Matcher matcher_bilibilicom = matcher_bilicom.matcher(Referer != null ? Referer : "");
            boolean isBiliCom = matcher_bilibilicom.find();
            if (matcher_tiktok.find()){
                URL = URL.replaceAll("\\|", "%7C");
            }

            try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build() :
                    HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .connectTimeout(Duration.ofSeconds(5))
                            .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                            .build()) {

                HttpRequest request;
                if (matcher_fc2url.find()) {
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Accept-Encoding", "gzip, br")
                            .headers("Connection", "keep-alive")
                            .headers("Cookie", "live_media_session=JD052LLx2GF0CXAJuPdlT")
                            .headers("Origin", "https://live.fc2.com")
                            .headers("Referer", "https://live.fc2.com/")
                            .GET()
                            .build()
                    ;
                } else {

                    if (isBiliCom){
                        request = HttpRequest.newBuilder()
                                .uri(new URI(URL))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                .headers("Accept-Encoding", "gzip, br")
                                .headers("Referer", Referer)
                                .HEAD()
                                .build();
                    } else if (CookieText == null || CookieText.isEmpty()){
                        if (Referer == null || Referer.isEmpty()){
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(URL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Accept", "*/*")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .headers("Accept-Encoding", "gzip, br")
                                    .GET()
                                    .build();
                        } else {
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(URL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Referer", Referer)
                                    .headers("Accept", "*/*")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .headers("Accept-Encoding", "gzip, br")
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
                                    .headers("Accept", "*/*")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .headers("Accept-Encoding", "gzip, br")
                                    .GET()
                                    .build();
                        } else {
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(URL))
                                    .headers("User-Agent", Function.UserAgent)
                                    .headers("Cookie", CookieText)
                                    .headers("Referer", Referer)
                                    .headers("Accept", "*/*")
                                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                    .headers("Accept-Encoding", "gzip, br")
                                    .GET()
                                    .build();
                        }
                    }
                }

                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String contentType = send.headers().firstValue("Content-Type").isPresent() ? send.headers().firstValue("Content-Type").get() : send.headers().firstValue("content-type").isPresent() ? send.headers().firstValue("content-type").get() : "";
                String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";

                ContentEncoding = ContentEncoding == null ? "" : ContentEncoding;
                //System.out.println(ContentEncoding);
                //System.out.println(contentEncoding);

                if (!isBiliCom) {
                    //System.out.println("a");
                    byte[] body = Function.decompressByte(send.body(), contentEncoding);

                    if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")){
                        //body = Function.decompressByte(send.body(), contentEncoding);
                        String s = new String(body, StandardCharsets.UTF_8);
                        //System.out.println(s);
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
                    if (!ContentEncoding.isEmpty()){
                        String ce_list = ContentEncoding.toLowerCase(Locale.ROOT);
                        //System.out.println(ce_list);
                        contentEncoding = "";

                        if (ce_list.matches(".*br.*")){
                            body = Function.compressByte(body, "br");

                            contentEncoding = "br";
                        } else if (ce_list.matches(".*gzip.*")){
                            body = Function.compressByte(body, "gzip");

                            contentEncoding = "gzip";

                        }
                    }
                    Function.sendHTTPRequest(sock, httpVersion, send.statusCode(), contentType, contentEncoding, body, method != null && method.equals("HEAD"));
                    body = null;
                } else {

                    int length = Integer.parseInt(send.headers().firstValue("content-length").isPresent() ? send.headers().firstValue("content-length").get() : "0");
                    int max = length / 10;
                    byte[][] temp = new byte[max][10];

                    OutputStream out = sock.getOutputStream();
                    StringBuilder sb_header = new StringBuilder();

                    for (int i = 0; i < 10; i++) {
                        int mi = max * i;
                        int mx = max * (i + 1);
                        //System.out.println(mi + " - " +Math.min(mx - 1, length - 1));
                        request = HttpRequest.newBuilder()
                                .uri(new URI(URL))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Referer", Referer)
                                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                .headers("Accept-Encoding", "gzip, br")
                                // Range bytes=0-1227
                                .headers("Range", "bytes=" + mi + "-" + Math.min(mx - 1, length - 1))
                                .build();
                        send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";

                        //System.out.println(send.statusCode());
                        //out.write(send.body());
                        temp[i] = Function.decompressByte(send.body(), contentEncoding);
                        if (send.statusCode() >= 300){
                            break;
                        }

                    }
                    //contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                    //System.out.println("o : "+contentEncoding);
                    byte[] bytes = concatByteArrays(temp[0], temp[1], temp[2], temp[3], temp[4], temp[5], temp[6], temp[7], temp[8], temp[9]);
                    sb_header.append("HTTP/").append(httpVersion == null ? "1.1" : httpVersion).append(" 200 OK\r\n");
                    sb_header.append("Content-Length: ").append(bytes.length).append("\r\n");
                    sb_header.append("Content-Type: ").append(contentType).append("\r\n");

                    if (!ContentEncoding.isEmpty()){
                        String ce_list = ContentEncoding.toLowerCase(Locale.ROOT);
                        //System.out.println(ce_list);
                        contentEncoding = "";

                        if (ce_list.matches(".*br.*")){
                            bytes = Function.compressByte(bytes, "br");

                            contentEncoding = "br";
                        } else if (ce_list.matches(".*gzip.*")){
                            bytes = Function.compressByte(bytes, "gzip");
                            contentEncoding = "gzip";

                        }

                        //System.out.println("d : "+contentEncoding);

                        if (!contentEncoding.isEmpty()){
                            sb_header.append("Content-Encoding: ").append(contentEncoding).append("\r\n");
                        }
                    }
                    sb_header.append("Date: ").append(new Date()).append("\r\n");

                    sb_header.append("\r\n");
                    out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));

                    out.write(bytes);
                    out.flush();
                    out = null;
                    bytes = null;
                    sb_header.setLength(0);
                    sb_header = null;

                }
                method = null;
                httpVersion = null;
                send = null;
                request = null;
                contentType = null;

            } catch (Exception e){
                e.printStackTrace();
                Function.sendHTTPRequest(sock, httpVersion, 200, "video/mp4", null, content, method != null && method.equals("HEAD"));
            }
            matcher_fc2url = null;
            matcher_twit = null;
            matcher_abematv = null;
            matcher_vimeourl = null;
            matcher_bilibilicom = null;

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

    private static byte[] concatByteArrays(byte[]... arrays) {
        return Arrays.stream(arrays)
                .collect(ByteArrayOutputStream::new,
                        ByteArrayOutputStream::writeBytes,
                        (left, right) -> left.writeBytes(right.toByteArray()))
                .toByteArray();
    }
}
