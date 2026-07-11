package net.nicovrc.dev.http;

import com.google.gson.JsonElement;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.data.LogData;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;
import net.nicovrc.dev.data.WebhookData;
import net.nicovrc.dev.http.getContent.*;

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
    private final HashMap<String, GetContent> GetContentList = new HashMap<>();

    private final List<ServiceAPI> list = ServiceList.getServiceList();

    private final Pattern NotRemoveQuestionMarkURL = Pattern.compile("(youtu\\.be|www\\.youtube\\.com|(.+)\\.pornhub\\.com)");

    private final Pattern pattern_Asterisk = Pattern.compile("\\*");

    private final Pattern dummy_url = Pattern.compile("&dummy=true");
    private final Pattern dummy_url2 = Pattern.compile("^/(\\?dummy=true&url=|dummy\\.m3u8\\?dummy=true&url=|proxy/\\?dummy=true&|\\?dummy=true&vi=)(.+)");
    private final Pattern getUrl2 = Pattern.compile("&url=(.+)");
    private final Pattern vrc_getStringUA = Pattern.compile("UnityPlayer/(.+) \\(UnityWebRequest/(.+), libcurl/(.+)\\)");
    private final Pattern ffmpegUA = Pattern.compile("[U|u]ser-[A|a]gent: Lavf/");

    private final Pattern vlc_ua = Pattern.compile("(VLC/(.+) LibVLC/(.+)|LibVLC)");
    private final Pattern avpro_ua = Pattern.compile("(NSPlayer|AVPro|AppleCoreMedia)");

    public GetURL(){

        GetContentList.put("ニコニコ", new NicoVideo());
        GetContentList.put("ツイキャス", new Twitcast());
        GetContentList.put("Abema", new Abema());
        GetContentList.put("TikTok", new TikTok());
        GetContentList.put("mellow-fan", new mellow_fan());
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
    }

    @Override
    public void run() {
        if (client == null){
            return;
        }
        if (ch == null){
            return;
        }

        final String httpVersion = Function.getHTTPVersion(httpRequest) != null ? Function.getHTTPVersion(httpRequest) : "1.1";

        //System.out.println("a : "+conent_encoding);
        //System.out.println("s : " + sendContentEncoding);

        try {
            //System.out.println(URL);

            Matcher matcher_m = dummy_url2.matcher(URL);
            if (matcher_m.find()){
                //System.out.println(URL);
                //URL = "/?url="+matcher_m.group(1)+"&dummy=true";
                URL = matcher_m.group(2) + "&dummy=true";
                //System.out.println(URL);
            }

            Matcher matcher = getUrl2.matcher(URL);
            if (matcher.find()){
                URL = matcher.group(1);
            }

            URL = URL.replaceAll("^(/(.*)\\?url=|/\\?vi=|/proxy/(.*)\\?)", "");
            final String targetUrl = (NotRemoveQuestionMarkURL.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", "");
            //System.out.println(targetUrl);

            ServiceAPI api = null;
            //System.out.println(targetUrl);
            CacheData cacheData = Function.getCache(targetUrl);

            String json = null;
            String ServiceName = null;

            final boolean isCache = cacheData != null;
            final boolean isHLSDummyPrint = !dummy_url.matcher(URL).find();// || ffmpegUA.matcher(httpRequest).find();
            final boolean isGetTitle = vrc_getStringUA.matcher(httpRequest).find();

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
                    }
                }

                if (cacheData != null){
                    String targetURL = cacheData.getTargetURL();
                    String cookieText = cacheData.getCookieText();
                    String refererText = cacheData.getRefererText();
                    boolean isFound = true;

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

                    if (!isFound){
                        Function.deleteCache(targetUrl);
                        cacheData = null;
                    }
                }

                if (cacheData != null){

                    final CacheData fCacheData = cacheData;
                    if (isGetTitle){
                        Thread.ofVirtual().start(()-> System.out.println("[Get URL (キャッシュ," + Function.sdf.format(new Date()) + ")] " + URL + " ---> " + fCacheData.getTitle()));
                        Function.sendHttpData(ch, httpVersion, 200, Function.contentType_textPlain, null, null, cacheData.getTitle().getBytes(StandardCharsets.UTF_8), null);

                    } else {
                        if (isHLSDummyPrint){
                            Thread.ofVirtual().start(() -> System.out.println("[Get URL (キャッシュ," + Function.sdf.format(new Date()) + ")] " + URL + " ---> " + fCacheData.getTargetURL()));
                        }

                        if (cacheData.isRedirect()){
                            Function.sendHttpData(ch, httpVersion, 302, null, null, null, null, cacheData.getTargetURL());
                            return;
                        }

                        if (cacheData.isHLS()){
                            byte[] dummy_bytes = ("#EXTM3U\n/dummy.m3u8?url="+URL+"&dummy=true").getBytes(StandardCharsets.UTF_8);
                            byte[] hls_bytes = cacheData.getHLS();
                            byte[] send_data = null;

                            if (cacheData.getDummyHLS() != null){

                                if (isHLSDummyPrint && !vlc_ua.matcher(httpRequest).find() && !ffmpegUA.matcher(httpRequest).find() && !avpro_ua.matcher(httpRequest).find()) {
                                    send_data = dummy_bytes;
                                } else {
                                    send_data = hls_bytes;
                                }
                            } else {
                                send_data = hls_bytes;
                            }
                            Function.sendHttpData(ch, httpVersion, 200, Function.contentType_hls, null, null, send_data, null);
                        } else {
                            Function.sendHttpData(ch, httpVersion, 302, null, null, null, null, cacheData.getTargetURL());
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

            try {
                if (api != null) {
                    //System.out.println("aaa");
                    //System.out.println(URL.startsWith("https://twitcasting.tv"));
                    ServiceName = api.getServiceName();
                    api.setHttpClient(client);
                    api.setURL(NotRemoveQuestionMarkURL.matcher(URL).find() ? URL.replaceAll("&dummy=true", "") : URL.split("\\?")[0].replaceAll("&dummy=true", ""));
                    api.setProxy(null);

                    if (ServiceName.equals("ニコニコ")) {

                        if (Function.config_user_session != null && Function.config_nicosid != null) {
                            api.setToken(new String[]{Function.config_user_session, Function.config_nicosid});
                        }

                    } else if (URL.startsWith("https://twitcasting.tv")) {
                        api.setToken(new String[]{Function.config_twitcast_ClientId, Function.config_twitcast_ClientSecret});
                    }
                    json = api.get();
                    //ServiceName = api.getServiceName();
                } else {
                    json = "{}";
                }
            } catch (Exception e){
                e.printStackTrace();
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
                JsonElement element = Function.gson.fromJson(json, JsonElement.class);

                String errorMessage = element.getAsJsonObject().has("ErrorMessage") ? element.getAsJsonObject().get("ErrorMessage").getAsString() : null;
                String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : (element.getAsJsonObject().has("LiveURL") ? element.getAsJsonObject().get("LiveURL").getAsString() : (element.getAsJsonObject().has("AudioURL") ? element.getAsJsonObject().get("AudioURL").getAsString() : null));

                logData.setResultURL(targetURL);
                webhookData.setResult(targetURL);
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    logData.setErrorMessage(errorMessage);
                    webhookData.setResult(errorMessage);
                }

                ContentObject content;
                //System.out.println("debug : " + ServiceName);
                GetContent hls = GetContentList.get(ServiceName);
                if (hls == null && (ServiceName == null || ServiceName.isEmpty())) {
                    logData.setResultURL(null);
                    webhookData.setResult("対応してないURL");
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    Function.WebhookData.put(logData.getLogID(), webhookData);
                    System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + "対応してないURL");

                    Function.sendHttpData(ch, httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_site, null);

                    return;
                } else if (hls == null) {
                    Function.sendHttpData(ch, httpVersion, 302, null, null, null, null, targetURL);

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
                    if (errorMessage != null) {
                        Function.sendHttpData(ch, httpVersion, 200, Function.contentType_textPlain, null, null, ("エラー : " + errorMessage).getBytes(StandardCharsets.UTF_8), null);

                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                        Function.WebhookData.put(logData.getLogID(), webhookData);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> エラー : " + errorMessage);
                        return;
                    }

                    Function.sendHttpData(ch, httpVersion, 200, Function.contentType_textPlain, null, null, cacheData.getTitle().getBytes(StandardCharsets.UTF_8), null);

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
                    Function.sendHttpData(ch, httpVersion, 200, Function.contentType_video_mp4, null, null, Function.getErrorMessageVideo(client, errorMessage), null);

                    logData.setErrorMessage(errorMessage);
                    webhookData.setResult(errorMessage);

                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    Function.WebhookData.put(logData.getLogID(), webhookData);
                    System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + errorMessage);
                    return;
                }

                if (!cacheData.isRedirect()){
                    byte[] dummy_bytes = ("#EXTM3U\n/dummy.m3u8?url="+URL+"&dummy=true").getBytes(StandardCharsets.UTF_8);
                    byte[] hls_bytes = cacheData.getHLS();
                    byte[] send_data = null;

                    if (cacheData.isHLS()){
                        if (cacheData.getDummyHLS() != null){
                            if (isHLSDummyPrint && !vlc_ua.matcher(httpRequest).find() && !ffmpegUA.matcher(httpRequest).find() && !avpro_ua.matcher(httpRequest).find()) {
                                send_data = dummy_bytes;
                            } else {
                                send_data = hls_bytes;
                            }
                        } else {
                            send_data = hls_bytes;
                        }
                        Function.sendHttpData(ch, httpVersion, 200, Function.contentType_hls, null, null, send_data, null);
                    } else {
                        String redirectUrl = "/https/cookie:[" + cacheData.getCookieText() + "]/referer:[" + cacheData.getRefererText() + "]/" + cacheData.getTargetURL().replaceAll("http(.*)://", "");
                        Function.sendHttpData(ch, httpVersion, 302, null, null, null, null, redirectUrl);
                    }

                }

                Function.addCache((NotRemoveQuestionMarkURL.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""), cacheData);
                return;

            }

            // ここには来ないと思うけど一応
            try {
                logData.setResultURL(null);
                webhookData.setResult("内部エラー");
                Function.GetURLAccessLog.put(logData.getLogID(), logData);
                Function.WebhookData.put(logData.getLogID(), webhookData);
                System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + "内部エラー");
                Function.sendHttpData(ch, httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_others, null);
            } catch (Exception ex){
                ex.printStackTrace();
            }
            return;
        } catch (Exception e){
            e.printStackTrace();

            try {
                Function.sendHttpData(ch, httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_others, null);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }

        // ここには来ないと思うけど
        try {
            Function.sendHttpData(ch, httpVersion, 200, Function.contentType_video_mp4, null, null, Function.content_errorVideo_others, null);
        } catch (Exception ex){
            ex.printStackTrace();
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
}
