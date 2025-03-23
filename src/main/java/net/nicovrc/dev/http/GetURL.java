package net.nicovrc.dev.http;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.CacheData;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.LogData;
import net.nicovrc.dev.Service.Result.NicoNicoVideo;
import net.nicovrc.dev.Service.Result.OPENREC_Result;
import net.nicovrc.dev.Service.Result.TikTokResult;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetURL implements Runnable, NicoVRCHTTP {

    private Socket sock = null;
    private String URL = null;
    private String httpRequest = null;

    private final Gson gson = Function.gson;
    private final List<ServiceAPI> list = ServiceList.getServiceList();

    private final Pattern NotRemoveQuestionMarkURL = Pattern.compile("(youtu\\.be|www\\.youtube\\.com|pornhub\\.com)");

    private final Pattern pattern_Asterisk = Pattern.compile("\\*");

    private final Pattern vlc_ua = Pattern.compile("(VLC/(.+) LibVLC/(.+)|LibVLC)");
    private final Pattern dummy_url = Pattern.compile("&dummy=true");
    private final Pattern dummy_url2 = Pattern.compile("^/\\?dummy=true&url=(.+)");
    private final Pattern vrc_getStringUA = Pattern.compile("UnityPlayer/(.+) \\(UnityWebRequest/(.+), libcurl/(.+)\\)");

    private final Pattern hls_video = Pattern.compile("#EXT-X-STREAM-INF:BANDWIDTH=(\\d+),AVERAGE-BANDWIDTH=(\\d+),CODECS=\"(.+)\",RESOLUTION=(.+),FRAME-RATE=(.+),AUDIO=\"(.+)\"\n");
    private final Pattern hls_audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"(.+)\"");

    private final Pattern hlslive_video = Pattern.compile("#EXT-X-STREAM-INF:BANDWIDTH=(\\d+),AVERAGE-BANDWIDTH=(\\d+),CODECS=\"(.+)\",RESOLUTION=(.+),FRAME-RATE=(.+),AUDIO=\"(.+)\"\n");
    private final Pattern hlslive_audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"(.+)\"");

    @Override
    public void run() {
        String method = Function.getMethod(httpRequest);

        try {

            Matcher matcher = dummy_url2.matcher(URL);
            if (matcher.find()){
                //System.out.println(URL);
                URL = "/?url="+matcher.group(1)+"&dummy=true";
                //System.out.println(URL);
            }

            URL = URL.replaceAll("^(/\\?url=|/\\?vi=|/proxy/(.*)\\?)", "");

            //System.out.println(URL);

            ServiceAPI api = null;
            CacheData cacheData = Function.CacheList.get((pattern_Asterisk.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""));

            String json = null;
            String ServiceName = null;
            String proxy = null;

            boolean isCache = false;

            if (cacheData == null) {
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

                if (!dummy_url.matcher(URL).find()){
                    if (api != null){
                        cacheData = new CacheData();
                        cacheData.setServiceAPI(api);
                        cacheData.setSet(false);
                        cacheData.setCacheDate(-2L);

                        Function.CacheList.put((pattern_Asterisk.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""), cacheData);
                    }
                }

                if (api != null) {
                    //System.out.println("aaa");
                    //System.out.println(URL.startsWith("https://twitcasting.tv"));
                    if (api.getServiceName().equals("ニコニコ")) {
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
                    ServiceName = api.getServiceName();
                    proxy = api.getUseProxy();

                    if (!dummy_url.matcher(URL).find()) {
                        cacheData = new CacheData();
                        cacheData.setCacheDate(new Date().getTime());
                        cacheData.setServiceAPI(api);
                        cacheData.setSet(true);
                        cacheData.setResultJson(json);

                        Function.CacheList.remove((pattern_Asterisk.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""));

                        JsonElement element = gson.fromJson(json, JsonElement.class);

                        if (element.getAsJsonObject().has("VideoURL") || element.getAsJsonObject().has("LiveURL") || element.getAsJsonObject().has("AudioURL")) {
                            Function.CacheList.put((pattern_Asterisk.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""), cacheData);
                        }
                    }
                }

            } else {

                int i = 0;
                while (i == 0){
                    cacheData = Function.CacheList.get((pattern_Asterisk.matcher(URL).find() ? URL.split("&")[0] : URL.split("\\?")[0]).replaceAll("&dummy=true", ""));
                    if (cacheData == null){
                        i = 1;
                        continue;
                    }
                    if (cacheData.isSet()){
                        i = 1;
                        continue;
                    }
                }

                if (cacheData != null){
                    isCache = true;
                    json = cacheData.getResultJson();
                    ServiceName = cacheData.getServiceAPI().getServiceName();
                    proxy = cacheData.getServiceAPI().getUseProxy();
                }

            }

            //System.out.println(json);
            byte[] content = new byte[0];
            LogData logData = new LogData();
            logData.setHTTPRequest(httpRequest);
            logData.setRequestURL(URL);

            Date date = new Date();

            if (json != null){
                JsonElement element = gson.fromJson(json, JsonElement.class);

                if (vrc_getStringUA.matcher(httpRequest).find()){
                    String errorMessage = null;
                    if (element.getAsJsonObject().has("ErrorMessage")) {
                        errorMessage = element.getAsJsonObject().get("ErrorMessage").getAsString();
                        logData.setErrorMessage(errorMessage);
                    }
                    // タイトル取得
                    if (errorMessage != null){
                        System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + errorMessage);
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "text/plain; charset=utf-8", ("エラー : " + errorMessage).getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                    } else if (element.getAsJsonObject().has("Title")) {
                        System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + element.getAsJsonObject().get("Title").getAsString());
                        logData.setResultTitle(element.getAsJsonObject().get("Title").getAsString());
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "text/plain; charset=utf-8", element.getAsJsonObject().get("Title").getAsString().getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                    } else {
                        System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + "(タイトルなし)");
                        logData.setResultTitle("(タイトルなし)");
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "text/plain; charset=utf-8", "".getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                    }

                    errorMessage = null;
                    element = null;
                    content = null;

                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    return;
                }

                if (element.getAsJsonObject().has("ErrorMessage")) {
                    String errorMessage = element.getAsJsonObject().get("ErrorMessage").getAsString();
                    if (!dummy_url.matcher(URL).find()){
                        System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + errorMessage);
                    }
                    logData.setErrorMessage(errorMessage);

                    try {
                        MessageDigest sha3_256 = MessageDigest.getInstance("SHA3-256");
                        byte[] sha3_256_result = sha3_256.digest(errorMessage.getBytes(StandardCharsets.UTF_8));
                        String str = new String(Base64.getEncoder().encode(sha3_256_result), StandardCharsets.UTF_8);
                        String videoId = str.replaceAll("\\\\", "").replaceAll("\\+", "").replaceAll("/", "").substring(0, 20);

                        File file = new File("./error-video/" + videoId + ".mp4");
                        if (file.exists()){
                            FileInputStream stream = new FileInputStream(file);
                            content = stream.readAllBytes();
                            stream.close();
                            stream = null;
                        } else {
                            //
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

                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(new URI("https://nicovrc.net/v3-video/error.php?msg=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)))
                                        .headers("User-Agent", Function.UserAgent)
                                        .GET()
                                        .build();
                                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                                FileOutputStream stream = new FileOutputStream("./error-video/" + videoId + ".png");
                                stream.write(send.body());
                                stream.close();
                                stream = null;
                                send = null;

                                final String ffmpegPass;
                                if (new File("/bin/ffmpeg").exists()){
                                    ffmpegPass = "/bin/ffmpeg";
                                } else if (new File("/usr/bin/ffmpeg").exists()){
                                    ffmpegPass = "/usr/bin/ffmpeg";
                                } else if (new File("/usr/local/bin/ffmpeg").exists()){
                                    ffmpegPass = "/usr/local/bin/ffmpeg";
                                } else if (new File("./ffmpeg").exists()){
                                    ffmpegPass = "./ffmpeg";
                                } else if (new File("./ffmpeg.exe").exists()){
                                    ffmpegPass = "./ffmpeg.exe";
                                } else if (new File("C:\\Windows\\System32\\ffmpeg.exe").exists()){
                                    ffmpegPass = "C:\\Windows\\System32\\ffmpeg.exe";
                                } else {
                                    ffmpegPass = "";
                                }
                                Runtime runtime = Runtime.getRuntime();
                                if (!new File("./error-video/out.mp3").exists()){
                                    final Process exec0 = runtime.exec(new String[]{ffmpegPass, "-f","lavfi","-i","anullsrc=r=44100:cl=mono","-t","5","-aq","1","-c:a","libmp3lame","./error-video/out.mp3"});
                                    Thread.ofVirtual().start(() -> {
                                        try {
                                            Thread.sleep(5000L);
                                        } catch (Exception e) {
                                            //e.printStackTrace();
                                        }

                                        if (exec0.isAlive()) {
                                            exec0.destroy();
                                        }
                                    });
                                    exec0.waitFor();
                                }
                                final Process exec1 = runtime.exec(new String[]{ffmpegPass, "-loop","1","-i","./error-video/"+videoId+".png","-i","./error-video/out.mp3","-c:v","libx264","-pix_fmt","yuv420p","-c:a","copy","-map","0:v:0","-map","1:a:0","-t","20","-r","60","./error-video/" + videoId + ".mp4"});
                                Thread.ofVirtual().start(() -> {
                                    try {
                                        Thread.sleep(5000L);
                                    } catch (Exception e) {
                                        //e.printStackTrace();
                                    }

                                    if (exec1.isAlive()) {
                                        exec1.destroy();
                                    }
                                });
                                exec1.waitFor();
                                /*byte[] read = null;
                                try {
                                    read = exec1.getInputStream().readAllBytes();
                                } catch (Exception e){
                                    read = new byte[0];
                                }
                                if (read.length == 0) {
                                    try {
                                        read = exec1.getErrorStream().readAllBytes();
                                    } catch (Exception e){
                                        read = new byte[0];
                                    }
                                }
                                String infoMessage = new String(read, StandardCharsets.UTF_8);
                                System.out.println(infoMessage);*/
                                //System.out.println(ffmpegPass);
                                file = new File("./error-video/" + videoId + ".jpg");
                                file.delete();

                                file = new File("./error-video/" + videoId + ".mp4");
                                if (file.exists()){
                                    FileInputStream stream1 = new FileInputStream(file);
                                    content = stream1.readAllBytes();
                                    stream1.close();
                                    stream1 = null;
                                }

                            } catch (Exception e){
                                e.printStackTrace();
                                file = new File("./error-video/error_000.mp4");
                                if (file.exists()){
                                    FileInputStream stream = new FileInputStream(file);
                                    content = stream.readAllBytes();
                                    stream.close();
                                    stream = null;
                                }
                            }
                        }
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));

                        file = null;
                        sha3_256_result = null;
                        sha3_256 = null;
                        str = null;
                        errorMessage = null;
                        element = null;
                        content = null;
                        return;
                    } catch (Exception e) {
                        File file = new File("./error-video/error_000.mp4");
                        if (file.exists()){
                            FileInputStream stream = new FileInputStream(file);
                            content = stream.readAllBytes();
                            stream.close();
                            stream = null;
                        }

                        errorMessage = null;
                        element = null;
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                        content = null;
                    }
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                } else if (ServiceName.equals("ニコニコ")) {
                    NicoNicoVideo result = gson.fromJson(json, NicoNicoVideo.class);
                    if (result != null) {
                        if (result.getVideoURL() != null) {
                            // ニコ動
                            if (!dummy_url.matcher(URL).find()) {
                                System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + result.getVideoURL());
                                logData.setResultURL(result.getVideoURL());
                                Function.GetURLAccessLog.put(logData.getLogID(), logData);
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

                                final StringBuilder sb = new StringBuilder();
                                result.getVideoAccessCookie().forEach((name, data) -> {
                                    sb.append(name).append("=").append(data).append(";");
                                });

                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(new URI(result.getVideoURL()))
                                        .headers("User-Agent", Function.UserAgent)
                                        .headers("Cookie", sb.substring(0, sb.length() - 1))
                                        .GET()
                                        .build();

                                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                                String hls = send.body().replaceAll("https://delivery\\.domand\\.nicovideo\\.jp", "/https/cookie:[" + sb.substring(0, sb.length() - 1) + "]/delivery.domand.nicovideo.jp");

                                if (vlc_ua.matcher(httpRequest).find()) {
                                    // VLCのときはそのまま
                                    Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "application/vnd.apple.mpegurl", hls.getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                                    sb.setLength(0);
                                    send = null;
                                    request = null;

                                    return;
                                }
                                // それ以外の場合は
                                if (!dummy_url.matcher(httpRequest).find()) {

                                    sb.setLength(0);
                                    Matcher matcher1 = hls_video.matcher(hls);
                                    Matcher matcher2 = hls_audio.matcher(hls);
                                    if (matcher1.find() && matcher2.find()) {
                                        hls = "#EXTM3U\n" +
                                                "#EXT-X-VERSION:6\n" +
                                                "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"" + matcher2.group(2) + "\"\n" +
                                                "#EXT-X-STREAM-INF:BANDWIDTH=" + matcher1.group(1) + ",AVERAGE-BANDWIDTH=" + matcher1.group(2) + ",CODECS=\"" + matcher1.group(3) + "\",RESOLUTION=" + matcher1.group(4) + ",FRAME-RATE=" + matcher1.group(5) + ",AUDIO=\"audio\"\n" +
                                                "dummy";

                                    }

                                    String[] split = hls.split("\n");
                                    split[split.length - 1] = "/?url=" + URL + "&dummy=true";

                                    for (String str : split) {
                                        sb.append(str).append("\n");
                                    }

                                    Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "application/vnd.apple.mpegurl", sb.toString().getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                                    sb.setLength(0);
                                    send = null;
                                    request = null;
                                } else {
                                    Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "application/vnd.apple.mpegurl", hls.getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                                    sb.setLength(0);
                                    send = null;
                                    request = null;
                                }
                            } catch (Exception e) {
                                // e.printStackTrace();
                            }
                        } else if (result.getLiveURL() != null) {
                            // ニコ生
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

                                String liveURL = result.getLiveURL();
                                if (!dummy_url.matcher(URL).find()) {
                                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + liveURL);
                                    logData.setResultURL(liveURL);
                                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                                }
                                if (result.getLiveAccessCookie() != null && !result.getLiveAccessCookie().isEmpty()) {
                                    // 新鯖

                                    final StringBuilder sb = new StringBuilder();
                                    result.getLiveAccessCookie().forEach((name, value) -> {
                                        sb.append(name).append("=").append(value).append("; ");
                                    });

                                    HttpRequest request = HttpRequest.newBuilder()
                                            .uri(new URI(liveURL))
                                            .headers("User-Agent", Function.UserAgent)
                                            .headers("Cookie", sb.substring(0, sb.length() - 2))
                                            .GET()
                                            .build();

                                    HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                                    String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                                    byte[] body = send.body();
                                    if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl")) {
                                        String s = new String(body, StandardCharsets.UTF_8);
                                        //System.out.println(s);
                                        s = s.replaceAll("https://", "/https/cookie:[" + (sb.substring(0, sb.length() - 2) == null || sb.substring(0, sb.length() - 2).isEmpty() ? "" : sb.substring(0, sb.length() - 2)) + "]/");
                                        body = s.getBytes(StandardCharsets.UTF_8);
                                    }
                                    if (vlc_ua.matcher(httpRequest).find()) {
                                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                                        method = null;
                                        sb.setLength(0);

                                        return;
                                    }

                                    // それ以外の場合は
                                    if (!dummy_url.matcher(httpRequest).find()) {
                                        sb.setLength(0);

                                        String hls = new String(body, StandardCharsets.UTF_8);
                                        Matcher matcher1 = hlslive_video.matcher(hls);
                                        Matcher matcher2 = hlslive_audio.matcher(hls);

                                        if (matcher1.find() && matcher2.find()) {
                                            hls = "#EXTM3U\n" +
                                                    "#EXT-X-VERSION:6\n" +
                                                    "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                                    "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"main\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"" + matcher2.group(2) + "\"\n" +
                                                    "#EXT-X-STREAM-INF:BANDWIDTH=" + matcher1.group(1) + ",AVERAGE-BANDWIDTH=" + matcher1.group(2) + ",CODECS=\"" + matcher1.group(3) + "\",RESOLUTION=" + matcher1.group(4) + ",FRAME-RATE=" + matcher1.group(5) + ",AUDIO=\"main\"\n" +
                                                    "dummy";
                                        }

                                        String[] split = hls.split("\n");
                                        split[split.length - 1] = "/?url=" + URL + "&dummy=true";

                                        for (String str : split) {
                                            sb.append(str).append("\n");
                                        }

                                        body = sb.toString().getBytes(StandardCharsets.UTF_8);

                                    }

                                    Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                                    sb.setLength(0);
                                    send = null;
                                    request = null;

                                } else {
                                    // dmc
                                    String[] split = liveURL.split("/");

                                    HttpRequest request = HttpRequest.newBuilder()
                                            .uri(new URI(liveURL))
                                            .headers("User-Agent", Function.UserAgent)
                                            .GET()
                                            .build();

                                    HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                                    String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").isPresent() ? send.headers().firstValue("content-type").get() : "" : send.headers().firstValue("Content-Type").get();
                                    byte[] body = send.body();
                                    if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl")) {
                                        // https://liveedge231.dmc.nico/hlslive/ht2_nicolive/nicolive-production-pg130607675867723_4ad3364a300b5325d4b25e4013a11ea9a50ef78c26d1d643c642f4ab14b905d4/4/mp4/playlist.m3u8?ht2_nicolive=131256034.ggfv0cb1a7_ss31uh_1u5gfs7lsf63i&__poll__=0
                                        String s = new String(body, StandardCharsets.UTF_8);
                                        //System.out.println(liveURL.replaceAll(split[split.length - 1].replaceAll("\\?", "\\\\?"), ""));
                                        String s1 = "/https/cookie:[]/" + (liveURL.replaceAll(split[split.length - 1].replaceAll("\\?", "\\\\?"), "").replaceAll("https://", ""));
                                        s = s.replaceAll("1/ts/playlist\\.m3u8", s1 + "/1/ts/playlist.m3u8");
                                        s = s.replaceAll("2/ts/playlist\\.m3u8", s1 + "/2/ts/playlist.m3u8");
                                        s = s.replaceAll("3/ts/playlist\\.m3u8", s1 + "/3/ts/playlist.m3u8");
                                        s = s.replaceAll("4/ts/playlist\\.m3u8", s1 + "/4/ts/playlist.m3u8");
                                        s = s.replaceAll("5/ts/playlist\\.m3u8", s1 + "/5/ts/playlist.m3u8");
                                        s = s.replaceAll("6/ts/playlist\\.m3u8", s1 + "/6/ts/playlist.m3u8");
                                        body = s.getBytes(StandardCharsets.UTF_8);
                                    }
                                    Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));

                                    method = null;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();

                                try {
                                    content = null;
                                    File file = new File("./error-video/error_000.mp4");
                                    if (file.exists()) {
                                        FileInputStream stream = new FileInputStream(file);
                                        content = stream.readAllBytes();
                                        stream.close();
                                        stream = null;
                                    }

                                    Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                                    content = null;
                                } catch (Exception ex) {
                                    // ex.printStackTrace();
                                }
                            }

                        } else {
                            File file = new File("./error-video/error_404_2.mp4");
                            if (file.exists()) {
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }
                            //System.out.println(content.length);
                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            file = null;
                            content = null;
                        }

                    }
                } else if (ServiceName.equals("ツイキャス")) {
                    String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + targetURL);
                    logData.setResultURL(targetURL);
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    OutputStream out = sock.getOutputStream();
                    StringBuilder sb_header = new StringBuilder();

                    //System.out.println(targetURL.replaceAll("https://", "/https/referer:[https://twitcasting.tv/]/cookie:[]/"));
                    sb_header.append("HTTP/").append(Function.getHTTPVersion(httpRequest)).append(" 302 Found\nLocation: ").append(targetURL.replaceAll("https://", "/https/referer:[https://twitcasting.tv/]/cookie:[]/")).append("\n\n");
                    out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    out = null;
                    sb_header.setLength(0);
                    sb_header = null;
                } else if (ServiceName.equals("Abema")) {
                    String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + targetURL);

                    OutputStream out = sock.getOutputStream();
                    StringBuilder sb_header = new StringBuilder();

                    //System.out.println(targetURL.replaceAll("https://", "/https/referer:[https://twitcasting.tv/]/cookie:[]/"));
                    sb_header.append("HTTP/").append(Function.getHTTPVersion(httpRequest)).append(" 302 Found\nLocation: ").append(targetURL.replaceAll("https://", "/https/referer:[]/cookie:[]/")).append("\n\n");
                    out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    out = null;
                    sb_header.setLength(0);
                    sb_header = null;

                } else if (ServiceName.equals("TikTok")) {
                    TikTokResult result = gson.fromJson(json, TikTokResult.class);
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + result.getVideoURL());
                    logData.setResultURL(result.getVideoURL());
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        //System.out.println(Proxy);
                        URI uri = new URI(result.getVideoURL());
                        //System.out.println(s);

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
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, send.body(), method != null && method.equals("HEAD"));
                        method = null;
                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }
                } else if (ServiceName.equals("OPENREC")) {
                    OPENREC_Result result = gson.fromJson(json, OPENREC_Result.class);
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + (result.isLive() ? result.getLiveURL() : result.getVideoURL()));
                    logData.setResultURL(result.isLive() ? result.getLiveURL() : result.getVideoURL());
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        //System.out.println(Proxy);
                        String url = result.isLive() ? result.getLiveURL() : result.getVideoURL();
                        URI uri = new URI(url);
                        //System.out.println(s);
                        //System.out.println(url);

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(uri)
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Referer", URL)
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        //System.out.println(send.uri());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();
                        //System.out.println(contentType.toLowerCase(Locale.ROOT));
                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl")) {
                            //System.out.println("!!!");
                            StringBuilder sb = new StringBuilder();
                            String hls = new String(send.body(), StandardCharsets.UTF_8);
                            String[] split = url.split("/");
                            boolean isEnd = false;
                            for (String s : hls.split("\n")) {
                                if (!isEnd){
                                    if (s.startsWith("#")){
                                        sb.append(s).append("\n");
                                        continue;
                                    }
                                    sb.append(url.replaceAll(split[split.length - 1], "")).append(s).append("\n");
                                    isEnd = true;
                                }
                            }
                            hls = sb.toString();
                            hls = hls.replaceAll("https://", "/https/referer:["+URL+"]/");
                            body = hls.getBytes(StandardCharsets.UTF_8);
                        }
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                        method = null;
                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }
                } else if (ServiceName.equals("TVer")) {
                    String url = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + url);
                    logData.setResultURL(url);
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);

                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(url))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Origin", "https://tver.jp")
                                .headers("Referer", "https://tver.jp/")
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        //System.out.println(send.uri());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();
                        if (vlc_ua.matcher(httpRequest).find()) {
                            // VLCのときはそのまま
                            if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl")) {
                                String s = new String(body, StandardCharsets.UTF_8);
                                s = s.replaceAll("https://", "/https/referer:[https://tver.jp/]/");
                                body = s.getBytes(StandardCharsets.UTF_8);
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                            send = null;
                            request = null;
                            return;
                        }

                        // それ以外の場合は
                        if (!dummy_url.matcher(httpRequest).find()) {
                            if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl")) {
                                body = (new String(body, StandardCharsets.UTF_8).replaceAll("https://", "/https/referer:[https://tver.jp/]/") + "\n/?url=" + URL + "&dummy=true").getBytes(StandardCharsets.UTF_8);
                            }
                        } else {
                            body = new String(body, StandardCharsets.UTF_8).replaceAll("https://", "/https/referer:[https://tver.jp/]/").getBytes(StandardCharsets.UTF_8);
                        }

                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                        send = null;
                        request = null;
                        method = null;

                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }

                } else if (ServiceName.equals("piapro") || ServiceName.equals("SoundCloud") || ServiceName.equals("Sonicbowl") || ServiceName.equals("Mixcloud") || ServiceName.equals("bandcamp")) {
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + element.getAsJsonObject().get("AudioURL").getAsString());
                    logData.setResultURL(element.getAsJsonObject().get("AudioURL").getAsString());
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(element.getAsJsonObject().get("AudioURL").getAsString()))
                                .headers("User-Agent", Function.UserAgent)
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                        if (ServiceName.equals("Sonicbowl") && send.statusCode() == 302){
                            String location = send.headers().firstValue("location").get();
                            request = HttpRequest.newBuilder()
                                    .uri(new URI(location))
                                    .headers("User-Agent", Function.UserAgent)
                                    .GET()
                                    .build();

                            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        }


                        //System.out.println(send.uri());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();

                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                            String s = new String(body, StandardCharsets.UTF_8);
                            s = s.replaceAll("https://", "/https/referer:[]/");
                            body = s.getBytes(StandardCharsets.UTF_8);

                            //System.out.println(s);
                        }

                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                        send = null;
                        request = null;
                        method = null;

                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }
                } else if (ServiceName.equals("Vimeo")) {
                    if (!dummy_url.matcher(httpRequest).find()) {
                        System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + element.getAsJsonObject().get("VideoURL").getAsString());
                        logData.setResultURL(element.getAsJsonObject().get("VideoURL").getAsString());
                        Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    }

                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(element.getAsJsonObject().get("VideoURL").getAsString()))
                                .headers("User-Agent", Function.UserAgent)
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        //System.out.println(send.uri());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();

                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                            String s = new String(body, StandardCharsets.UTF_8);

                            String[] split = element.getAsJsonObject().get("VideoURL").getAsString().split("/");

                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < split.length - 4; i++){
                                sb.append(split[i]).append("/");
                            }

                            s = s.replaceAll("\\.\\./\\.\\./\\.\\./", sb.toString());
                            s = s.replaceAll("https://", "/https/referer:[]/");
                            body = s.getBytes(StandardCharsets.UTF_8);

                            //System.out.println(s);
                        }

                        if (vlc_ua.matcher(httpRequest).find()) {
                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                            send = null;
                            request = null;
                            method = null;

                            return;
                        }

                        if (!dummy_url.matcher(httpRequest).find()) {
                            if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                                body = (new String(body, StandardCharsets.UTF_8) + "\n/?url=" + URL + "&dummy=true").getBytes(StandardCharsets.UTF_8);
                            }
                        }

                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                        send = null;
                        request = null;
                        method = null;

                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }

                } else if (ServiceName.equals("fc2")) {
                    String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + targetURL);
                    logData.setResultURL(targetURL);
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);

                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(targetURL))
                                .headers("User-Agent", Function.UserAgent)
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        //System.out.println(send.uri());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();

                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                            String s = new String(body, StandardCharsets.UTF_8);
                            s = s.replaceAll("https://", "/https/cookie:[]/");
                            body = s.getBytes(StandardCharsets.UTF_8);

                            //System.out.println(s);
                        }

                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                        send = null;
                        request = null;
                        method = null;
                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }

                } else if (ServiceName.equals("XVIDEOS.COM")) {
                    String targetURL = element.getAsJsonObject().get("VideoURL").getAsString();
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + targetURL);
                    logData.setResultURL(targetURL);
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);

                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(targetURL))
                                .headers("User-Agent", Function.UserAgent)
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        //System.out.println(send.uri());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();

                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                            String s = new String(body, StandardCharsets.UTF_8);

                            StringBuilder sb = new StringBuilder();

                            for (String str : s.split("\n")){
                                if (str.startsWith("#")){
                                    sb.append(str).append("\n");
                                    continue;
                                }

                                String s1 = targetURL.split("/")[targetURL.split("/").length - 1];
                                sb.append(targetURL.replaceAll(s1, "")).append(str).append("\n");
                                break;
                            }

                            s = sb.toString();
                            s = s.replaceAll("https://", "/https/cookie:[]/");

                            body = s.getBytes(StandardCharsets.UTF_8);
                            sb.setLength(0);
                            //System.out.println(s);
                        }

                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                        send = null;
                        request = null;
                        method = null;
                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }
                } else if (ServiceName.equals("Pornhub")) {
                    String targetURL = element.getAsJsonObject().get("VideoURL").getAsString();
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + targetURL);
                    logData.setResultURL(targetURL);
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);

                    try (HttpClient client = proxy == null ? HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build() :
                            HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]))))
                                    .build()) {

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(targetURL))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        //System.out.println(send.uri());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();

                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("application/x-mpegurl") || contentType.toLowerCase(Locale.ROOT).equals("audio/mpegurl")) {
                            String s = new String(body, StandardCharsets.UTF_8);

                            StringBuilder sb = new StringBuilder();

                            for (String str : s.split("\n")){
                                if (str.startsWith("#")){
                                    sb.append(str).append("\n");
                                    continue;
                                }

                                String s1 = targetURL.split("/")[targetURL.split("/").length - 1];
                                sb.append(targetURL.replaceAll(s1, "")).append(str).append("\n");
                                break;
                            }

                            s = sb.toString();
                            s = s.replaceAll("https://", "/https/cookie:[]/");

                            body = s.getBytes(StandardCharsets.UTF_8);
                            sb.setLength(0);
                            //System.out.println(s);
                        }

                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), send.statusCode(), contentType, body, method != null && method.equals("HEAD"));
                        send = null;
                        request = null;
                        method = null;
                    } catch (Exception e){
                        e.printStackTrace();
                        try {
                            content = null;
                            File file = new File("./error-video/error_000.mp4");
                            if (file.exists()){
                                FileInputStream stream = new FileInputStream(file);
                                content = stream.readAllBytes();
                                stream.close();
                                stream = null;
                            }

                            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                            content = null;
                        } catch (Exception ex){
                            // ex.printStackTrace();
                        }
                    }
                } else {
                    String targetURL = element.getAsJsonObject().has("VideoURL") ? element.getAsJsonObject().get("VideoURL").getAsString() : element.getAsJsonObject().get("LiveURL").getAsString();
                    System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> " + targetURL);
                    logData.setResultURL(targetURL);
                    Function.GetURLAccessLog.put(logData.getLogID(), logData);
                    OutputStream out = sock.getOutputStream();
                    StringBuilder sb_header = new StringBuilder();

                    sb_header.append("HTTP/").append(Function.getHTTPVersion(httpRequest)).append(" 302 Found\nLocation: ").append(targetURL).append("\n\n");
                    out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    out = null;
                    sb_header.setLength(0);
                    sb_header = null;
                }

                //Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "text/plain; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
            } else {
                System.out.println("[Get URL ("+(isCache ? "キャッシュ," : "")+Function.sdf.format(date)+")] " + URL + " ---> 対応してないサイトです");
                logData.setResultURL(null);
                Function.GetURLAccessLog.put(logData.getLogID(), logData);
                File file = new File("./error-video/error_404.mp4");
                if (file.exists()){
                    FileInputStream stream = new FileInputStream(file);
                    content = stream.readAllBytes();
                    stream.close();
                    stream = null;
                }
                //System.out.println(content.length);
                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                file = null;
                content = null;
            }
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

                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                content = null;
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
