package xyz.n7mn;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import xyz.n7mn.data.*;
import xyz.n7mn.nico_proxy.*;
import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;
import xyz.n7mn.nico_proxy.data.TokenJSON;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestFunction {


    public static VideoResult getURL(VideoRequest videoRequest, boolean isRedis) {
        final VideoResult videoResult = new VideoResult();
        final LogData logData = new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), videoRequest.getHTTPRequest(), videoRequest.getServerIP(), videoRequest.getRequestURL(), null, null);

        Matcher matcher_NicoVideoURL = Pattern.compile("(\\.nicovideo\\.jp|nico\\.ms)").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_BilibiliComURL = Pattern.compile("bilibili\\.com").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_BilibiliTvURL = Pattern.compile("bilibili\\.tv").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_YoutubeURL = Pattern.compile("(youtu\\.be|youtube\\.com)").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_XvideoURL = Pattern.compile("xvideo").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_TikTokURL = Pattern.compile("tiktok").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_TwitterURL = Pattern.compile("(x|twitter)\\.com/(.*)/status/(.*)").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_OpenrecURL = Pattern.compile("openrec").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_PornhubURL = Pattern.compile("pornhub\\.com").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_TwicastURL = Pattern.compile("twitcasting\\.tv").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_AbemaURL = Pattern.compile("abema\\.tv").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_TVerURL = Pattern.compile("tver\\.jp").matcher(videoRequest.getTempRequestURL());

        boolean isNico = matcher_NicoVideoURL.find();
        boolean isBiliBiliCom = matcher_BilibiliComURL.find();
        boolean isBiliBiliTv = matcher_BilibiliTvURL.find();
        boolean isYoutube = matcher_YoutubeURL.find();
        boolean isXvideo = matcher_XvideoURL.find();
        boolean isTiktok = matcher_TikTokURL.find();
        boolean isTwitter = matcher_TwitterURL.find();
        boolean isOpenrec = matcher_OpenrecURL.find();
        boolean isPornhub = matcher_PornhubURL.find();
        boolean isTwicast = matcher_TwicastURL.find();
        boolean isAbema = matcher_AbemaURL.find();
        boolean isTVer = matcher_TVerURL.find();

        final ShareService service;

        final List<String> proxyList = new ArrayList<>();

        if (isNico) {
            service = new NicoNicoVideo();
            if (Pattern.compile("(lv|so)").matcher(videoRequest.getTempRequestURL()).find()) {
                proxyList.addAll(videoRequest.getProxyListOfficial());
            } else {
                proxyList.addAll(videoRequest.getProxyListVideo());
            }
        } else if (isBiliBiliCom) {
            service = new BilibiliCom();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isBiliBiliTv) {
            service = new BilibiliTv();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isYoutube) {
            service = new YoutubeHLS();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isXvideo) {
            service = new Xvideos();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isTiktok) {
            service = new TikTok();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isTwitter) {
            service = new Twitter();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isOpenrec) {
            service = new OPENREC();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isPornhub) {
            service = new Pornhub();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isTwicast) {
            proxyList.addAll(videoRequest.getProxyListVideo());
            service = new Twicast(videoRequest.getTwitcastClientId(), videoRequest.getTwitcastClientSecret());
        } else if (isAbema) {
            proxyList.addAll(videoRequest.getProxyListVideo());
            service = new Abema();
        } else if (isTVer) {
            proxyList.addAll(videoRequest.getProxyListVideo());
            service = new TVer();
        } else {
            ShareService t;
            proxyList.addAll(videoRequest.getProxyListVideo());
            try {
                final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                String[] split = !videoRequest.getProxyListVideo().isEmpty() ? videoRequest.getProxyListVideo().get(new SecureRandom().nextInt(0, videoRequest.getProxyListVideo().size())).split(":") : null;
                final OkHttpClient client = split != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split[0], Integer.parseInt(split[1])))).build() : new OkHttpClient();

                Request img = new Request.Builder()
                        .url(videoRequest.getTempRequestURL())
                        .build();
                Response response_img = client.newCall(img).execute();

                if (response_img.body() != null && response_img.body().contentType().toString().startsWith("image")) {
                    t = new Image();
                } else {
                    t = null;
                }
                response_img.close();
            } catch (Exception e) {
                t = null;
            }

            service = t;
        }

        if (service == null) {
            videoResult.setErrorMessage("Not Found");
            logData.setErrorMessage("Not Found");
            new Thread(() -> LogWrite(logData, isRedis)).start();
            return videoResult;
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        String[] split = !proxyList.isEmpty() ? proxyList.get(new SecureRandom().nextInt(0, proxyList.size())).split(":") : null;
        final OkHttpClient client = !proxyList.isEmpty() ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split[0], Integer.parseInt(split[1])))).build() : new OkHttpClient();

        // ニコ動 / ニコ生
        if (isNico) {
            ResultVideoData data = null;

            try {
                if (Pattern.compile("lv|live").matcher(videoRequest.getTempRequestURL()).find()) {
                    data = service.getLive(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                } else {
                    data = service.getVideo(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                }
            } catch (Exception e) {
                logData.setErrorMessage(e.getMessage());
                videoResult.setErrorMessage(e.getMessage());
            }

            if (data == null) {
                new Thread(() -> LogWrite(logData, isRedis)).start();
                return videoResult;
            }

            logData.setResultURL(data.getVideoURL());
            videoResult.setResultURL(data.getVideoURL());

            if (data.isEncrypted()) {
                try {
                    // TODO: 後でUDP通信を使ったものに書き換える
                    OkHttpClient client2 = new OkHttpClient();
                    Request m3u8 = new Request.Builder()
                            .url("https://n.nicovrc.net/?url=" + data.getVideoURL() + "&proxy=" + (split != null ? split[0] + ":" + split[1] : ""))
                            .build();

                    Response response = client2.newCall(m3u8).execute();
                    String s1 = response.body() != null && response.code() == 200 ? response.body().string() : "";
                    response.close();
                    if (s1.startsWith("/")) {
                        logData.setResultURL("https://n.nicovrc.net" + s1);
                        videoResult.setResultURL("https://n.nicovrc.net" + s1);
                    }
                } catch (Exception e) {
                    logData.setResultURL(null);
                    videoResult.setResultURL(null);
                    logData.setErrorMessage(e.getMessage());
                }
            }

            if (!data.isStream()) {
                ResultVideoData finalData = data;
                new Thread(() -> {
                    try {
                        // ハートビート信号
                        Request request_html = new Request.Builder()
                                .url(videoRequest.getTempRequestURL())
                                .build();
                        Response response1 = client.newCall(request_html).execute();
                        String HtmlText;
                        if (response1.body() != null) {
                            HtmlText = response1.body().string();
                        } else {
                            HtmlText = "";
                        }
                        response1.close();

                        Matcher matcher_video = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">").matcher(HtmlText);

                        final long videoTime;
                        if (matcher_video.find()) {
                            videoTime = Long.parseLong(matcher_video.group(1));
                        } else {
                            videoTime = 3600L;
                        }

                        TokenJSON json = new Gson().fromJson(finalData.getTokenJson(), TokenJSON.class);

                        Timer timer = new Timer();
                        int[] count = new int[]{0};
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                if (count[0] > (videoTime / 40L)) {
                                    timer.cancel();
                                    return;
                                }

                                RequestBody body = RequestBody.create(json.getTokenValue(), MediaType.get("application/json; charset=utf-8"));
                                Request request1 = new Request.Builder()
                                        .url(json.getTokenSendURL())
                                        .post(body)
                                        .build();
                                try {
                                    Response response1 = client.newCall(request1).execute();
                                    //System.out.println(response.body().string());
                                    response1.close();
                                } catch (IOException e) {
                                    // e.printStackTrace();
                                    count[0]++;
                                    return;
                                }

                                count[0]++;
                            }
                        }, 0L, 40000L);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }).start();
            }

        } else if (isBiliBiliCom || isBiliBiliTv) {
            // bilibili
            try {
                ResultVideoData video;
                if (split != null) {
                    video = service.getVideo(new RequestVideoData(videoRequest.getTempRequestURL(), new ProxyData(split[0], Integer.parseInt(split[1]))));
                } else {
                    video = service.getVideo(new RequestVideoData(videoRequest.getTempRequestURL(), null));
                }

                // TODO: 後でUDP通信を使ったものに書き換える
                // 映像と音声で分離しているので結合処理が必要
                String bilibiliSystemURL = "";
                try {
                    YamlMapping mapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                    bilibiliSystemURL = mapping.string("BiliBliSystem");
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                final Request m3u8;
                final String bi = isBiliBiliCom ? "com" : "tv";
                if (split != null) {
                    //System.out.println(bilibiliSystemURL+"/?url="+video.getVideoURL()+",,"+video.getAudioURL()+"&cc&" + (m.find() ? "tv" : "com")+"&cc&"+split[0]+":"+split[1]);
                    m3u8 = new Request.Builder()
                            .url(bilibiliSystemURL + "/?url=" + video.getVideoURL() + ",," + video.getAudioURL() + "&cc&" + bi + "&cc&" + split[0] + ":" + split[1])
                            .build();
                } else {
                    //System.out.println(bilibiliSystemURL+"/?url="+video.getVideoURL()+",,"+video.getAudioURL()+"&cc&" + (isTv ? "tv" : "com"));
                    m3u8 = new Request.Builder()
                            .url(bilibiliSystemURL + "/?url=" + video.getVideoURL() + ",," + video.getAudioURL() + "&&cc&" + bi)
                            .build();
                }

                Response response = client.newCall(m3u8).execute();
                String s1 = response.body() != null ? response.body().string() : "";
                response.close();
                logData.setResultURL(bilibiliSystemURL + s1);
                videoResult.setResultURL(bilibiliSystemURL + s1);

            } catch (Exception e) {
                logData.setErrorMessage(e.getMessage());
                logData.setResultURL(null);
                videoResult.setErrorMessage(e.getMessage());
                videoResult.setResultURL(null);
            }
        } else if (isXvideo || isTiktok || isTwitter || isPornhub || isAbema || isTVer) {
            // xvideos / TikTok / Twitter / Pornhub / Ameba / TVer
            try {
                ResultVideoData video;
                if (isAbema && Pattern.compile("https://abema\\.tv/now-on-air/(.+)").matcher(videoRequest.getTempRequestURL()).find()){
                    video = service.getLive(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                } else if (isTVer && Pattern.compile("https://tver\\.jp/live/(.+)").matcher(videoRequest.getTempRequestURL()).find()) {
                    video = service.getLive(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                } else {
                    video = service.getVideo(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                }

                if (isTwitter) {
                    logData.setResultURL(video.getVideoURL().split("\\?")[0]);
                    videoResult.setResultURL(video.getVideoURL().split("\\?")[0]);
                } else {
                    logData.setResultURL(video.getVideoURL());
                    videoResult.setResultURL(video.getVideoURL());
                }

            } catch (Exception e) {
                logData.setErrorMessage(e.getMessage());
                logData.setResultURL(null);
                videoResult.setErrorMessage(e.getMessage());
                videoResult.setResultURL(null);
            }
        } else if (isOpenrec) {
            Matcher m = Pattern.compile("live").matcher(videoRequest.getTempRequestURL());

            try {
                ResultVideoData video = m.find() ? service.getLive(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null)) : service.getVideo(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));
                //videoUrl = video.getVideoURL();

                String proxy = "null";
                if (split != null) {
                    proxy = split[0] + ":" + split[1];
                }

                // TODO: 後でUDP通信を使ったものに書き換える
                String isStream = video.isStream() ? "true" : "false";
                Request m3u8 = new Request.Builder()
                        .url("https://nicovrc.net/rec/?isStream=" + isStream + "&u=" + video.getVideoURL() + "&proxy=" + proxy)
                        .build();

                Response response = client.newCall(m3u8).execute();
                logData.setResultURL(response.body() != null ? response.body().string() : "");
                videoResult.setResultURL(response.body() != null ? response.body().string() : "");
                response.close();

            } catch (Exception e) {
                logData.setErrorMessage(e.getMessage());
                videoResult.setErrorMessage(e.getMessage());
            }
        } else if (isTwicast) {
            // ツイキャス
            try {
                ResultVideoData video = service.getLive(new RequestVideoData(videoRequest.getTempRequestURL(), split != null ? new ProxyData(split[0], Integer.parseInt(split[1])) : null));

                if (video.isStream()) {
                    logData.setResultURL(video.getVideoURL());
                    videoResult.setResultURL(video.getVideoURL());
                } else {
                    // アーカイブはReferer付きじゃないとアクセスできないので
                    Request twicast = new Request.Builder()
                            .url(video.getVideoURL())
                            .addHeader("Referer", "https://twitcasting.tv/")
                            .build();

                    Response response_twicast = client.newCall(twicast).execute();

                    if (response_twicast.code() == 200) {
                        if (response_twicast.body() != null) {

                            Matcher tempUrl = Pattern.compile("https://(.+)/tc.vod.v2").matcher(video.getVideoURL());

                            String baseUrl = "";
                            if (tempUrl.find()) {
                                baseUrl = "https://" + tempUrl.group(1);
                            }

                            String str = response_twicast.body().string();
                            for (String s : str.split("\n")) {
                                if (s.startsWith("#")) {
                                    continue;
                                }

                                logData.setResultURL(baseUrl + s);
                                videoResult.setResultURL(baseUrl + s);
                                break;
                            }
                        }
                    }
                    response_twicast.close();
                }
            } catch (Exception e) {
                logData.setResultURL(null);
                videoResult.setResultURL(null);
                logData.setErrorMessage(e.getMessage());
                videoResult.setErrorMessage(e.getMessage());
            }
        } else if (isYoutube){
            logData.setResultURL(videoRequest.getTempRequestURL());
            videoResult.setResultURL(videoRequest.getTempRequestURL());
            logData.setErrorMessage(null);
            videoResult.setErrorMessage(null);
        } else {
            // 画像
            ResultVideoData video;
            try {
                if (split != null){
                    video = service.getVideo(new RequestVideoData(videoRequest.getTempRequestURL(), new ProxyData(split[0], Integer.parseInt(split[1]))));
                } else {
                    video = service.getVideo(new RequestVideoData(videoRequest.getTempRequestURL(), null));
                }
                logData.setResultURL(video.getVideoURL());
                videoResult.setResultURL(video.getVideoURL());
            } catch (Exception e){
                logData.setErrorMessage(e.getMessage());
                videoResult.setErrorMessage(e.getMessage());
            }
        }

        new Thread(()-> LogWrite(logData, isRedis)).start();
        return videoResult;
    }

    public static VideoResult getTitle(VideoRequest videoRequest, boolean isRedis){
        final VideoResult videoResult = new VideoResult();
        final LogData logData = new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), videoRequest.getHTTPRequest(), videoRequest.getServerIP(), videoRequest.getRequestURL(), null, null);

        Matcher matcher_NicoVideoURL = Pattern.compile("(\\.nicovideo\\.jp|nico\\.ms)").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_BilibiliComURL = Pattern.compile("bilibili\\.com").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_BilibiliTvURL = Pattern.compile("bilibili\\.tv").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_XvideoURL = Pattern.compile("xvideo").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_TikTokURL = Pattern.compile("tiktok").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_OpenrecURL = Pattern.compile("openrec").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_PornhubURL = Pattern.compile("pornhub\\.com").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_TwicastURL = Pattern.compile("twitcasting\\.tv").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_AbemaURL = Pattern.compile("abema\\.tv").matcher(videoRequest.getTempRequestURL());
        Matcher matcher_YoutubeURL = Pattern.compile("(youtu\\.be|youtube\\.com)").matcher(videoRequest.getTempRequestURL());

        boolean isNico = matcher_NicoVideoURL.find();
        boolean isBiliBiliCom = matcher_BilibiliComURL.find();
        boolean isBiliBiliTv = matcher_BilibiliTvURL.find();
        boolean isXvideo = matcher_XvideoURL.find();
        boolean isTiktok = matcher_TikTokURL.find();
        boolean isOpenrec = matcher_OpenrecURL.find();
        boolean isPornhub = matcher_PornhubURL.find();
        boolean isTwicast = matcher_TwicastURL.find();
        boolean isAbema = matcher_AbemaURL.find();
        boolean isYoutube = matcher_YoutubeURL.find();

        List<String> proxyList = new ArrayList<>();
        ShareService service = null;
        if (isNico){
            service = new NicoNicoVideo();
            if (Pattern.compile("(lv|so)").matcher(videoRequest.getTempRequestURL()).find()){
                proxyList.addAll(videoRequest.getProxyListOfficial());
            } else {
                proxyList.addAll(videoRequest.getProxyListVideo());
            }
        } else if (isBiliBiliCom){
            service = new BilibiliCom();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isBiliBiliTv){
            service = new BilibiliTv();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isXvideo){
            service = new Xvideos();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isTiktok){
            service = new TikTok();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isOpenrec){
            service = new OPENREC();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isPornhub){
            service = new Pornhub();
            proxyList.addAll(videoRequest.getProxyListVideo());
        } else if (isTwicast){
            proxyList.addAll(videoRequest.getProxyListVideo());
            service = new Twicast(videoRequest.getTwitcastClientId(), videoRequest.getTwitcastClientSecret());
        } else if (isAbema){
            proxyList.addAll(videoRequest.getProxyListVideo());
            service = new Abema();
        } else if (isYoutube){
            proxyList.addAll(videoRequest.getProxyListVideo());
            service = new YoutubeHLS();
        }

        if (isNico || isBiliBiliCom || isBiliBiliTv || isXvideo || isTiktok || isOpenrec || isPornhub || isTwicast || isAbema || isYoutube){
            String[] split = !proxyList.isEmpty() ? proxyList.get(new SecureRandom().nextInt(0, proxyList.size())).split(":") : null;
            String title;
            try {
                if (split != null){
                    title = service.getTitle(new RequestVideoData(videoRequest.getTempRequestURL(), new ProxyData(split[0], Integer.parseInt(split[1]))));
                } else {
                    title = service.getTitle(new RequestVideoData(videoRequest.getTempRequestURL(), null));
                }
            } catch (Exception e){
                title = "";
                logData.setErrorMessage(e.getMessage());
            }
            logData.setResultURL("Title : "+title);
            videoResult.setTitle(title);
        }

        new Thread(()-> LogWrite(logData, isRedis)).start();
        return videoResult;
    }

    public static void LogWrite(LogData data, boolean isRedis){
        new Thread(() -> {
            if (isRedis) {
                // Redis
                File config = new File("./config.yml");
                YamlMapping ConfigYml = null;

                try {
                    ConfigYml = Yaml.createYamlInput(config).readYamlMapping();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
                    Jedis jedis = jedisPool.getResource();
                    if (!ConfigYml.string("RedisPass").isEmpty()){
                        jedis.auth(ConfigYml.string("RedisPass"));
                    }

                    jedis.set("nico-proxy:ExecuteLog:"+data.getLogID(), new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data));

                    jedis.close();
                    jedisPool.close();
                } catch (Exception e){
                    // 5秒後に再試行。それでもだめならファイルに保存
                    try {
                        Thread.sleep(5000L);
                    } catch (Exception ex){
                        //ex.printStackTrace();
                    }
                    try {
                        JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
                        Jedis jedis = jedisPool.getResource();
                        if (!ConfigYml.string("RedisPass").isEmpty()) {
                            jedis.auth(ConfigYml.string("RedisPass"));
                        }

                        jedis.set("nico-proxy:ExecuteLog:" + data.getLogID(), new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data));

                        jedis.close();
                        jedisPool.close();
                    } catch (Exception exx){
                        if (!new File("./log").exists()) {
                            new File("./log").mkdir();
                        }

                        try {
                            File file = new File("./log/" + data.getLogID() + ".json");
                            file.createNewFile();
                            PrintWriter writer = new PrintWriter(file);
                            writer.print(new Gson().toJson(data));
                            writer.close();
                        } catch (Exception exxx) {
                            System.out.println("---- " + data.getLogID() + ".json ----");
                            System.out.println(new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data));
                            System.out.println("---- " + data.getLogID() + ".json ----");
                        }
                    }
                }

            } else {
                // ファイル
                if (!new File("./log").exists()) {
                    new File("./log").mkdir();
                }

                try {
                    File file = new File("./log/" + data.getLogID() + ".json");
                    file.createNewFile();
                    PrintWriter writer = new PrintWriter(file);
                    writer.print(new Gson().toJson(data));
                    writer.close();
                } catch (Exception e) {
                    System.out.println("---- " + data.getLogID() + ".json ----");
                    System.out.println(new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data));
                    System.out.println("---- " + data.getLogID() + ".json ----");
                }
            }
        }).start();
    }
}
