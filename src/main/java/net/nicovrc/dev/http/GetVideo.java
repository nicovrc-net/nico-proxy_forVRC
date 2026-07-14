package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.data.HttpHeader;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetVideo implements Runnable, NicoVRCHTTP {

    private String httpRequest = null;
    private String URL = null;
    private AsynchronousSocketChannel ch = null;
    private HttpClient client = null;

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

    //private final Pattern matcher_niconico = Pattern.compile("nicovideo\\.jp");
    private final Pattern matcher_host = Pattern.compile("[H|h]ost: (.+)");
    //private final Pattern matcher_hlsUri = Pattern.compile("URI=\"(.+)\"");
    //private final Pattern matcher_hlsKey = Pattern.compile("IV=(.+)");
    private final Pattern matcher_UA = Pattern.compile("(GStreamer|AVProMobileVideo|VLC/3\\.0\\.6)");


    private final Pattern matcher_bili_range1 = Pattern.compile("[r|R]ange: bytes=(\\d+)-(\\d+)");
    private final Pattern matcher_bili_range2 = Pattern.compile("[r|R]ange: bytes=(\\d+)-");

    private final String http = "https://";

    @Override
    public void run() {
        if (client == null){
            return;
        }

        if (ch == null){
            return;
        }

        try {
            //System.out.println("/https/");
            //System.out.println(URL);

            //System.out.println(httpRequest);

            URL = URLDecoder.decode(URL, StandardCharsets.UTF_8);

            Matcher m = matcher_UA.matcher(httpRequest);
            if (m.find()) {
                URL = URLDecoder.decode(URL.replaceAll("_ss_", "[").replaceAll("_se_", "]"), StandardCharsets.UTF_8);
                URL = URL.replaceAll("_dot_", ".");
            }

            Matcher matcher1 = matcher_host.matcher(httpRequest);
            String hostname = matcher1.find() ? matcher1.group(1) : "localhost:"+Function.config_httpPort;

            String httpVersion = Function.getHTTPVersion(httpRequest);

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

            //System.out.println("debug : " + CookieText + " / " + Referer + " / " + URL);
            if (URL == null) {
                //System.out.println("debug : " + CookieText + " / " + Referer + " / " + URL);

                Function.sendHttpData(ch, new HttpHeader(httpVersion, 404, Function.contentType_textPlain, null, null, Function.content_VideoNotFound, null));
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
            //Matcher matcher_avproMobile = Function.avproM_ua.matcher(httpRequest);
            //Matcher matcher_nico = matcher_niconico.matcher(URL);
            //Matcher matcher_hostname = matcher_host.matcher(httpRequest);
            boolean isBiliCom = matcher_bilibilicom.find();
            if (matcher_tiktok.find()){
                URL = URL.replaceAll("\\|", "%7C");
            }

            HttpRequest request;
            if (matcher_fc2url.find()) {
                request = HttpRequest.newBuilder()
                        .uri(new URI(URL))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Connection", "keep-alive")
                        .headers("Cookie", "live_media_session=JD052LLx2GF0CXAJuPdlT")
                        .headers("Origin", "https://live.fc2.com")
                        .headers("Referer", "https://live.fc2.com/")
                        .GET()
                        .build();
            } else {

                if (isBiliCom){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
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
                                .GET()
                                .build();
                    } else {
                        request = HttpRequest.newBuilder()
                                .uri(new URI(URL))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Referer", Referer)
                                .headers("Accept", "*/*")
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
                                .headers("Accept", "*/*")
                                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
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
                                .GET()
                                .build();
                    }
                }
            }

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = send.headers().firstValue("Content-Type").isPresent() ? send.headers().firstValue("Content-Type").get() : send.headers().firstValue("content-type").isPresent() ? send.headers().firstValue("content-type").get() : "";

            if (!isBiliCom) {
                byte[] send_data = send.body();
                //System.out.println("a");

                if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")){
                    //body = Function.decompressByte(send.body(), contentEncoding);
                    String s = new String(send_data, StandardCharsets.UTF_8);
                    //System.out.println(s);
                    /*
                    if (matcher_nico.find() && matcher_hostname.find() && matcher_avproMobile.find()){
                        String host = matcher_hostname.group(1);
                        StringBuffer sb = new StringBuffer();
                        String[] split = s.split("\n");

                        for (String string : split) {
                            if (CookieText != null && !CookieText.isEmpty()){
                                if (Referer == null || Referer.isEmpty()){
                                    string = string.replaceAll(http, "/https/cookie:["+CookieText+"]/");
                                } else {
                                    string = string.replaceAll(http, "/https/referer:["+Referer+"]/cookie:["+CookieText+"]/");
                                }
                            } else {
                                if (Referer == null || Referer.isEmpty()){
                                    string = string.replaceAll(http, "/https/cookie:[]/");
                                } else {
                                    string = string.replaceAll(http, "/https/referer:["+Referer+"]/");
                                }
                            }

                            Matcher matcher3 = matcher_hlsUri.matcher(string);
                            Matcher matcher4 = matcher_hlsKey.matcher(string);
                            //System.out.println("d:"+string);
                            if (matcher3.find()) {
                                String url = matcher3.group(1);
                                String url_encode = URLEncoder.encode(url, StandardCharsets.UTF_8).replaceAll("%2F", "/").replaceAll("%3F", "?").replaceAll("%26", "&").replaceAll("%3D", "=").replaceAll("\\.", "_dot_").replaceAll("_dot_cmfa", ".cmfa").replaceAll("_dot_cmfv", ".cmfv").replaceAll("_dot_key", ".key");

                                //System.out.println("d:"+url);
                                //System.out.println("d:"+url_encode);

                                if (string.startsWith("#EXT-X-MAP")){
                                    sb.append("#EXT-X-MAP:URI=\"").append("https://").append(host).append(url_encode).append("\"\n");
                                } else if (matcher4.find()) {
                                    sb.append("#EXT-X-KEY:METHOD=AES-128,\"").append("https://").append(host).append(url_encode).append("\"").append(",IV=").append(matcher4.group(1)).append("\n");
                                }
                            } else if (string.startsWith("/https/")){
                                String encode = URLEncoder.encode(string, StandardCharsets.UTF_8).replaceAll("%2F", "/").replaceAll("%3F", "?").replaceAll("%26", "&").replaceAll("%3D", "=").replaceAll("\\.", "_dot_").replaceAll("_dot_cmfa", ".cmfa").replaceAll("_dot_cmfv", ".cmfv");
                                sb.append("https://").append(host).append(encode).append("\n");

                            } else {
                                sb.append(string).append("\n");
                            }
                        }

                        s = sb.toString();
                    } else*/ if (matcher_twit.find()) {
                        s = s.replaceAll(http, "/https/referer:[" + Referer + "]/");
                        s = s.replaceAll("\"/tc\\.vod\\.v2", "\"/https/referer:[" + Referer + "]/" + request.uri().getHost() + "/tc.vod.v2");

                        StringBuffer sb = new StringBuffer();
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

                        StringBuffer sb = new StringBuffer();
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

                        StringBuffer sb = new StringBuffer();
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
                                if (m.find()){
                                    s = s.replaceAll(http, "https://"+hostname+"/https/cookie:_ss_"+URLEncoder.encode(CookieText, StandardCharsets.UTF_8)+"_se_/");
                                } else {
                                    s = s.replaceAll(http, "/https/cookie:["+CookieText+"]/");
                                }

                            } else {
                                if (m.find()){
                                    s = s.replaceAll(http, "https://"+hostname+"/https/referer:_ss_"+URLEncoder.encode(Referer, StandardCharsets.UTF_8)+"_se_/cookie:_ss_"+URLEncoder.encode(CookieText, StandardCharsets.UTF_8)+"_se_/");
                                } else {
                                    s = s.replaceAll(http, "/https/referer:["+Referer+"]/cookie:["+CookieText+"]/");
                                }
                            }
                        } else {
                            if (Referer == null || Referer.isEmpty()){
                                if (m.find()){
                                    s = s.replaceAll(http, "https://"+hostname+"/https/cookie:_ss__se_/");
                                } else {
                                    s = s.replaceAll(http, "/https/cookie:[]/");
                                }
                            } else {
                                if (m.find()) {
                                    s = s.replaceAll(http, "https://"+hostname+"/https/referer:_ss_"+URLEncoder.encode(Referer, StandardCharsets.UTF_8)+"_se_/");
                                } else {
                                    s = s.replaceAll(http, "/https/referer:["+Referer+"]/");
                                }
                            }
                        }
                    }

                    send_data = s.getBytes(StandardCharsets.UTF_8);

                    s = null;
                }
                //System.out.println("b");
                Function.sendHttpData(ch, new HttpHeader(httpVersion, send.statusCode(), contentType, null, null, send_data, null));

            } else {

                Matcher mat1 = matcher_bili_range1.matcher(httpRequest);
                Matcher mat2 = matcher_bili_range2.matcher(httpRequest);

                //System.out.println(httpRequest);
                //System.out.println(mat1.find());
                //System.out.println(mat2.find());

                if (mat1.find()){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Referer", Referer)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            // Range bytes=0-1227
                            .headers("Range", "bytes=" + mat1.group(2) + "-" + mat1.group(3))
                            .build();
                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                    long rangeSize = -1;
                    if (send.headers().firstValue("content-range").isPresent()){
                        String s = send.headers().firstValue("content-range").get();
                        try {
                            rangeSize = Long.parseLong(s.split("/")[1]);
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }

                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 206, contentType, null, null, send.body(), null, Long.parseLong(mat1.group(3)), Long.parseLong(mat1.group(2)), rangeSize));
                } else {
                    int length = Integer.parseInt(send.headers().firstValue("content-length").isPresent() ? send.headers().firstValue("content-length").get() : "0");
                    int max = length / 10;
                    byte[][] temp = new byte[max][10];

                    for (int i = 0; i < 5; i++) {
                        int mi = max * i;
                        int mx = max * (i + 1);
                        //System.out.println(mi + " - " +Math.min(mx - 1, length - 1));
                        request = HttpRequest.newBuilder()
                                .uri(new URI(URL))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Referer", Referer)
                                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                // Range bytes=0-1227
                                .headers("Range", "bytes=" + mi + "-" + Math.min(mx - 1, length - 1))
                                .build();
                        send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                        //System.out.println(send.statusCode());
                        //out.write(send.body());
                        temp[i] = send.body();
                        if (send.statusCode() >= 300){
                            break;
                        }

                    }
                    //contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                    //System.out.println("o : "+contentEncoding);
                    byte[] data = Function.concatByteArrays(temp[0], temp[1], temp[2], temp[3], temp[4], temp[5], temp[6], temp[7], temp[8], temp[9]);
                    if (mat2.find()){
                        Function.sendHttpData(ch, new HttpHeader(httpVersion, 206, contentType, null, null, data, null, 0, data.length - 1, data.length));
                    } else {
                        Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, contentType, null, null, data, null));
                    }
                }
            }
            httpVersion = null;
            send = null;
            request = null;
            contentType = null;
            httpRequest = null;

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
    public void setHTTPSocket(AsynchronousSocketChannel ch) {
        this.ch = ch;
    }

    @Override
    public void setHTTPClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setProxy(String proxy) {

    }
}
