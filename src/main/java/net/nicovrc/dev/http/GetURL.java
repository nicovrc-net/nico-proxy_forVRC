package net.nicovrc.dev.http;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.data.LogData;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;
import net.nicovrc.dev.data.WebhookData;
import net.nicovrc.dev.http.getContent.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetURL implements Runnable, NicoVRCHTTP {

    private Socket sock = null;
    private String URL = null;
    private String httpRequest = null;
    private final HashMap<String, GetContent> GetContentList = new HashMap<>();

    private final Gson gson = Function.gson;
    private final List<ServiceAPI> list = ServiceList.getServiceList();

    private final Pattern NotRemoveQuestionMarkURL = Pattern.compile("(youtu\\.be|www\\.youtube\\.com|(.+)\\.pornhub\\.com)");

    private final Pattern pattern_Asterisk = Pattern.compile("\\*");

    private final Pattern dummy_url = Pattern.compile("&dummy=true");
    private final Pattern dummy_url2 = Pattern.compile("^/(\\?dummy=true&url=|dummy\\.m3u8\\?dummy=true&url=|proxy/\\?dummy=true&|\\?dummy=true&vi=)(.+)");
    private final Pattern vrc_getStringUA = Pattern.compile("UnityPlayer/(.+) \\(UnityWebRequest/(.+), libcurl/(.+)\\)");

    private final Pattern vlc_ua = Pattern.compile("(VLC/(.+) LibVLC/(.+)|LibVLC)");

    private byte[] errContent000;
    private byte[] errContent404;

    public GetURL(){

        GetContentList.put("ニコニコ", new NicoVideo());
        GetContentList.put("ツイキャス", new Twitcast());
        GetContentList.put("Abema", new Abema());
        GetContentList.put("TikTok", new TikTok());
        GetContentList.put("OPENREC", new OPENREC());
        GetContentList.put("TVer", new TVer());
        GetContentList.put("piapro", new AudioSite());
        GetContentList.put("SoundCloud", new AudioSite());
        GetContentList.put("Sonicbowl", new AudioSite());
        GetContentList.put("Mixcloud", new AudioSite());
        GetContentList.put("bandcamp", new AudioSite());
        GetContentList.put("Vimeo", new Vimeo());
        GetContentList.put("fc2", new fc2());
        GetContentList.put("XVIDEOS.COM", new XVIDEOS());
        GetContentList.put("Pornhub", new Pornhub());
        GetContentList.put("bilibili.com", new bilibili_com());
        GetContentList.put("Twitter", new Twitter());

        Function.tempCacheCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                HashMap<String, Long> temp = new HashMap<>(Function.tempCacheList);

                temp.forEach((url, time) ->{
                    if (new Date().getTime() - time >= 150000L){
                        Function.tempCacheList.remove(url);
                    }
                });

                temp.clear();
                temp = null;
            }
        }, 0L, 1000L);

        try {
            File file = new File("./error-video/error_000.mp4");
            if (file.exists()){
                FileInputStream stream = new FileInputStream(file);
                errContent000 = stream.readAllBytes();
                stream.close();
                stream = null;
            }
        } catch (Exception e){
            errContent000 = Function.zeroByte;
        }

        try {
            File file = new File("./error-video/error_404.mp4");
            if (file.exists()){
                FileInputStream stream = new FileInputStream(file);
                errContent404 = stream.readAllBytes();
                stream.close();
                stream = null;
            }
        } catch (Exception e){
            errContent404 = Function.zeroByte;
        }

    }

    @Override
    public void run() {
        final String method = Function.getMethod(httpRequest);
        final String httpVersion = Function.getHTTPVersion(httpRequest) != null ? Function.getHTTPVersion(httpRequest) : "1.1";
        final boolean isHead = method != null && method.equals("HEAD");

        final String contentType_video_mp4 = "video/mp4";
        final String contentType_hls = "application/vnd.apple.mpegurl";
        final String contentType_text = "text/plain; charset=utf-8";

        final String conent_encoding = Function.getContentEncoding(httpRequest);
        String sendContentEncoding = "";
        if (conent_encoding != null && conent_encoding.matches(".*br.*")){
            sendContentEncoding = "br";
        } else if (conent_encoding != null && conent_encoding.matches(".*gzip.*")){
            sendContentEncoding = "gzip";
        }

        try {
            //System.out.println(URL);

            Matcher matcher_m = dummy_url2.matcher(URL);
            if (matcher_m.find()){
                //System.out.println(URL);
                //URL = "/?url="+matcher_m.group(1)+"&dummy=true";
                URL = matcher_m.group(2) + "&dummy=true";
                //System.out.println(URL);
            }

            URL = URL.replaceAll("^(/(.*)\\?url=|/\\?vi=|/proxy/(.*)\\?)", "");
            final String targetUrl = (NotRemoveQuestionMarkURL.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", "");
            //System.out.println(targetUrl);

            ServiceAPI api = null;
            //System.out.println(targetUrl);
            CacheData cacheData = Function.getCache(targetUrl);

            String json = null;
            String ServiceName = null;
            String proxy = null;

            final boolean isCache = cacheData != null;
            final boolean isHLSDummyPrint = !dummy_url.matcher(URL).find();
            final boolean isGetTitle = vrc_getStringUA.matcher(httpRequest).find();
            final long currentTimeLong = new Date().getTime();

            //System.out.println(isCache);
            if (isCache) {
                cacheData = Function.getCache(targetUrl);
                int i = cacheData == null ? 1 : 0;
                while (i == 0){
                    cacheData = Function.getCache(targetUrl);
                    if (cacheData == null){
                        i = 1;
                        continue;
                    }
                    if (cacheData.getTargetURL() != null && !cacheData.getTargetURL().isEmpty()){
                        i = 1;
                        continue;
                    }
                }

                if (cacheData != null){
                    proxy = cacheData.getProxy() != null ? cacheData.getProxy() : null;
                    final String[] split = proxy != null ? proxy.split(":") : null;
                    final int splitLength = split != null ? split.length : 0;

                    if (Function.tempCacheList.get(targetUrl) == null){
                        if (currentTimeLong - cacheData.getCacheDate() <= 1800000L){
                            Function.tempCacheList.put(targetUrl, currentTimeLong);
                        } else {
                            String targetURL = cacheData.getTargetURL();
                            String cookieText = cacheData.getCookieText();
                            String refererText = cacheData.getRefererText();
                            boolean isFound = true;

                            try (HttpClient client = proxy == null || splitLength != 2 ? HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NORMAL)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .build()
                                    :
                                    HttpClient.newBuilder()
                                            .version(HttpClient.Version.HTTP_2)
                                            .followRedirects(HttpClient.Redirect.NORMAL)
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .proxy(ProxySelector.of(new InetSocketAddress(split[0], Integer.parseInt(split[1]))))
                                            .build()
                            ) {

                                HttpRequest request = null;
                                if (cookieText == null && refererText == null){
                                    request = HttpRequest.newBuilder()
                                            .uri(new URI(targetURL))
                                            .headers("User-Agent", Function.UserAgent)
                                            .GET()
                                            .build();
                                } else if (cookieText != null && refererText == null){
                                    request = HttpRequest.newBuilder()
                                            .uri(new URI(targetURL))
                                            .headers("User-Agent", Function.UserAgent)
                                            .headers("Cookie", cookieText)
                                            .GET()
                                            .build();
                                } else if (cookieText == null){
                                    request = HttpRequest.newBuilder()
                                            .uri(new URI(targetURL))
                                            .headers("User-Agent", Function.UserAgent)
                                            .headers("Referer", refererText)
                                            .GET()
                                            .build();
                                } else {
                                    request = HttpRequest.newBuilder()
                                            .uri(new URI(targetURL))
                                            .headers("User-Agent", Function.UserAgent)
                                            .headers("Cookie", cookieText)
                                            .headers("Referer", refererText)
                                            .GET()
                                            .build();
                                }

                                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                                if (send.statusCode() >= 300 || send.statusCode() <= 199){
                                    isFound = false;
                                }
                                send = null;

                            }

                            if (isFound){
                                Function.tempCacheList.put(targetUrl, currentTimeLong);
                            } else {
                                Function.deleteCache(targetUrl);
                                cacheData = null;
                            }
                        }
                    }
                }

                if (cacheData != null){

                    final CacheData fCacheData = cacheData;
                    if (isGetTitle){
                        Thread.ofVirtual().start(()-> System.out.println("[Get URL (キャッシュ," + Function.sdf.format(new Date()) + ")] " + URL + " ---> " + fCacheData.getTitle()));

                        byte[] bytes = Function.compressByte(cacheData.getTitle().getBytes(StandardCharsets.UTF_8), sendContentEncoding);

                        Function.sendHTTPRequest(sock, httpVersion, 200, contentType_text, sendContentEncoding, bytes == null ? cacheData.getTitle().getBytes(StandardCharsets.UTF_8) : bytes, isHead);
                    } else {
                        if (isHLSDummyPrint){
                            Thread.ofVirtual().start(() -> System.out.println("[Get URL (キャッシュ," + Function.sdf.format(new Date()) + ")] " + URL + " ---> " + fCacheData.getTargetURL()));
                        }

                        if (cacheData.isRedirect()){
                            OutputStream out = sock.getOutputStream();
                            StringBuilder sb_header = new StringBuilder();

                            sb_header.append("HTTP/").append(httpVersion).append(" 302 Found\r\nDate: ").append(new Date()).append("\r\nLocation: ").append(cacheData.getTargetURL()).append("\r\n\r\n");
                            out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                            out.flush();

                            out = null;
                            sb_header.setLength(0);
                            sb_header = null;

                            return;
                        }

                        if (cacheData.isHLS()){
                            byte[] dummy_bytes = Function.compressByte(cacheData.getDummyHLS(), sendContentEncoding);
                            byte[] hls_bytes = Function.compressByte(cacheData.getHLS(), sendContentEncoding);

                            if (cacheData.getDummyHLS() != null){

                                if (isHLSDummyPrint && !vlc_ua.matcher(httpRequest).find()) {
                                    Function.sendHTTPRequest(sock, httpVersion, 200, contentType_hls, sendContentEncoding, dummy_bytes == null ? cacheData.getDummyHLS() : dummy_bytes, isHead);
                                } else {
                                    Function.sendHTTPRequest(sock, httpVersion, 200, contentType_hls, sendContentEncoding, hls_bytes == null ? cacheData.getHLS() : hls_bytes, isHead);
                                }
                            } else {
                                Function.sendHTTPRequest(sock, httpVersion, 200, contentType_hls, sendContentEncoding, hls_bytes == null ? cacheData.getHLS() : hls_bytes, isHead);
                            }
                        } else {
                            OutputStream out = sock.getOutputStream();
                            StringBuilder sb_header = new StringBuilder();

                            sb_header.append("HTTP/").append(httpVersion).append(" 302 Found\r\nDate: ").append(new Date()).append("\r\nLocation: /https/cookie:[").append(cacheData.getCookieText()).append("]/referer:[").append(cacheData.getRefererText()).append("]/").append(cacheData.getTargetURL().replaceAll("https://", "")).append("\r\n\r\n");
                            //System.out.println(sb_header);
                            out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                            out.flush();

                            out = null;
                            sb_header.setLength(0);
                            sb_header = null;
                        }
                    }

                    Thread.ofVirtual().start(()->{
                        Date date = new Date();

                        LogData logData = new LogData();
                        logData.setHTTPRequest(httpRequest);
                        logData.setRequestURL(URL);
                        logData.setUnixTime(date.getTime());

                        WebhookData webhookData = new WebhookData();
                        webhookData.setURL(URL);
                        webhookData.setHTTPRequest(httpRequest);
                        webhookData.setDate(date);

                        if (!isGetTitle){
                            logData.setResultURL(fCacheData.getTargetURL());
                            webhookData.setResult(fCacheData.getTargetURL());
                        } else {
                            logData.setResultURL(fCacheData.getTitle());
                            webhookData.setResult(fCacheData.getTitle());
                        }

                        if (isHLSDummyPrint){
                            Function.GetURLAccessLog.put(logData.getLogID(), logData);
                            Function.WebhookData.put(logData.getLogID(), webhookData);
                        }
                    });

                    return;
                }
            }

            for (ServiceAPI vrcapi : list) {
                boolean isFound = false;
                for (String str : vrcapi.getCorrespondingURL()) {

                    Pattern matcher_0 = null;
                    if (pattern_Asterisk.matcher(str).find()){
                        //System.out.println(str.replaceAll("\\.", "\\\\.").replaceAll("\\*", "(.+)"));
                        matcher_0 = Pattern.compile(str.replaceAll("\\.", "\\\\.").replaceAll("\\*", "(.+)"));
                    }

                    if (URL.startsWith("https://"+str) || URL.startsWith("http://"+str) || URL.startsWith(str) || (matcher_0 != null && matcher_0.matcher(URL).find())){
                        //System.out.println(str);
                        if ((str.equals("so") || str.equals("jp")) && URL.startsWith("http")){
                            continue;
                        }

                        api = vrcapi;
                        isFound = true;
                    }
                }

                if (isFound){
                    break;
                }
            }

            if (api != null) {
                //System.out.println("aaa");
                //System.out.println(URL.startsWith("https://twitcasting.tv"));
                ServiceName = api.getServiceName();
                if (ServiceName.equals("ニコニコ")) {

                    if (Function.config_user_session != null && Function.config_user_session_secure != null && Function.config_nicosid != null) {
                        api.Set("{\"URL\":\"" + URL.split("\\?")[0].replaceAll("&dummy=true", "") + "\", \"user_session\":\"" + Function.config_user_session + "\", \"user_session_secure\":\"" + Function.config_user_session_secure + "\", \"nicosid\": \"" + Function.config_nicosid + "\"}");
                    } else {
                        api.Set("{\"URL\":\"" + URL.split("\\?")[0].replaceAll("&dummy=true", "") + "\"}");
                    }

                } else if (URL.startsWith("https://twitcasting.tv")) {
                    if (NotRemoveQuestionMarkURL.matcher(URL).find()) {
                        api.Set("{\"URL\":\"" + URL.replaceAll("&dummy=true", "") + "\", \"ClientID\":\"" + Function.config_twitcast_ClientId + "\", \"ClientSecret\":\"" + Function.config_twitcast_ClientSecret + "\"}");
                    } else {
                        api.Set("{\"URL\":\"" + URL.split("\\?")[0].replaceAll("&dummy=true", "") + "\", \"ClientID\":\"" + Function.config_twitcast_ClientId + "\", \"ClientSecret\":\"" + Function.config_twitcast_ClientSecret + "\"}");
                    }
                } else {
                    if (NotRemoveQuestionMarkURL.matcher(URL).find()) {
                        api.Set("{\"URL\":\"" + URL.replaceAll("&dummy=true", "") + "\"}");
                    } else {
                        api.Set("{\"URL\":\"" + URL.split("\\?")[0].replaceAll("&dummy=true", "") + "\"}");
                    }
                }


                json = api.Get();
                //ServiceName = api.getServiceName();
                proxy = api.getUseProxy();
            } else {
                json = "{}";
            }

            //System.out.println(json);
            Date date = new Date();

            LogData logData = new LogData();
            logData.setHTTPRequest(httpRequest);
            logData.setRequestURL(URL);
            logData.setUnixTime(date.getTime());

            WebhookData webhookData = new WebhookData();
            webhookData.setURL(URL);
            webhookData.setHTTPRequest(httpRequest);
            webhookData.setDate(date);


            cacheData = new CacheData();
            if (json != null){
                JsonElement element = gson.fromJson(json, JsonElement.class);

                String errorMessage = element.getAsJsonObject().has("ErrorMessage") ? element.getAsJsonObject().get("ErrorMessage").getAsString() : null;
                String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : (element.getAsJsonObject().has("LiveURL") ? element.getAsJsonObject().get("LiveURL").getAsString() : (element.getAsJsonObject().has("AudioURL") ? element.getAsJsonObject().get("AudioURL").getAsString() : null));

                logData.setResultURL(targetURL);
                webhookData.setResult(targetURL);
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    logData.setErrorMessage(errorMessage);
                    webhookData.setResult(errorMessage);
                }

                try (HttpClient client = proxy == null || proxy.split(":").length != 2 ? HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build()
                        :
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofSeconds(5))
                                .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                .build()
                ) {

                    ContentObject content;
                    //System.out.println("debug : " + ServiceName);
                    GetContent hls = GetContentList.get(ServiceName);
                    if (hls == null && (ServiceName == null || ServiceName.isEmpty())) {
                        logData.setResultURL(null);
                        webhookData.setResult("対応してないURL");
                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                        Function.WebhookData.put(logData.getLogID(), webhookData);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + "対応してないURL");

                        byte[] bytes = Function.compressByte(errContent404, sendContentEncoding);
                        Function.sendHTTPRequest(sock, httpVersion, 200, contentType_video_mp4, sendContentEncoding, bytes == null ? errContent404 : bytes, isHead);

                        return;
                    } else if (hls == null) {
                        OutputStream out = sock.getOutputStream();
                        StringBuilder sb_header = new StringBuilder();

                        sb_header.append("HTTP/").append(httpVersion).append(" 302 Found\r\nDate: ").append(new Date()).append("\r\nLocation: ").append(targetURL).append("\r\n\r\n");
                        out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                        out.flush();

                        out = null;
                        sb_header.setLength(0);
                        sb_header = null;

                        content = new ContentObject();
                        content.setDummyHLSText(null);
                        content.setHLSText(null);
                        cacheData.setRedirect(true);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + targetURL);

                    } else {
                        //System.out.println("!");
                        logData.setResultURL(targetURL);
                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                        if (isHLSDummyPrint) {
                            webhookData.setResult(targetURL);
                            Function.WebhookData.put(logData.getLogID(), webhookData);
                            System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + targetURL);
                        }

                        content = hls.run(client, httpRequest, URL, json);

                        cacheData.setHLS(content.getHLSText() != null ? content.getHLSText().getBytes(StandardCharsets.UTF_8) : null);
                        cacheData.setDummyHLS(content.getDummyHLSText() != null ? content.getDummyHLSText().getBytes(StandardCharsets.UTF_8) : null);
                        cacheData.setRedirect(false);
                        cacheData.setCookieText(content.getCookieText());
                        cacheData.setRefererText(content.getRefererText());
                        cacheData.setHLSFlag(content.isHLS());

                    }

                    cacheData.setProxy(proxy);
                    cacheData.setTargetURL(targetURL);
                    cacheData.setTitle(element.getAsJsonObject().has("Title") ? element.getAsJsonObject().get("Title").getAsString() : "(タイトルなし)");
                    cacheData.setSet(true);
                    cacheData.setCacheDate(new Date().getTime());

                    // タイトル取得
                    if (isGetTitle) {

                        byte[] title_bytes = Function.compressByte(cacheData.getTitle().getBytes(StandardCharsets.UTF_8), sendContentEncoding);
                        if (errorMessage != null) {

                            byte[] error = ("エラー : " + errorMessage).getBytes(StandardCharsets.UTF_8);
                            byte[] bytes = Function.compressByte(error, sendContentEncoding);

                            Function.sendHTTPRequest(sock, httpVersion, 200, contentType_text, sendContentEncoding, bytes == null ? error : bytes, isHead);

                            Function.GetURLAccessLog.put(logData.getLogID(), logData);
                            Function.WebhookData.put(logData.getLogID(), webhookData);
                            System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> エラー : " + errorMessage);
                            return;
                        }

                        Function.sendHTTPRequest(sock, httpVersion, 200, contentType_text, sendContentEncoding, title_bytes == null ? cacheData.getTitle().getBytes(StandardCharsets.UTF_8) : title_bytes, isHead);

                        logData.setResultURL(cacheData.getTitle());
                        webhookData.setResult(cacheData.getTitle());
                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                        Function.WebhookData.put(logData.getLogID(), webhookData);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + cacheData.getTitle());

                        Function.addCache((pattern_Asterisk.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""), cacheData);
                        return;

                    }

                    // エラー
                    if (errorMessage != null) {
                        byte[] content2 = Function.getErrorMessageVideo(client, errorMessage);
                        byte[] bytes = Function.compressByte(content2, sendContentEncoding);

                        Function.sendHTTPRequest(sock, httpVersion, 200, contentType_video_mp4, sendContentEncoding, bytes == null ? content2 : bytes, isHead);

                        logData.setErrorMessage(errorMessage);
                        webhookData.setResult(errorMessage);

                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                        Function.WebhookData.put(logData.getLogID(), webhookData);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + errorMessage);
                        return;
                    }

                    if (!cacheData.isRedirect()){
                        byte[] dummy_bytes = Function.compressByte(cacheData.getDummyHLS(), sendContentEncoding);
                        byte[] hls_bytes = Function.compressByte(cacheData.getHLS(), sendContentEncoding);

                        if (cacheData.isHLS()){
                            if (cacheData.getDummyHLS() != null){
                                if (isHLSDummyPrint && !vlc_ua.matcher(httpRequest).find()) {
                                    Function.sendHTTPRequest(sock, httpVersion, 200, contentType_hls, sendContentEncoding, dummy_bytes == null ? cacheData.getDummyHLS() : dummy_bytes, isHead);
                                } else {
                                    Function.sendHTTPRequest(sock, httpVersion, 200, contentType_hls, sendContentEncoding, hls_bytes == null ? cacheData.getHLS() : hls_bytes, isHead);
                                }
                            } else {
                                Function.sendHTTPRequest(sock, httpVersion, 200, contentType_hls, sendContentEncoding, hls_bytes == null ? cacheData.getHLS() : hls_bytes, isHead);
                            }
                        } else {
                            OutputStream out = sock.getOutputStream();
                            StringBuilder sb_header = new StringBuilder();

                            sb_header.append("HTTP/").append(httpVersion).append(" 302 Found\r\nDate: ").append(new Date()).append("\r\nLocation: /https/cookie:[").append(cacheData.getCookieText()).append("]/referer:[").append(cacheData.getRefererText()).append("]/").append(cacheData.getTargetURL().replaceAll("http(.*)://", "")).append("\r\n\r\n");
                            out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                            out.flush();

                            out = null;
                            sb_header.setLength(0);
                            sb_header = null;
                        }

                    }

                    Function.addCache((NotRemoveQuestionMarkURL.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""), cacheData);

                    return;

                } catch (Exception e){
                    e.printStackTrace();
                    byte[] bytes = Function.compressByte(errContent000, sendContentEncoding);
                    try {
                        Function.sendHTTPRequest(sock, httpVersion, 200, contentType_video_mp4, sendContentEncoding, bytes == null ? errContent000 : bytes, isHead);
                    } catch (Exception ex){
                        // ex.printStackTrace();
                    }

                    logData.setResultURL(e.getMessage());
                    webhookData.setResult(e.getMessage());
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    Function.WebhookData.put(logData.getLogID(), webhookData);

                    return;
                }

            }

            // ここには来ないと思うけど一応
            try {
                logData.setResultURL(null);
                webhookData.setResult("内部エラー");
                Function.GetURLAccessLog.put(logData.getLogID(), logData);
                Function.WebhookData.put(logData.getLogID(), webhookData);
                System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + "内部エラー");
                byte[] bytes = Function.compressByte(errContent000, sendContentEncoding);

                Function.sendHTTPRequest(sock, httpVersion, 200, contentType_video_mp4, sendContentEncoding, bytes == null ? errContent000 : bytes, isHead);
            } catch (Exception ex){
                // ex.printStackTrace();
            }
            return;
        } catch (Exception e){
            e.printStackTrace();

            try {
                byte[] bytes = Function.compressByte(errContent000, sendContentEncoding);
                Function.sendHTTPRequest(sock, httpVersion, 200, contentType_video_mp4, sendContentEncoding, bytes == null ? errContent000 : bytes, isHead);
            } catch (Exception ex){
                // ex.printStackTrace();
            }
        }

        // ここには来ないと思うけど
        try {
            byte[] bytes = Function.compressByte(errContent404, sendContentEncoding);
            Function.sendHTTPRequest(sock, httpVersion, 200, contentType_video_mp4, sendContentEncoding, bytes == null ? errContent404 : bytes, isHead);
        } catch (Exception ex){
            // ex.printStackTrace();
        }
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
    public void setHTTPSocket(Socket sock) {
        this.sock = sock;
    }
}
