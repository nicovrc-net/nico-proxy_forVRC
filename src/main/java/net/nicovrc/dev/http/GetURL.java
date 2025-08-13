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
        final String httpVersion = Function.getHTTPVersion(httpRequest);
        final boolean isHead = method != null && method.equals("HEAD");

        try {
            //System.out.println(URL);

            Matcher matcher_m = dummy_url2.matcher(URL);
            if (matcher_m.find()){
                //System.out.println(URL);
                //URL = "/?url="+matcher_m.group(1)+"&dummy=true";
                URL = matcher_m.group(1) + matcher_m.group(2) + "&dummy=true";
                //System.out.println(URL);
            }

            URL = URL.replaceAll("^(/(.*)\\?url=|/\\?vi=|/proxy/(.*)\\?)", "");
            final String targetUrl = (NotRemoveQuestionMarkURL.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", "");
            //System.out.println(targetUrl);

            ServiceAPI api = null;
            //System.out.println(targetUrl);
            CacheData cacheData = Function.CacheList.get(targetUrl);

            String json = null;
            String ServiceName = null;
            String proxy = null;

            final boolean isCache = cacheData != null;
            final boolean isHLSDummyPrint = !dummy_url.matcher(URL).find();
            final boolean isGetTitle = vrc_getStringUA.matcher(httpRequest).find();
            final long currentTimeLong = new Date().getTime();

            //System.out.println(isCache);
            if (isCache) {
                cacheData = Function.CacheList.get(targetUrl);
                int i = cacheData == null ? 1 : 0;
                while (i == 0){
                    cacheData = Function.CacheList.get(targetUrl);
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
                                Function.CacheList.remove(targetUrl);
                                cacheData = null;
                            }
                        }
                    }
                }

                if (cacheData != null){

                    final CacheData fCacheData = cacheData;
                    if (isGetTitle){
                        Thread.ofVirtual().start(()-> System.out.println("[Get URL (キャッシュ," + Function.sdf.format(new Date()) + ")] " + URL + " ---> " + fCacheData.getTitle()));

                        Function.sendHTTPRequest(sock, httpVersion, 200, "text/plain; charset=utf-8", cacheData.getTitle().getBytes(StandardCharsets.UTF_8), isHead);
                    } else {
                        if (isHLSDummyPrint){
                            Thread.ofVirtual().start(() -> System.out.println("[Get URL (キャッシュ," + Function.sdf.format(new Date()) + ")] " + URL + " ---> " + fCacheData.getTargetURL()));
                        }
                        byte[] content = null;

                        if (cacheData.isRedirect()){
                            OutputStream out = sock.getOutputStream();
                            StringBuilder sb_header = new StringBuilder();

                            sb_header.append("HTTP/").append(Function.getHTTPVersion(httpRequest)).append(" 302 Found\r\nLocation: ").append(cacheData.getTargetURL()).append("\r\n\r\n");
                            out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                            out.flush();

                            out = null;
                            sb_header.setLength(0);
                            sb_header = null;

                            return;
                        }

                        if (cacheData.getDummyHLS() != null){
                            if (isHLSDummyPrint && !vlc_ua.matcher(httpRequest).find()) {
                                Function.sendHTTPRequest(sock, httpVersion, 200, "application/vnd.apple.mpegurl", cacheData.getDummyHLS() != null ? cacheData.getDummyHLS() : Function.zeroByte, isHead);
                            } else {
                                Function.sendHTTPRequest(sock, httpVersion, 200, "application/vnd.apple.mpegurl", cacheData.getHLS() != null ? cacheData.getHLS() : Function.zeroByte, isHead);
                            }
                        } else {
                            Function.sendHTTPRequest(sock, httpVersion, 200, "application/vnd.apple.mpegurl", cacheData.getHLS() != null ? cacheData.getHLS() : Function.zeroByte, isHead);
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
                    String user_session = null;
                    String user_session_secure = null;

                    try {
                        final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                        user_session = yamlMapping.string("NicoNico_user_session");
                        user_session_secure = yamlMapping.string("NicoNico_user_session_secure");
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    if (user_session != null && user_session_secure != null) {
                        api.Set("{\"URL\":\"" + URL.split("\\?")[0].replaceAll("&dummy=true", "") + "\", \"user_session\":\"" + user_session + "\", \"user_session_secure\":\"" + user_session_secure + "\"}");
                    } else {
                        api.Set("{\"URL\":\"" + URL.split("\\?")[0].replaceAll("&dummy=true", "") + "\"}");
                    }

                } else if (URL.startsWith("https://twitcasting.tv")) {
                    String ClientId = "";
                    String ClientSecret = "";

                    try {
                        final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                        ClientId = yamlMapping.string("TwitcastingClientID");
                        ClientSecret = yamlMapping.string("TwitcastingClientSecret");
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    if (NotRemoveQuestionMarkURL.matcher(URL).find()) {
                        api.Set("{\"URL\":\"" + URL.replaceAll("&dummy=true", "") + "\", \"ClientID\":\"" + ClientId + "\", \"ClientSecret\":\"" + ClientSecret + "\"}");
                    } else {
                        api.Set("{\"URL\":\"" + URL.split("\\?")[0].replaceAll("&dummy=true", "") + "\", \"ClientID\":\"" + ClientId + "\", \"ClientSecret\":\"" + ClientSecret + "\"}");
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
                    GetContent hls = GetContentList.get(ServiceName);
                    if (hls == null && ServiceName.isEmpty()) {
                        logData.setResultURL(null);
                        webhookData.setResult("対応してないURL");
                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                        Function.WebhookData.put(logData.getLogID(), webhookData);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + "対応してないURL");
                        Function.sendHTTPRequest(sock, httpVersion, 200, "video/mp4", errContent404, isHead);
                    } else if (hls == null) {
                        OutputStream out = sock.getOutputStream();
                        StringBuilder sb_header = new StringBuilder();

                        sb_header.append("HTTP/").append(httpVersion).append(" 302 Found\r\nLocation: ").append(targetURL).append("\r\n\r\n");
                        out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                        out.flush();

                        out = null;
                        sb_header.setLength(0);
                        sb_header = null;

                        content = new ContentObject();
                        content.setDummyHLSText(null);
                        content.setHLSText(null);
                        content.setContentObject(null);
                        cacheData.setRedirect(true);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + targetURL);

                    } else {
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

                    }

                    cacheData.setProxy(proxy);
                    cacheData.setTargetURL(targetURL);
                    cacheData.setTitle(element.getAsJsonObject().has("Title") ? element.getAsJsonObject().get("Title").getAsString() : "(タイトルなし)");
                    cacheData.setSet(true);
                    cacheData.setCacheDate(new Date().getTime());

                    // タイトル取得
                    if (isGetTitle) {

                        if (errorMessage != null) {
                            Function.sendHTTPRequest(sock, httpVersion, 200, "text/plain; charset=utf-8", ("エラー : " + errorMessage).getBytes(StandardCharsets.UTF_8), isHead);

                            Function.GetURLAccessLog.put(logData.getLogID(), logData);
                            Function.WebhookData.put(logData.getLogID(), webhookData);
                            System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> エラー : " + errorMessage);
                            return;
                        }

                        if (element.getAsJsonObject().has("Title")) {
                            String title = element.getAsJsonObject().get("Title").getAsString();
                            Function.sendHTTPRequest(sock, httpVersion, 200, "text/plain; charset=utf-8", title.getBytes(StandardCharsets.UTF_8), isHead);

                            logData.setResultURL(title);
                            webhookData.setResult(title);
                            Function.GetURLAccessLog.put(logData.getLogID(), logData);
                            Function.WebhookData.put(logData.getLogID(), webhookData);
                            System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + title);
                        } else {
                            Function.sendHTTPRequest(sock, httpVersion, 200, "text/plain; charset=utf-8", "(タイトルなし)".getBytes(StandardCharsets.UTF_8), isHead);

                            logData.setResultURL("(タイトルなし)");
                            webhookData.setResult("(タイトルなし)");
                            Function.GetURLAccessLog.put(logData.getLogID(), logData);
                            Function.WebhookData.put(logData.getLogID(), webhookData);
                            System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> (タイトルなし)");
                        }

                        Function.CacheList.put((pattern_Asterisk.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""), cacheData);
                        return;

                    }

                    // エラー
                    if (errorMessage != null) {
                        byte[] content2 = Function.getErrorMessageVideo(client, errorMessage);
                        Function.sendHTTPRequest(sock, httpVersion, 200, "video/mp4", content2, isHead);

                        logData.setErrorMessage(errorMessage);
                        webhookData.setResult(errorMessage);

                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                        Function.WebhookData.put(logData.getLogID(), webhookData);
                        System.out.println("[Get URL (" + Function.sdf.format(date) + ")] " + URL + " ---> " + errorMessage);
                        return;
                    }

                    if (!cacheData.isRedirect()){
                        if (cacheData.getDummyHLS() != null){
                            if (isHLSDummyPrint && !vlc_ua.matcher(httpRequest).find()) {
                                Function.sendHTTPRequest(sock, httpVersion, 200, "application/vnd.apple.mpegurl", cacheData.getDummyHLS(), isHead);
                            } else {
                                Function.sendHTTPRequest(sock, httpVersion, 200, "application/vnd.apple.mpegurl", cacheData.getHLS(), isHead);
                            }
                        } else {
                            Function.sendHTTPRequest(sock, httpVersion, 200, "application/vnd.apple.mpegurl", cacheData.getHLS(), isHead);
                        }
                    }

                    Function.CacheList.put((NotRemoveQuestionMarkURL.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""), cacheData);



                } catch (Exception e){
                    e.printStackTrace();
                    try {
                        byte[] content = null;
                        File file = new File("./error-video/error_000.mp4");
                        if (file.exists()){
                            FileInputStream stream = new FileInputStream(file);
                            content = stream.readAllBytes();
                            stream.close();
                            stream = null;
                        }

                        Function.sendHTTPRequest(sock, httpVersion, 200, "video/mp4", content, isHead);
                        content = null;
                    } catch (Exception ex){
                        // ex.printStackTrace();
                    }

                    logData.setResultURL(e.getMessage());
                    webhookData.setResult(e.getMessage());
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    Function.WebhookData.put(logData.getLogID(), webhookData);


                }

            }
        } catch (Exception e){
            e.printStackTrace();
            try {

                Function.sendHTTPRequest(sock, httpVersion, 200, "video/mp4", errContent000, isHead);
            } catch (Exception ex){
                // ex.printStackTrace();
            }
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
