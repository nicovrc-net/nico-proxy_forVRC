package net.nicovrc.dev.http;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Service.Result.*;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.data.HttpHeader;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;
import net.nicovrc.dev.data.VideoData;
import net.nicovrc.dev.data.WebhookData;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetURL implements Runnable, NicoVRCHTTP {

    private AsynchronousSocketChannel ch = null;
    private String URL = null;
    private String httpRequest = null;
    private HttpClient client = null;
    private String proxy = null;
    private String httpHostname = "localhost:"+Function.config_httpPort;
    private String http = "https://";

    private final Pattern matcher_dummyPrintParameter = Pattern.compile("dummy=true");
    private final Pattern matcher_VRCStringUA = Pattern.compile("UnityPlayer/(.+) \\(UnityWebRequest/(.+), libcurl/(.+)\\)");

    private final Pattern matcher_browser = Pattern.compile("(([fF])irefox|([oO])pera|([sS])ec-([cC])h-([uU])a)");


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

        URL = URL.replaceAll("/\\?url=", "").replaceAll("&url=", "").replaceAll("/dummy\\.m3u8", "");

        final String httpVersion = Function.getHTTPVersion(httpRequest) != null ? Function.getHTTPVersion(httpRequest) : "1.1";
        final boolean isDummyPrint = matcher_dummyPrintParameter.matcher(httpRequest).find();
        final boolean isVLC = Function.matcher_VLC.matcher(httpRequest).find();
        final boolean isAVPro = Function.matcher_AVPro.matcher(httpRequest).find();
        final boolean isFFmpeg = Function.matcher_FFMpeg.matcher(httpRequest).find();
        final boolean isTitle = matcher_VRCStringUA.matcher(httpRequest).find();

        while (Function.CacheWaitList.get(URL) != null){
            try {
                Thread.sleep(100L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final CacheData cache = Function.getCache(URL);


        // キャッシュ有無
        if (cache != null){
            // キャッシュあり
            OutputVideoData(true, isDummyPrint, isTitle, cache, httpVersion, isVLC, isFFmpeg, isAVPro);
            return;
        }

        // キャッシュなし
        Function.CacheWaitList.put(URL, new Date());

        ServiceAPI service = null;
        //System.out.println(URL);
        for (ServiceAPI api : ServiceList.getServiceList()) {
            for (String s : api.getCorrespondingURL()) {
                Pattern compile = Pattern.compile(s.replaceAll("\\.", "\\.").replaceAll("\\*", ".*"));
                //System.out.println(s);

                if (URL.startsWith("http://"+s) ||  URL.startsWith("https://"+s) || (!URL.startsWith("http") && URL.startsWith(s) && api.getServiceName().equals("ニコニコ"))) {
                    service = api;
                    break;
                }

                if (URL.startsWith("http") && compile.matcher(URL).find() && !api.getServiceName().equals("ニコニコ")){
                    service = api;
                    break;
                }
            }

            if (service != null){
                break;
            }
        }

        if (service == null){
            // 対応していないサイト
            Function.CacheWaitList.remove(URL);
            Thread.ofVirtual().start(()->{
                PrintLog(URL, "対応していないサイト", false);
                AddLog(URL, "対応していないサイト", false);
                AddWebhook(URL, "対応していないサイト");
            });
            SendErrorData(isTitle, httpVersion, "対応していないサイト");
            return;
        }

        service.setProxy(proxy);
        service.setURL(URL);
        service.setHttpClient(client);

        if (service.getServiceName().equals("ツイキャス")){
            service.setToken(new String[]{Function.config_twitcast_ClientId, Function.config_twitcast_ClientSecret});
        } else if (service.getServiceName().equals("ニコニコ")){
            service.setToken(new String[]{Function.config_user_session, Function.config_nicosid});
        }

        final CacheData data = new CacheData();
        data.setProxy(proxy);
        data.setCacheDate(new Date().getTime());

        final String jsonText = service.get();
        final JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);
        if (json != null && json.getAsJsonObject().has("ErrorMessage")){
            // エラーの場合
            Function.CacheWaitList.remove(URL);
            String errorMessage = json.getAsJsonObject().get("ErrorMessage").getAsString();
            Thread.ofVirtual().start(()->{
                PrintLog(URL, "エラー: "+errorMessage, false);
                AddLog(URL, "エラー: "+errorMessage, false);
                AddWebhook(URL, "エラー: "+errorMessage);
            });

            SendErrorData(isTitle, httpVersion, "エラー: "+errorMessage);

            return;
        }
        // エラーなし
        String originUrl = "";
        String cookieText = null;
        String refererText = null;
        String title = "タイトルなし";

        // 生URL
        if (json != null && json.getAsJsonObject().has("VideoURL")){
            originUrl = json.getAsJsonObject().get("VideoURL").getAsString();
        } else if (json != null && json.getAsJsonObject().has("LiveURL")){
            originUrl = json.getAsJsonObject().get("LiveURL").getAsString();
        } else if (json != null && json.getAsJsonObject().has("AudioURL")){
            originUrl = json.getAsJsonObject().get("AudioURL").getAsString();
        }
        if (json != null && json.getAsJsonObject().has("VideoHLSURL")){
            originUrl = json.getAsJsonObject().get("VideoHLSURL").getAsString();
        }

        // タイトル
        if (json != null && json.getAsJsonObject().has("Title")){
            title = json.getAsJsonObject().get("Title").getAsString();
        } else if (json != null && json.getAsJsonObject().has("TweetText")){
            title = json.getAsJsonObject().get("TweetText").getAsString();
        }

        // Cookie
        if (service.getServiceName().equals("bilibili.com")){
            bilibiliResult temp = Function.gson.fromJson(json, bilibiliResult.class);
            StringBuilder sb = new StringBuilder();
            if (temp != null){
                temp.getVideoAccessCookie().forEach((key, value) -> {
                    sb.append(key).append("=").append(value).append(";");
                });
                cookieText = sb.substring(0, sb.length() - 1);
            }
            refererText = URL;
        } else if (service.getServiceName().equals("ニコニコ")){
            NicoVideoResult temp = Function.gson.fromJson(json, NicoVideoResult.class);
            StringBuilder sb = new StringBuilder();
            if (temp != null && temp.getVideoAccessCookie() != null){
                temp.getVideoAccessCookie().forEach((key, value) -> {
                    sb.append(key).append("=").append(value).append(";");
                });
                cookieText = sb.substring(0, sb.length() - 1);
            } else if (temp != null && temp.getLiveAccessCookie() != null){
                temp.getLiveAccessCookie().forEach((key, value) -> {
                    sb.append(key).append("=").append(value).append(";");
                });
                cookieText = sb.substring(0, sb.length() - 1);
            }
        } else if (service.getServiceName().equals("TikTok")) {
            TikTokResult temp = Function.gson.fromJson(json, TikTokResult.class);
            cookieText = temp != null ? temp.getVideoAccessCookie() : null;
        }


        // 出力用データ生成
        byte[] videoData = null;
        try {
            videoData = getHLSData(originUrl, cookieText, refererText, data.getCacheId());
            if (videoData != null) {
                data.setHLS(videoData);
                data.setContentType(Function.contentType_hls);
            } else {
                VideoData video = getVideoData(originUrl, cookieText, refererText);
                if (video == null) {
                    Thread.ofVirtual().start(()->{
                        Function.CacheWaitList.remove(URL);
                        PrintLog(URL, "エラー: 動画ファイル取得失敗", false);
                        AddLog(URL, "エラー: 動画ファイル取得失敗", false);
                        AddWebhook(URL, "エラー: 動画ファイル取得失敗");
                    });
                    SendErrorData(isTitle, httpVersion, "内部エラー");
                    return;
                }
                data.setRange(video.isRange());
                data.setRangeStart(video.getStartRange());
                data.setRangeEnd(video.getEndRange());
                data.setData(video.isRange() && video.getEndRange() != (video.getLength() - 1) ? null : (video.isRange() && video.getEndRange() == (video.getLength() - 1) ? video.getVideoData() : null));
                String tempUrl = http+httpHostname+"/video/?cacheId="+URLEncoder.encode(data.getCacheId(), StandardCharsets.UTF_8)+"&url="+URLEncoder.encode(originUrl, StandardCharsets.UTF_8);
                data.setRedirectURL(video.isRange() && video.getEndRange() != (video.getLength() - 1) ? tempUrl : (video.isRange() && video.getEndRange() == (video.getLength() - 1) ? null : tempUrl));
            }
            data.setURL(URL);
            data.setOriginURL(originUrl.isEmpty() ? null : originUrl);
            data.setCookieText(cookieText);
            data.setRefererText(refererText);
            data.setTitle(title);
        } catch (Exception e){

            Thread.ofVirtual().start(()->{
                PrintLog(URL, "エラー: "+e.getMessage(), false);
                AddLog(URL, "エラー: "+e.getMessage(), false);
                AddWebhook(URL, "エラー: "+e.getMessage());
            });

            SendErrorData(isTitle, httpVersion, "内部エラー");
        }

        // 送信
        OutputVideoData(false, isDummyPrint, isTitle, data, httpVersion, isVLC, isFFmpeg, isAVPro);
        Function.addCache(URL, data);
        Function.CacheWaitList.remove(URL);

    }

    @Override
    public String getStartURI() {
        return "/?url=";
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
        this.proxy = proxy;
    }

    private void SendErrorData(boolean isTitle, String httpVersion, String str) {
        try {

            if (str.equals("内部エラー")){
                if (isTitle){
                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_textPlain, null, null, str.getBytes(StandardCharsets.UTF_8), null));
                } else {
                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_others, null));
                }
                return;
            }

            if (str.startsWith("エラー:")){
                if (isTitle){
                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_textPlain, null, null, str.getBytes(StandardCharsets.UTF_8), null));
                } else {
                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_video_mp4, null, null, Function.getErrorMessageVideo(client, str.replaceFirst("エラー: ", "")), null));
                }
                return;
            }

            if (str.equals("対応していないサイト")){
                if (isTitle){
                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_textPlain, null, null, str.getBytes(StandardCharsets.UTF_8), null));
                } else {
                    Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_site, null));
                }
            }

        } catch (Exception e){
            if (isTitle){
                Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_textPlain, null, null, "内部エラー".getBytes(StandardCharsets.UTF_8), null));
            } else {
                Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_others, null));
            }
        }
    }

    private void PrintLog(String fromUrl ,String toUrl, boolean isCache){
        if (isCache){
            Thread.ofVirtual().start(() -> System.out.println("[Get URL (キャッシュ," + Function.sdf.format(new Date()) + ")] " + fromUrl + " ---> " + toUrl));
        } else {
            Thread.ofVirtual().start(() -> System.out.println("[Get URL (" + Function.sdf.format(new Date()) + ")] " + fromUrl + " ---> " + toUrl));
        }
    }

    private void AddWebhook(String fromUrl ,String toUrl){
        Thread.ofVirtual().start(() -> {
            WebhookData webhookData = new WebhookData();
            webhookData.setHTTPRequest(httpRequest);
            webhookData.setDate(new Date());
            webhookData.setURL(fromUrl);
            webhookData.setResult(toUrl);

            Function.WebhookData.put(UUID.randomUUID().toString(), webhookData);
        });
    }

    private void AddLog(String fromUrl ,String toUrl, boolean isCache){
        Thread.ofVirtual().start(() -> {
            WebhookData webhookData = new WebhookData();
            webhookData.setHTTPRequest(httpRequest);
            webhookData.setDate(new Date());
            webhookData.setURL(fromUrl);
            webhookData.setResult(toUrl);

            Function.WebhookData.put(UUID.randomUUID().toString(), webhookData);
        });
    }

    private void OutputVideoData(boolean isCache, boolean isDummyPrint, boolean isTitle, CacheData cache, String httpVersion, boolean isVLC, boolean isFFmpeg, boolean isAVPro){
        if (!isDummyPrint){
            if (isTitle) {
                PrintLog(URL, cache.getTitle() != null ? cache.getTitle() : "タイトルなし", isCache);
                AddLog(URL, cache.getTitle() != null ? cache.getTitle() : "タイトルなし", isCache);
                AddWebhook(URL, cache.getTitle() != null ? cache.getTitle() : (cache.getOriginURL() != null ? cache.getOriginURL() : null));
            } else {
                PrintLog(URL, cache.getOriginURL() != null ? cache.getOriginURL() : "", isCache);
                AddLog(URL, cache.getOriginURL() != null ? cache.getOriginURL() : "", isCache);
            }
        }

        if (isTitle) {
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, Function.contentType_textPlain, null, null, cache.getTitle().getBytes(StandardCharsets.UTF_8), null));
            return;
        }

        if (cache.isRedirect()){
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 302, null, null, null, null, cache.getRedirectURL()));
            return;
        }

        if (cache.isRange()){
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 206, cache.getContentType(), null, null, cache.getData(), null, 0, cache.getRangeEnd(), cache.getRangeLength()));
            return;
        }

        if (!cache.isHLS()){
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, cache.getContentType(), null, null, cache.getData(), null));
            return;
        }

        final byte[] hls_bytes = cache.getHLS();

        if (isDummyPrint && !isVLC && !isFFmpeg && !isAVPro){
            Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, cache.getContentType(), null, null, createDummyHLS(hls_bytes), null));
            return;
        }

        Function.sendHttpData(ch, new HttpHeader(httpVersion, 200, cache.getContentType(), null, null, hls_bytes, null));
    }

    private byte[] createDummyHLS(byte[] hls){
        Matcher matcher = matcher_browser.matcher(httpRequest);
        if (matcher.find()){
            return hls;
        }
        return Function.zeroByte;
    }


    private final Pattern matcher_hls_twitcasting = Pattern.compile("twitcasting\\.tv");
    private final Pattern matcher_hls_abema = Pattern.compile("(.+)-abematv\\.akamaized\\.net");
    private final Pattern matcher_hls_vimeo = Pattern.compile("vimeocdn\\.com");
    private final Pattern matcher_hls_fc2Live = Pattern.compile("(.+)\\.live\\.fc2\\.com");

    private final Pattern matcher_video_tiktok = Pattern.compile("tiktok\\.com");
    private final Pattern matcher_video_bilicom = Pattern.compile("bilibili\\.com");

    private byte[] getHLSData(String url, String cookieText, String refererText, String cacheId) throws Exception {
        byte[] hls_data = null;
        final Matcher hls_twitcas = matcher_hls_twitcasting.matcher(url);
        final Matcher hls_abema = matcher_hls_abema.matcher(url);
        final Matcher hls_vimeo = matcher_hls_vimeo.matcher(url);
        final Matcher hls_fc2Live = matcher_hls_fc2Live.matcher(url);

        final HttpRequest request;
        if (cookieText == null && refererText == null) {
            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();
        } else if (cookieText != null && refererText == null) {
            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Cookie", cookieText)
                    .GET()
                    .build();
        } else if (cookieText == null) {
            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Referer", refererText)
                    .GET()
                    .build();
        } else if (hls_fc2Live.find()) {
            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
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
            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Cookie", cookieText)
                    .headers("Referer", refererText)
                    .GET()
                    .build();
        }

        final HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 && response.statusCode() >= 300){
            return hls_data;
        }

        final String contentType = response.headers().firstValue("Content-Type").isPresent() ? response.headers().firstValue("Content-Type").get() : response.headers().firstValue("content-type").isPresent() ? response.headers().firstValue("content-type").get() : "";
        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")){
            hls_data = response.body();

            final String hlsText = new String(hls_data, StandardCharsets.UTF_8);

            StringBuffer sb = new StringBuffer();
            for (String line : hlsText.split("\n")){
                if (line.startsWith("http")){
                    sb.append(http).append(httpHostname).append("/video/?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(line, StandardCharsets.UTF_8)).append("\n");
                    continue;
                }

                if (line.startsWith("/")){
                    String hlsUrl = "https://"+request.uri().getHost()+line;

                    if (hls_twitcas.find() && line.startsWith("/tc\\.vod\\.v2")){
                        sb.append(http).append(httpHostname).append("/video/?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(hlsUrl, StandardCharsets.UTF_8)).append("\n");
                        continue;
                    }

                    if (hls_abema.find()){
                        if (line.startsWith("/tsad")){
                            sb.append(http).append(httpHostname).append("/video/?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(hlsUrl, StandardCharsets.UTF_8)).append("\n");
                            continue;
                        }
                        if (line.startsWith("/preview")) {
                            sb.append(http).append(httpHostname).append("/video/?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(hlsUrl, StandardCharsets.UTF_8)).append("\n");
                            continue;
                        }
                    }

                }

                if (hls_vimeo.find()){
                    StringBuffer tempHost = new StringBuffer();
                    String[] split = url.split("/");
                    for (int i = 0; i < split.length - 6; i++) {
                        tempHost.append(split[i]).append("/");
                    }
                    line = line.replaceAll("\\.\\./\\.\\./\\.\\./\\.\\./\\.\\./", tempHost.toString());
                    sb.append(http).append(httpHostname).append("/video/?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(line, StandardCharsets.UTF_8)).append("\n");
                    continue;
                }

                sb.append(line).append("\n");
            }

            hls_data = sb.toString().getBytes(StandardCharsets.UTF_8);
        }
        return hls_data;
    }

    private final Pattern matcher_http_range1 = Pattern.compile("[r|R]ange: bytes=(\\d+)-(\\d+)");
    private final Pattern matcher_http_range2 = Pattern.compile("[r|R]ange: bytes=(\\d+)-");

    private VideoData getVideoData(String url, String cookieText, String refererText) throws Exception {

        final Matcher matcher_tiktok = matcher_video_tiktok.matcher(url);
        final Matcher matcher_bilibili_com = matcher_video_bilicom.matcher(refererText !=  null ? refererText : "");

        final VideoData videoData = new VideoData();
        videoData.setRange(false);

        if (matcher_tiktok.find()){
            url = url.replaceAll("\\|", "%7C");
        }

        if (!matcher_bilibili_com.find()){
            final HttpRequest request;

            if (cookieText == null && refererText == null) {
                request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .GET()
                        .build();
            } else if (cookieText != null && refererText == null) {
                request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Cookie", cookieText)
                        .GET()
                        .build();
            } else {
                request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Referer", refererText)
                        .GET()
                        .build();
            }

            final HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300){
                videoData.setVideoData(response.body());
                return videoData;
            }
            return null;
        }

        // bilibiliの場合だけHTTP分割ダウンロードに対応する
        final Matcher range1 = matcher_http_range1.matcher(httpRequest);
        final Matcher range2 = matcher_http_range2.matcher(httpRequest);

        final HttpRequest request;

        if (range1.find()){
            videoData.setRange(true);
            videoData.setStartRange(Long.parseLong(range1.group(2)));
            videoData.setEndRange(Long.parseLong(range1.group(3)));

            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Referer", refererText)
                    .headers("Range", "bytes=" + videoData.getStartRange() + "-" + videoData.getEndRange())
                    .GET()
                    .build();

            final HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            long rangeSize = -1;
            if (response.headers().firstValue("content-range").isPresent()){
                String s = response.headers().firstValue("content-range").get();
                try {
                    rangeSize = Long.parseLong(s.split("/")[1]);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            videoData.setLength(rangeSize);

            return videoData;

        } else if (range2.find()){
            videoData.setRange(true);
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Referer", refererText)
                    .HEAD()
                    .build();

            HttpResponse<byte[]> response = client.send(request2, HttpResponse.BodyHandlers.ofByteArray());

            final int length = Integer.parseInt(response.headers().firstValue("content-length").isPresent() ? response.headers().firstValue("content-length").get() : "0");
            final int max = length / 10;
            final byte[][] temp = new byte[max][10];

            for (int i = 0; i < temp.length; i++) {
                int mi = max * i;
                int mx = max * (i + 1);
                //System.out.println(mi + " - " +Math.min(mx - 1, length - 1));
                request2 = HttpRequest.newBuilder()
                        .uri(new URI(URL))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Referer", refererText)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        // Range bytes=0-1227
                        .headers("Range", "bytes=" + mi + "-" + Math.min(mx - 1, length - 1))
                        .build();
                response = client.send(request2, HttpResponse.BodyHandlers.ofByteArray());

                //System.out.println(send.statusCode());
                //out.write(send.body());
                temp[i] = response.body();
                if (response.statusCode() >= 300){
                    break;
                }

            }
            videoData.setVideoData(Function.concatByteArrays(temp[0], temp[1], temp[2], temp[3], temp[4], temp[5], temp[6], temp[7], temp[8], temp[9]));
            videoData.setLength(videoData.getVideoData().length);
            videoData.setStartRange(0);
            videoData.setEndRange(videoData.getLength() - 1);

            return videoData;

        } else {
            return null;
        }
    }

}
