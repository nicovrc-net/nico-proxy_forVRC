package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.data.HttpHeader;

import java.net.URI;
import java.net.URLEncoder;
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
    private String url = null;
    private AsynchronousSocketChannel ch = null;
    private HttpClient client = null;
    private String proxy = null;
    private String httpHostname = "localhost:"+Function.config_httpPort;
    private String http = "https://";

    private final Pattern matcher_videoURI1 = Pattern.compile("/video/(.+)/(.+)\\.(m3u8|ts|cmfv|cmfa|key)");
    private final Pattern matcher_videoURI2 = Pattern.compile("/video/(.+)\\.(mp4)");
    private final Pattern matcher_dummyHLS = Pattern.compile("dummy=true");
    private final Pattern matcher_http_range = Pattern.compile("[r|R]ange: bytes=(\\d+)-(\\d+)");
    private final Pattern matcher_tver = Pattern.compile("https://steering-manifest\\.streaks\\.jp");
    private final Pattern matcher_HLSVideo = Pattern.compile("\\.cmfv");
    private final Pattern matcher_HLSAudio = Pattern.compile("\\.cmfa");
    private final Pattern matcher_hls_url = Pattern.compile("#EXTINF:(.+),\n(http|https)://(.+)\n");
    private final Pattern matcher_hls2_url = Pattern.compile("#EXT-X-MAP:URI=\"(.+)\"");

    @Override
    public void run() {
        if (client == null){
            return;
        }
        if (ch == null){
            return;
        }
        if (httpRequest == null){
            return;
        }
        httpHostname = Function.getHost(httpRequest);
        http = httpHostname.startsWith("localhost") ? "http://" : "https://";

        // 下準備
        final String httpVersion = Function.getHTTPVersion(httpRequest) != null ? Function.getHTTPVersion(httpRequest) : "1.1";

        final Matcher matcher_uri1 = matcher_videoURI1.matcher(httpRequest);
        final Matcher matcher_uri2 = matcher_videoURI2.matcher(httpRequest);
        final Matcher matcher_httpRange = matcher_http_range.matcher(httpRequest);

        final String cacherId;
        final String accessUrl;

        if (matcher_uri1.find()) {
            cacherId = matcher_uri1.group(1);
            String videoId = matcher_uri1.group(2);
            accessUrl = Function.getVideoIDListData(videoId).split("\"")[0];
        } else if (matcher_uri2.find()) {
            cacherId = matcher_uri2.group(1);

            CacheData cache = Function.getCache(Function.getCacheIDDataListData(cacherId));
            accessUrl = cache.getOriginURL();

        } else {
            cacherId = null;
            accessUrl = null;
        }

        //System.out.println("cacherId: " + cacherId);
        //System.out.println("accessUrl: " + accessUrl);

        long tempLong1 = -1;
        long tempLong2 = -1;

        if (matcher_httpRange.find()){
            String start = matcher_httpRange.group(1);
            String end = matcher_httpRange.group(2);

            if (!start.equals("0")){
                tempLong1 = Long.parseLong(start);
                tempLong2 = Long.parseLong(end);
            }
        }

        CacheData cache = Function.getCache(Function.getCacheIDDataListData(cacherId));

        if (cache == null){
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_others, null));
            return;
        }

        if (accessUrl == null){
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_others, null));
            return;
        }

        final String cookieText = cache != null ? cache.getCookieText() : "";
        final String refererText = cache != null ? cache.getRefererText() : "";
        final Long rangeStart = tempLong1 == -1 ? null :  tempLong1;
        final Long rangeEnd = tempLong2 == -1 ? null : tempLong2;
        final Long rangeLength = cache != null ? cache.getRangeLength() : null;

        //System.out.println("cookieText:"+cookieText);
        //System.out.println("refererText:"+refererText);
        //System.out.println("accessUrl:" +  accessUrl);

        // 動画ファイル取得
        try {
            final HttpRequest request;
            final boolean isRange = rangeLength != null && rangeEnd != null && rangeEnd.equals(rangeLength - 1);
            if (cookieText != null && refererText != null) {
                if (isRange){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Cookie", cookieText)
                            .headers("Referer", refererText)
                            .headers("Range", "bytes=" + rangeStart + "-" + rangeEnd)
                            .GET()
                            .build();
                } else {
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Cookie", cookieText)
                            .headers("Referer", refererText)
                            .GET()
                            .build();
                }
            } else if (cookieText != null) {
                if (isRange){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Cookie", cookieText)
                            .headers("Range", "bytes=" + rangeStart + "-" + rangeEnd)
                            .GET()
                            .build();
                } else {
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Cookie", cookieText)
                            .GET()
                            .build();
                }
            } else if (refererText != null) {
                if (isRange){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Referer", refererText)
                            .headers("Range", "bytes=" + rangeStart + "-" + rangeEnd)
                            .GET()
                            .build();
                } else {
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Referer", refererText)
                            .GET()
                            .build();
                }
            } else {
                if (isRange){
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Range", "bytes=" + rangeStart + "-" + rangeEnd)
                            .GET()
                            .build();
                } else {
                    request = HttpRequest.newBuilder()
                            .uri(new URI(accessUrl))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .GET()
                            .build();
                }
            }

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("Content-Type").isPresent() ? response.headers().firstValue("Content-Type").get() : response.headers().firstValue("content-type").isPresent() ? response.headers().firstValue("content-type").get() : "";

            if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                byte[] hls = Function.replaceHLS(response.body(), http, httpHostname, cacherId, request.uri().getHost(), url);
                String hls_text = new String(hls, StandardCharsets.UTF_8);

                Matcher matcher = Function.matcher_niconico.matcher(request.uri().getHost());
                Matcher matcher2 = matcher_dummyHLS.matcher(httpRequest);
                Matcher matcher3 = Function.matcher_abema.matcher(cache.getOriginURL());
                Matcher matcher4 = Function.matcher_AVProMobile.matcher(httpRequest);
                Matcher matcher5 = Function.matcher_hls_tver.matcher(httpRequest);
                Matcher matcher6 = Function.matcher_vrcLinux.matcher(httpRequest);
                Matcher matcher7 = matcher_tver.matcher(hls_text);
                Matcher matcher8 = matcher_HLSVideo.matcher(hls_text);
                Matcher matcher9 = matcher_HLSAudio.matcher(hls_text);


                if (matcher5.find() && accessUrl.equals(cache.getOriginURL())) {
                    // TVer
                    //System.out.println(new String(hls, StandardCharsets.UTF_8));
                    String s = new String(hls, StandardCharsets.UTF_8);
                    StringBuilder sb = new StringBuilder();

                    int i = 0;
                    String[] split = s.split("\n");
                    for (String string : split) {
                        if (string.startsWith("#EXT-X-CONTENT-STEERING")) {
                            i++;
                            continue;
                        }
                        if (string.startsWith("#EXT-X-MEDIA:TYPE=SUBTITLES")) {
                            i++;
                            continue;
                        }
                        if (string.startsWith("#EXT-X-STREAM-INF:")) {
                            sb.append(string).append("\n");
                            sb.append(split[i+1]).append("\n");
                            i++;
                            break;
                        }

                        sb.append(string).append("\n");
                    }

                    //System.out.println("----");
                    //System.out.println(sb.toString());
                    //System.out.println("----");
                    hls = sb.toString().getBytes(StandardCharsets.UTF_8);

                }

                if (matcher4.find() || matcher7.find()) {
                    hls_text = hls_text.replaceAll(",mp4a\\.40\\.2", "");
                    hls = hls_text.getBytes(StandardCharsets.UTF_8);
                }

                boolean isNico = matcher.find();
                if (isNico) {
                    // VRC かつ ニコ動などは選択できる最高画質/音質のみにする
                    //System.out.println(new String(hls, StandardCharsets.UTF_8));
                    //System.out.println("CacheID : " + cacherId);
                    //System.out.println("Access : " + accessUrl);
                    hls = Function.recreateHLS(new String(hls, StandardCharsets.UTF_8), matcher4.find(), matcher6.find()).getBytes(StandardCharsets.UTF_8);
                    //System.out.println(new String(hls, StandardCharsets.UTF_8));
                } else if (matcher3.find()) {
                    // AbemaはHLSの再処理が必要
                    hls = Function.fixAbemaHLS(new String(hls, StandardCharsets.UTF_8), cache.getOriginURL(), http, httpHostname, cacherId).getBytes(StandardCharsets.UTF_8);
                }
/*
                if (isNico && matcher8.find()) {
                    String text = Function.getFileByText("./cm-video.txt", StandardCharsets.UTF_8);
                    //System.out.println(text);
                    Matcher m = matcher_hls_url.matcher(text != null ? text : "");
                    Matcher m2 = matcher_hls2_url.matcher(text != null ? text : "");
                    if (m.find()) {
                        String url = m.group(2)+"://"+m.group(3);
                        String videoId = Function.getVideoID(url);
                        Function.addVideoIDList(videoId, url);
                        text = (text != null ? text : "").replaceAll(url, http+httpHostname+"/video/"+ URLEncoder.encode(cacherId, StandardCharsets.UTF_8)+"/"+videoId+".cmfv");
                    }
                    if (m2.find()) {
                        String url = m2.group(1);
                        //System.out.println(url);
                        String videoId = Function.getVideoID(url);
                        Function.addVideoIDList(videoId, url);
                        text = (text != null ? text : "").replaceAll(url, http+httpHostname+"/video/"+ URLEncoder.encode(cacherId, StandardCharsets.UTF_8)+"/"+videoId+".cmfv");
                    }

                    if (Function.matcher_VLC.matcher(httpRequest).find() || Function.matcher_AVPro.matcher(httpRequest).find()) {
                        hls_text = hls_text.replaceAll("#EXT-X-ENDLIST", "#EXT-X-DISCONTINUITY\n" + text + "#EXT-X-ENDLIST");
                    } else {
                        hls_text = hls_text.replaceAll("#EXT-X-ENDLIST", text + "#EXT-X-ENDLIST");
                    }
                    hls = hls_text.getBytes(StandardCharsets.UTF_8);
                }
                if (isNico && matcher9.find()) {
                    String text = Function.getFileByText("./cm-audio.txt", StandardCharsets.UTF_8);
                    Matcher m = matcher_hls_url.matcher(text != null ? text : "");
                    Matcher m2 = matcher_hls2_url.matcher(text != null ? text : "");
                    if (m.find()) {
                        String url = m.group(2)+"://"+m.group(3);
                        String videoId = Function.getVideoID(url);
                        Function.addVideoIDList(videoId, url);
                        text = (text != null ? text : "").replaceAll(url, http+httpHostname+"/video/"+ URLEncoder.encode(cacherId, StandardCharsets.UTF_8)+"/"+videoId+".cmfa");
                    }
                    if (m2.find()) {
                        String url = m2.group(1);
                        //System.out.println(url);
                        String videoId = Function.getVideoID(url);
                        Function.addVideoIDList(videoId, url);
                        text = (text != null ? text : "").replaceAll(url, http+httpHostname+"/video/"+ URLEncoder.encode(cacherId, StandardCharsets.UTF_8)+"/"+videoId+".cmfa");
                    }

                    if (Function.matcher_VLC.matcher(httpRequest).find() || Function.matcher_AVPro.matcher(httpRequest).find()) {
                        hls_text = hls_text.replaceAll("#EXT-X-ENDLIST", "#EXT-X-DISCONTINUITY\n" + text + "#EXT-X-ENDLIST");
                    } else {
                        hls_text = hls_text.replaceAll("#EXT-X-ENDLIST", text + "#EXT-X-ENDLIST");
                    }

                    hls = hls_text.getBytes(StandardCharsets.UTF_8);
                } */

                Function.sendHttpData(ch, new HttpHeader(httpVersion, response.statusCode(), contentType, null, null, hls, null));
                return;

            } else {
                if (isRange){
                    long rangeSize = -1;
                    String rangeText = "0-0/0";
                    if (response.headers().firstValue("content-range").isPresent()){
                        rangeText = response.headers().firstValue("content-range").get();
                        try {
                            rangeSize = Long.parseLong(rangeText.split("/")[1]);

                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }
                    String[] split = rangeText.split("/");
                    String[] split1 = split[0].split("-");

                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 206, contentType, null, null, response.body(), null, Long.parseLong(split1[0]), Long.parseLong(split1[1]), rangeSize));
                    return;
                }
            }

            Function.sendHttpData(ch, new HttpHeader(httpVersion, response.statusCode(), contentType, null, null, response.body(), null));
            return;

        } catch (Exception e){
            e.printStackTrace();
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_video_mp4, null, null, Function.getErrorMessageVideo(client, e.getMessage()), null));
            return;
        }

        //Function.sendHttpData(ch, new HttpHeader(httpVersion, 404, Function.contentType_textPlain, null, null, "???".getBytes(StandardCharsets.UTF_8), null));
    }

    @Override
    public String getStartURI() {
        return "/video";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public void setURL(String URL) {
        this.url = URL;
    }

    @Override
    public void setHTTPSocket(AsynchronousSocketChannel sock) {
        this.ch = sock;
    }

    @Override
    public void setHTTPClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }
}
