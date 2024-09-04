package net.nicovrc.dev.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.nicovrc.dev.data.*;
import okhttp3.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import xyz.n7mn.nico_proxy.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionAPI {

    private static final String ver = Constant.getVersion();

    private final ProxyAPI proxyAPI;
    private final String SocketIP;
    private final ConcurrentHashMap<String, LogData> LogDataList = new ConcurrentHashMap<>();
    private final List<String> ServiceURLList = new ArrayList<>();

    private final Pattern matcher_errorNico = Pattern.compile("fail-message");
    //private final Pattern matcher_maintenanceNico = Pattern.compile("(メンテナンス|[m|M]aintenance)");

    private final Pattern matcher_YoutubeID = Pattern.compile("\\?v=(.+)");

    private final Pattern matcher_NicoVideo = Pattern.compile("sm|nm|am|fz|ut|dm");
    private final Pattern matcher_NicoVideo2 = Pattern.compile("so|ax|ca|cd|cw|fx|ig|na|om|sd|sk|yk|yo|za|zb|zc|zd|ze|nl|watch/(\\d+)|^(\\d+)");
    private final Pattern matcher_NicoLive = Pattern.compile("lv|ch|kl");
    private final Pattern matcher_dmcnico = Pattern.compile("dmc\\.nico");
    private final Pattern matcher_cloudfrontNico = Pattern.compile("\\.cloudfront\\.net");

    private final Pattern matcher_bilibiliDuration = Pattern.compile("\"dash\":\\{\"duration\":(\\d+)");

    private final Pattern matcher_abemaURL = Pattern.compile("https://abema\\.tv/now-on-air/(.+)");
    private final Pattern matcher_TVerLiveURL = Pattern.compile("https://tver\\.jp/live/(.+)");
    private final Pattern matcher_TVerOlympicLive = Pattern.compile("par-jba-live-f\\.ovpobs\\.tv");
    private final Pattern matcher_TwicastArchiveURL = Pattern.compile("https://(.+)/tc\\.vod\\.v2");

    private final Pattern matcher_OPENRECLiveURL = Pattern.compile("https://(.+)\\.cloudfront\\.net");

    private final Pattern matcher_Vimeo1 = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio-high\",NAME=\"Original\",DEFAULT=YES,AUTOSELECT=YES,CHANNELS=\"2\",URI=\"(.+)\"");
    private final Pattern matcher_Vimeo2 = Pattern.compile("#EXT-X-STREAM-INF:CLOSED-CAPTIONS=(.+),BANDWIDTH=(\\d+),AVERAGE-BANDWIDTH=(\\d+),RESOLUTION=(.+),FRAME-RATE=(.+),CODECS=\"(.+)\",AUDIO=\"(.+)\"");

    private final Pattern matcher_Access = Pattern.compile("\\?access=(.+) HTTP");

    private final Pattern matcher_NicoNicoVideoURL = Pattern.compile("(nico\\.ms|nicovideo\\.jp)");
    private final Pattern matcher_BiliBiliComURL = Pattern.compile("bilibili\\.com");
    private final Pattern matcher_BiliBiliTvURL = Pattern.compile("bilibili\\.tv");
    private final Pattern matcher_YoutubeURL = Pattern.compile("(youtu\\.be|youtube\\.com)");
    private final Pattern matcher_XVIDEOSURL = Pattern.compile("xvideos\\.com");
    private final Pattern matcher_TikTokURL = Pattern.compile("tiktok\\.com");
    private final Pattern matcher_TwitterURL = Pattern.compile("(x|twitter)\\.com/(.*)/status/(.*)");
    private final Pattern matcher_OPENRECURL = Pattern.compile("openrec\\.tv");
    private final Pattern matcher_PornhubURL = Pattern.compile("pornhub\\.com");
    private final Pattern matcher_TwitcastingURL = Pattern.compile("twitcasting\\.tv");
    private final Pattern matcher_AbemaURL = Pattern.compile("abema\\.tv");
    private final Pattern matcher_TVerURL = Pattern.compile("tver\\.jp");
    private final Pattern matcher_TVerURL2 = Pattern.compile("tver\\.jp/olympic");
    private final Pattern matcher_iwaraURL = Pattern.compile("iwara\\.tv");
    private final Pattern matcher_piaproURL = Pattern.compile("piapro\\.jp");
    private final Pattern matcher_SoundCloudURL = Pattern.compile("soundcloud\\.com");
    private final Pattern matcher_VimeoURL = Pattern.compile("vimeo\\.com");
    private final Pattern matcher_fc2VideoURL = Pattern.compile("video\\.fc2\\.com");
    private final Pattern matcher_fc2VideoAdultURL = Pattern.compile("video\\.fc2\\.com/a");
    private final Pattern matcher_fc2LiveURL = Pattern.compile("live\\.fc2\\.com");
    private final Pattern matcher_youjizzURL = Pattern.compile("youjizz\\.com");
    private final Pattern matcher_sonicbowlURL = Pattern.compile("player\\.sonicbowl\\.cloud");
    private final Pattern matcher_mixcloudURL = Pattern.compile("mixcloud\\.com");
    private final Pattern matcher_bandcampURL = Pattern.compile("bandcamp\\.com");
    private final Pattern matcher_SpankBangURL = Pattern.compile("spankbang\\.com");
    private final Pattern matcher_PeerTube = Pattern.compile("window\\.PeerTubeServerConfig");

    public ConversionAPI(ProxyAPI proxyAPI){
        this.proxyAPI = proxyAPI;
        String temp = null;
        try {
            //
            OkHttpClient client = new OkHttpClient();
            Request ip = new Request.Builder()
                    .url("https://ipinfo.io/ip")
                    .build();
            Response response = client.newCall(ip).execute();
            if (response.body() != null){
                temp = response.body().string();
            }
            response.close();
        } catch (Exception e){
            temp = null;
        }
        SocketIP = temp;

        // 1hおきにログ出力をする
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ForceLogDataWrite();
            }
        }, 0L, 3600000L);

        // 対応サービスリスト
        ServiceURLList.add("nicovideo.jp");
        ServiceURLList.add("nico.ms");
        ServiceURLList.add("bilibili.com");
        ServiceURLList.add("bilibili.tv");
        ServiceURLList.add("youtu.be");
        ServiceURLList.add("youtube.com");
        ServiceURLList.add("xvideos.com");
        ServiceURLList.add("tiktok.com");
        ServiceURLList.add("x.com");
        ServiceURLList.add("twitter.com");
        ServiceURLList.add("openrec.tv");
        ServiceURLList.add("pornhub.com");
        ServiceURLList.add("twitcasting.tv");
        ServiceURLList.add("abema.tv");
        ServiceURLList.add("abema.app");
        ServiceURLList.add("tver.jp");
        ServiceURLList.add("iwara.tv");
        ServiceURLList.add("piapro.jp");
        ServiceURLList.add("soundcloud.com");
        ServiceURLList.add("vimeo.com");
        ServiceURLList.add("video.fc2.com");
        ServiceURLList.add("live.fc2.com");
        ServiceURLList.add("youjizz.com");
        ServiceURLList.add("player.sonicbowl.cloud");
        ServiceURLList.add("mixcloud.com");
        ServiceURLList.add("bandcamp.com");
        ServiceURLList.add("spankbang.com");
    }

    /**
     * @param HTTPRequest 生のHTTPリクエストorUDPリクエスト
     * @param RequestURL 事前変換前のURL
     * @param TempRequestURL 事前変換後のURL(陣内システムがついていたら除去したあとのURL)
     * @param isTitleGet タイトル取得する場合はtrue
     * @return 処理結果のURL
     * @throws Exception エラーメッセージ(#getMessageで取得してエラー画面に表示する想定)
     */
    public String get(String HTTPRequest, String RequestURL, String TempRequestURL, boolean isTitleGet) throws Exception {
        //System.out.println("Debug : " + TempRequestURL);
        String result = null;

        final String request;

        if (HTTPRequest == null){
            request = new Gson().toJson(new UDPServerAccessLog(RequestURL, TempRequestURL, isTitleGet));
        } else {
            request = HTTPRequest;
        }
        String ErrorMessage;

        ShareService Service = getService(TempRequestURL);
        String ServiceName = null;

        if (Service != null){
            ServiceName = Service.getServiceName();
        }

        if (ServiceName == null){
            ServiceName = getServiceName(TempRequestURL);
        }
        //System.out.println("Debug1-1 : " + ServiceName);

        Matcher matcher_a = matcher_Access.matcher(request);
        boolean isNotVRC = false;
        if (matcher_a.find()){
            if (matcher_a.group(1).equals("no_vrc")){
                isNotVRC = true;
            }
        }
        //System.out.println(isNotVRC);

        try {
            if (Service == null){
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, null, null))).start();
                return null;
            }
            if (ServiceName == null){
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, null, null))).start();
                return null;
            }
            //System.out.println("Debug1-2 : " + ServiceName);

            final List<ProxyData> list = proxyAPI.getMainProxyList();
            final List<ProxyData> list_jp = proxyAPI.getJPProxyList();
            int main_count = list.isEmpty() ? 0 : (list.size() > 1 ? new SecureRandom().nextInt(0, list.size()) : 0);
            int jp_count = list_jp.isEmpty() ? 0 : (list_jp.size() > 1 ? new SecureRandom().nextInt(0, list_jp.size()) : 0);
            //System.out.println("Debug1-3 : " + ServiceName);

            final ProxyData proxyData = list.isEmpty() ? null : new ProxyData(list.get(main_count).getIP(), list.get(main_count).getPort());
            final ProxyData proxyData_jp = list_jp.isEmpty() ? null : new ProxyData(list_jp.get(jp_count).getIP(), list_jp.get(jp_count).getPort());
            boolean isUseJPProxy = false;

            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
            OkHttpClient client = proxyData == null ? new OkHttpClient() : builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(list.get(main_count).getIP(), list.get(main_count).getPort()))).build();

            if (!ServiceName.startsWith("動画") && !ServiceName.startsWith("画像")){
                Request img = new Request.Builder()
                        .url(TempRequestURL)
                        .build();
                Response response = client.newCall(img).execute();
                if (response.body() != null){
                    String temp = response.body().string();
                    //System.out.println(temp);
                    isUseJPProxy = matcher_errorNico.matcher(temp).find();
                    if (!matcher_errorNico.matcher(temp).find()) {
                        isUseJPProxy = matcher_TVerURL.matcher(TempRequestURL).find();
                    }
                    if (!matcher_TVerURL.matcher(temp).find()) {
                        isUseJPProxy = matcher_AbemaURL.matcher(TempRequestURL).find();
                    }
                }

                response.close();
            }


            //System.out.println(isUseJPProxy);
            //System.out.println(proxyData_jp.getProxyIP());

            if (isTitleGet){
                final String title = Service.getTitle(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, title, null))).start();
                return title;
            }

            //System.out.println("Debug3 : "+TempRequestURL);
            Matcher matcher1 = matcher_YoutubeID.matcher(TempRequestURL);
            boolean b = matcher1.find();

            if (!b){
                TempRequestURL = TempRequestURL.split("\\?")[0];
            } else if (matcher_NicoNicoVideoURL.matcher(TempRequestURL).find()) {
                TempRequestURL = "https://nico.ms/"+matcher1.group(1);
            } else {
                TempRequestURL = "https://youtu.be/"+matcher1.group(1);
            }

            ResultVideoData video;
            //System.out.println("debug : " + TempRequestURL);
            //System.out.println("debug : " + ServiceName);
            if (ServiceName.equals("ニコニコ動画") || ServiceName.equals("ニコニコ動画(Re:仮)")){

                if (matcher_NicoVideo.matcher(TempRequestURL).find()){
                    // 通常動画
                    video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                } else if (matcher_NicoVideo2.matcher(TempRequestURL).find()){
                    // 公式動画 or 配信
                    try {
                        video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                    } catch (Exception e){
                        if (e.getMessage().equals("www.nicovideo.jp Not Found")){
                            try {
                                video = Service.getLive(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                            } catch (Exception ex){
                                if (ex.getMessage().equals("live.nicovideo.jp No WebSocket Found")){

                                    throw new Exception("対応していない動画または配信です");
                                } else {
                                    throw e;
                                }
                            }
                        } else {
                            throw e;
                        }
                    }
                } else if (matcher_NicoLive.matcher(TempRequestURL).find()) {
                    // 配信
                    try {
                        video = Service.getLive(new RequestVideoData(TempRequestURL, proxyData));
                    } catch (Exception e){
                        video = Service.getLive(new RequestVideoData(TempRequestURL, proxyData_jp));
                    }
                } else {
                    throw new Exception("対応していない動画または配信です。\n※URLが間違っていないか再度確認ください。");
                }

                //System.out.println("debug : "+video.getVideoURL());
                if (matcher_dmcnico.matcher(video.getVideoURL()).find() || matcher_cloudfrontNico.matcher(video.getVideoURL()).find()) {
                    final ResultVideoData finalVideo = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();
                    System.gc();
                    return video.getVideoURL();
                }

                NicoVideoInputData nicoVideoInputData = new NicoVideoInputData();
                nicoVideoInputData.setVideoURL(video.getVideoURL());
                nicoVideoInputData.setAudioURL(video.getAudioURL());
                nicoVideoInputData.setCookie(video.getTokenJson());
                nicoVideoInputData.setVRC(!isNotVRC);
                //System.out.println(!isNotVRC);

                nicoVideoInputData.setProxy(null);
                if (proxyData != null && !isUseJPProxy){
                    nicoVideoInputData.setProxy(proxyData.getProxyIP() + ":" + proxyData.getPort());
                }
                if (proxyData_jp != null && isUseJPProxy){
                    nicoVideoInputData.setProxy(proxyData_jp.getProxyIP() + ":" + proxyData_jp.getPort());
                }

                // HLSがちゃんと再生できるかチェック
                client = nicoVideoInputData.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(nicoVideoInputData.getProxy().split(":")[0], Integer.parseInt(nicoVideoInputData.getProxy().split(":")[1])))).build() : new OkHttpClient();

                JsonElement json = new Gson().fromJson(video.getTokenJson(), JsonElement.class);

                final Pattern matcher_keyUri = Pattern.compile("#EXT-X-KEY:METHOD=AES-128,URI=\"(.+)\",IV=(.+)");

                boolean isOK = false;
                while (!isOK){
                    Request request0 = new Request.Builder()
                            .url(video.getVideoURL())
                            .addHeader("Cookie", "nicosid="+json.getAsJsonObject().get("nicosid").getAsString()+"; domand_bid=" + json.getAsJsonObject().get("domand_bid").getAsString())
                            .build();
                    Response response0 = client.newCall(request0).execute();

                    String keyUri = null;
                    if (response0.body() != null){
                        Matcher matcher = matcher_keyUri.matcher(response0.body().string());
                        if (matcher.find()){
                            keyUri = matcher.group(1);
                        }
                    }
                    response0.close();

                    Request request1 = new Request.Builder()
                            .url(keyUri)
                            .addHeader("Cookie", "nicosid="+json.getAsJsonObject().get("nicosid").getAsString()+"; domand_bid=" + json.getAsJsonObject().get("domand_bid").getAsString())
                            .build();
                    Response response1 = client.newCall(request1).execute();
                    if (response1.code() < 200 || response1.code() > 299) {
                        // 再取得
                        if (matcher_NicoVideo.matcher(TempRequestURL).find()) {
                            // 通常動画
                            video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                        } else if (matcher_NicoVideo2.matcher(TempRequestURL).find()) {
                            // 公式動画 or 配信
                            video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                        }

                        nicoVideoInputData.setVideoURL(video.getVideoURL());
                        nicoVideoInputData.setAudioURL(video.getAudioURL());
                        nicoVideoInputData.setCookie(video.getTokenJson());
                    } else {
                        isOK = true;
                    }
                    response1.close();

                    Request request0_2 = new Request.Builder()
                            .url(video.getAudioURL())
                            .addHeader("Cookie", "nicosid="+json.getAsJsonObject().get("nicosid").getAsString()+"; domand_bid=" + json.getAsJsonObject().get("domand_bid").getAsString())
                            .build();
                    Response response0_2 = client.newCall(request0_2).execute();

                    keyUri = null;
                    if (response0_2.body() != null){
                        Matcher matcher = matcher_keyUri.matcher(response0_2.body().string());
                        if (matcher.find()){
                            keyUri = matcher.group(1);
                        }
                    }
                    response0_2.close();

                    Request request1_2 = new Request.Builder()
                            .url(keyUri)
                            .addHeader("Cookie", "nicosid="+json.getAsJsonObject().get("nicosid").getAsString()+"; domand_bid=" + json.getAsJsonObject().get("domand_bid").getAsString())
                            .build();
                    Response response1_2 = client.newCall(request1_2).execute();
                    if (response1_2.code() < 200 || response1_2.code() > 299) {
                        // 再取得
                        if (matcher_NicoVideo.matcher(TempRequestURL).find()) {
                            // 通常動画
                            video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                        } else if (matcher_NicoVideo2.matcher(TempRequestURL).find()) {
                            // 公式動画 or 配信
                            video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                        }

                        nicoVideoInputData.setVideoURL(video.getVideoURL());
                        nicoVideoInputData.setAudioURL(video.getAudioURL());
                        nicoVideoInputData.setCookie(video.getTokenJson());
                    } else {
                        isOK = true;
                    }
                    response1_2.close();
                }


                String jsonText = new Gson().toJson(nicoVideoInputData);
                String SystemIP = "";
                try {
                    YamlMapping mapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                    SystemIP = mapping.string("NicoVideoSystem");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                //System.out.println("Debug : "+SystemIP);

                Socket sock = new Socket(SystemIP, 25250);
                sock.setSoTimeout(4000);
                OutputStream outputStream = sock.getOutputStream();
                InputStream inputStream = sock.getInputStream();
                outputStream.write(jsonText.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                byte[] bytes = inputStream.readAllBytes();
                sock.close();

                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, new String(bytes, StandardCharsets.UTF_8), null))).start();
                System.gc();
                return new String(bytes, StandardCharsets.UTF_8);
            }

            if (ServiceName.equals("bilibili.com") || ServiceName.equals("bilibili.tv")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                String bilibiliSystem = "";
                try {
                    YamlMapping mapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                    bilibiliSystem = mapping.string("BiliBiliSystemIP");
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                if (!bilibiliSystem.isEmpty()) {
                    BiliBiliInputData jsonText = new BiliBiliInputData();
                    jsonText.setSiteType(ServiceName.split("\\.")[1]);
                    jsonText.setVideoURL(video.getVideoURL());
                    jsonText.setAudioURL(video.getAudioURL());
                    jsonText.setVRC(!isNotVRC);
                    if (proxyData != null && !isUseJPProxy){
                        jsonText.setProxy(proxyData.getProxyIP() + ":" + proxyData.getPort());
                    }
                    if (proxyData_jp != null && isUseJPProxy){
                        jsonText.setProxy(proxyData_jp.getProxyIP() + ":" + proxyData_jp.getPort());
                    }

                    if (jsonText.getSiteType().equals("com")) {
                        long duration = 0;
                        Request request_html = new Request.Builder()
                                .url(TempRequestURL)
                                .build();
                        Response response1 = client.newCall(request_html).execute();
                        if (response1.body() != null) {
                            Matcher matcher = matcher_bilibiliDuration.matcher(response1.body().string());
                            if (matcher.find()) {
                                duration = Long.parseLong(matcher.group(1));
                            }
                        }
                        response1.close();
                        jsonText.setVideoDuration(duration);
                    }

                    String json = new Gson().toJson(jsonText);
                    Socket sock = new Socket(bilibiliSystem, 28279);
                    sock.setSoTimeout(4000);
                    OutputStream outputStream = sock.getOutputStream();
                    InputStream inputStream = sock.getInputStream();
                    outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();

                    byte[] bytes = inputStream.readAllBytes();
                    final String url = new String(bytes, StandardCharsets.UTF_8);

                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, url, null))).start();
                    System.gc();
                    return url;
                }
            }

            // xvideos / Twitter / Pornhub / Ameba / TVer
            if (ServiceName.equals("XVIDEOS.com") || ServiceName.equals("Twitter") || ServiceName.equals("Pornhub") || ServiceName.equals("Abema") || ServiceName.equals("TVer") || ServiceName.equals("TVer (オリンピック)")){
                if (ServiceName.equals("Abema") && matcher_abemaURL.matcher(TempRequestURL).find()){
                    video = Service.getLive(new RequestVideoData(TempRequestURL, proxyData_jp));

                    ResultVideoData finalVideo1 = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL(), null))).start();
                } else if (ServiceName.equals("TVer") && matcher_TVerLiveURL.matcher(TempRequestURL).find()) {
                    video = Service.getLive(new RequestVideoData(TempRequestURL, proxyData_jp));

                    ResultVideoData finalVideo1 = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL(), null))).start();
                } else if (ServiceName.equals("Pornhub")) {

                    //System.out.println(TempRequestURL);
                    String[] split = RequestURL.split("\\?");
                    //System.out.println(split[0] +"?"+ split[1]);
                    video = Service.getVideo(new RequestVideoData(split[0] +"?"+ split[1], proxyData));

                    ResultVideoData finalVideo1 = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL(), null))).start();
                } else if (ServiceName.equals("TVer")) {
                    video = Service.getVideo(new RequestVideoData(TempRequestURL, proxyData_jp));

                    ResultVideoData finalVideo1 = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL(), null))).start();

                } else if (ServiceName.equals("TVer (オリンピック)")){
                    video = Service.getLive(new RequestVideoData(TempRequestURL, proxyData_jp));

                    if (matcher_TVerOlympicLive.matcher(video.getVideoURL()).find()){
                        ResultVideoData finalVideo1 = video;
                        new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL().replaceAll("par-jba-live-f\\.ovpobs\\.tv", "tv.nicovrc.net"), null))).start();

                        return video.getVideoURL().replaceAll("par-jba-live-f\\.ovpobs\\.tv", "tv.nicovrc.net");
                    } else {
                        ResultVideoData finalVideo1 = video;
                        new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL(), null))).start();

                        return video.getVideoURL();
                    }


                } else {

                    video = Service.getVideo(new RequestVideoData(TempRequestURL, proxyData));

                    ResultVideoData finalVideo1 = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL(), null))).start();
                }



                if (ServiceName.equals("Twitter")) {
                    System.gc();
                    return video.getVideoURL().split("\\?")[0];
                } else {
                    System.gc();
                    return video.getVideoURL();
                }
            }

            // TikTok
            if (ServiceName.equals("TikTok")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, proxyData));

                ResultVideoData finalVideo1 = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, ("https://t.nicovrc.net/?url=" + URLEncoder.encode(finalVideo1.getVideoURL()+"&cookiee="+finalVideo1.getTokenJson(), StandardCharsets.UTF_8)), null))).start();

                return ("https://t.nicovrc.net/?url=" + URLEncoder.encode(video.getVideoURL()+"&cookiee="+video.getTokenJson(), StandardCharsets.UTF_8));
            }

            if (ServiceName.equals("ツイキャス")){
                video = Service.getLive(new RequestVideoData(TempRequestURL, proxyData));

                if (video.isStream()) {
                    ResultVideoData finalVideo1 = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo1.getVideoURL(), null))).start();

                    return video.getVideoURL();
                } else {
                    // アーカイブはReferer付きじゃないとアクセスできないので
                    Request twicast = new Request.Builder()
                            .url(video.getVideoURL())
                            .addHeader("Referer", "https://twitcasting.tv/")
                            .build();

                    Response response_twicast = client.newCall(twicast).execute();

                    if (response_twicast.code() == 200) {
                        if (response_twicast.body() != null) {

                            Matcher tempUrl = matcher_TwicastArchiveURL.matcher(video.getVideoURL());

                            String baseUrl = "";
                            if (tempUrl.find()) {
                                baseUrl = "https://" + tempUrl.group(1);
                            }

                            String str = response_twicast.body().string();
                            for (String s : str.split("\n")) {
                                if (s.startsWith("#")) {
                                    continue;
                                }

                                String s1 = baseUrl + s;
                                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, s1, null))).start();

                                return s1;
                            }
                        }
                    }
                    response_twicast.close();
                }
            }

            // OPENREC
            if (ServiceName.equals("Openrec")){
                try {
                    video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                } catch (Exception e){
                    video = Service.getLive(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                }

                Matcher matcher = matcher_OPENRECLiveURL.matcher(video.getVideoURL());
                if (matcher.find()){
                    String host = matcher.group(1) + ".cloudfront.net";

                    ResultVideoData finalVideo3 = video;
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo3.getVideoURL().replaceAll(host.replaceAll("\\.","\\\\."), "o.nicovrc.net") + "&host="+host, null))).start();

                    return video.getVideoURL().replaceAll(host.replaceAll("\\.","\\\\."), "o.nicovrc.net") + "&host="+host;
                } else {
                    throw new Exception("対応していない動画または配信です。\n※URLが間違っていないか再度確認ください。");
                }
            }

            // Iwara
            if (ServiceName.equals("iwara")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                ResultVideoData finalVideo2 = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo2.getVideoURL(), null))).start();
                return video.getVideoURL();

            }

            // piapro、SoundCloud
            if (ServiceName.equals("piapro") || ServiceName.equals("SoundCloud")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                ResultVideoData finalVideo2 = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo2.getAudioURL(), null))).start();
                return video.getAudioURL();
            }

            // vimeo
            if (ServiceName.equals("vimeo")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                String vimeoSystem = "";
                try {
                    YamlMapping mapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                    vimeoSystem = mapping.string("VimeoSystemIP");
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                if (!vimeoSystem.isEmpty()){

                    String temp = "";
                    Request tempRequest = new Request.Builder()
                            .url(video.getVideoURL())
                            .build();
                    Response response = client.newCall(tempRequest).execute();
                    if (response.body() != null) {
                        temp = response.body().string();
                    }
                    response.close();
                    VimeoData data = new VimeoData();

                    long bitrate = 0;
                    long aveBitRate = 0;
                    String Codecs = null;
                    String Resolution = null;
                    String FrameRate = null;
                    String Audio = null;

                    int bitrate_c = 0;
                    int i = 0;
                    String[] split = temp.split("\n");
                    for (String str : split){
                        Matcher matcher = matcher_Vimeo2.matcher(str);
                        if (matcher.find()){
                            String bit = matcher.group(2);
                            if (Long.parseLong(bit) >= bitrate){
                                bitrate = Long.parseLong(bit);
                                aveBitRate = Long.parseLong(matcher.group(3));
                                Resolution = matcher.group(4);
                                FrameRate = matcher.group(5);
                                Codecs = matcher.group(6);
                                Audio = matcher.group(7);
                                bitrate_c = i + 1;
                            }
                        }
                        i++;
                    }
                    data.setVideoURL(split[bitrate_c]);
                    Matcher matcher = matcher_Vimeo1.matcher(temp);
                    if (matcher.find()){
                        data.setAudioURL(matcher.group(1));
                    }
                    String[] s = video.getVideoURL().split("/");
                    data.setBaseURL("https://"+s[2]+"/"+s[3]+"/"+s[4]+"/"+s[5]);
                    data.setBandwidth(bitrate);
                    data.setAverageBandwidth(aveBitRate);
                    data.setResolution(Resolution);
                    data.setFrameRate(FrameRate);
                    data.setCodecs(Codecs);
                    data.setAudio(Audio);
                    data.setNovrc(isNotVRC);

                    String json = new Gson().toJson(data);
                    Socket sock = new Socket(vimeoSystem, 22222);
                    sock.setSoTimeout(4000);
                    OutputStream outputStream = sock.getOutputStream();
                    InputStream inputStream = sock.getInputStream();
                    outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();

                    byte[] bytes = inputStream.readAllBytes();
                    final String url = new String(bytes, StandardCharsets.UTF_8);

                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, url, null))).start();

                    return url;
                }
            }

            // fc2
            if (ServiceName.equals("FC2動画") || ServiceName.equals("FC2動画 アダルト")){
                if (matcher_fc2LiveURL.matcher(TempRequestURL).find()){
                    video = Service.getLive(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                } else {
                    video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                }

                final ResultVideoData finalVideo = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();
                return video.getVideoURL();
            }

            // PeerTube
            if (ServiceName.equals("PeerTube")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                final ResultVideoData finalVideo = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();

                return finalVideo.getVideoURL();
            }


            // Youjizz
            if (ServiceName.equals("Youjizz")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                final ResultVideoData finalVideo = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();

                return finalVideo.getVideoURL();
            }

            // Sonicbowl
            if (ServiceName.equals("Sonicbowl")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                final ResultVideoData finalVideo = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();

                return finalVideo.getAudioURL();
            }

            // Mixcloud
            if (ServiceName.equals("Mixcloud")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                final ResultVideoData finalVideo = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();

                return finalVideo.getAudioURL();
            }

            // bandcamp
            if (ServiceName.equals("bandcamp")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));

                final ResultVideoData finalVideo = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();

                return finalVideo.getAudioURL();
            }

            // SpankBang
            if (ServiceName.equals("SpankBang")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, proxyData_jp));

                final ResultVideoData finalVideo = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo.getVideoURL(), null))).start();

                return finalVideo.getVideoURL();
            }

            // Youtube
            if (ServiceName.equals("Youtube")){
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, "https://yt.8uro.net/r?v="+RequestURL, null))).start();
                return "https://yt.8uro.net/r?v="+RequestURL;
            }

            if (ServiceName.equals("画像") || ServiceName.equals("動画")){
                video = Service.getVideo(new RequestVideoData(TempRequestURL, null));

                ResultVideoData finalVideo2 = video;
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, finalVideo2.getVideoURL(), null))).start();
                return video.getVideoURL();
            }

            new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, null, null))).start();


        } catch (Exception e){
            ErrorMessage = ServiceName + " : " + e.getMessage();
            //e.printStackTrace();


            final String finalErrorMessage = ErrorMessage;
            new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), request, SocketIP, RequestURL, null, finalErrorMessage))).start();
            throw new Exception(ErrorMessage);
        }


        System.gc();
        return result;
    }

    /**
     * @param RequestURL 事前処理前URL
     * @param TempRequestURL 事前処理後URL
     * @param isTitleGet タイトル取得する場合はtrue
     * @return 処理後のURL
     * @throws Exception エラーメッセージ
     */
    public String get(String RequestURL, String TempRequestURL, boolean isTitleGet) throws Exception{
        return get(null, RequestURL, TempRequestURL, isTitleGet);
    }

    /**
     * @return 処理プログラムのバージョン
     */
    @Deprecated
    public static String getVer(){
        return ver;
    }

    /**
     * @param data ログデータ
     */
    public void LogWrite(LogData data){
        //System.out.println(data.getRequestIP());
        LogDataList.put(data.getLogID(), data);
    }

    /**
     * @param URL 処理するURL
     * @return 対応する処理サービスのオブジェクト
     */

    private ShareService getService(String URL){

        Matcher matcher_NicoVideoURL = matcher_NicoNicoVideoURL.matcher(URL);
        Matcher matcher_BilibiliComURL = matcher_BiliBiliComURL.matcher(URL);
        Matcher matcher_BilibiliTvURL = matcher_BiliBiliTvURL.matcher(URL);
        Matcher matcher_YoutubeURL = this.matcher_YoutubeURL.matcher(URL);
        Matcher matcher_XvideoURL = matcher_XVIDEOSURL.matcher(URL);
        Matcher matcher_TikTokURL = this.matcher_TikTokURL.matcher(URL);
        Matcher matcher_TwitterURL = this.matcher_TwitterURL.matcher(URL);
        Matcher matcher_OpenrecURL = matcher_OPENRECURL.matcher(URL);
        Matcher matcher_PornhubURL = this.matcher_PornhubURL.matcher(URL);
        Matcher matcher_TwicastURL = matcher_TwitcastingURL.matcher(URL);
        Matcher matcher_AbemaURL = this.matcher_AbemaURL.matcher(URL);
        Matcher matcher_TVerURL = this.matcher_TVerURL.matcher(URL);
        Matcher matcher_TVerURL2 = this.matcher_TVerURL2.matcher(URL);
        Matcher matcher_IwaraURL = matcher_iwaraURL.matcher(URL);
        Matcher matcher_PiaproURL = matcher_piaproURL.matcher(URL);
        Matcher matcher_SoundCloudURL = this.matcher_SoundCloudURL.matcher(URL);
        Matcher matcher_VimeoURL = this.matcher_VimeoURL.matcher(URL);
        Matcher matcher_fc2VideoURL = this.matcher_fc2VideoURL.matcher(URL);
        Matcher matcher_fc2VideoAdultURL = this.matcher_fc2VideoAdultURL.matcher(URL);
        Matcher matcher_fc2LiveURL = this.matcher_fc2LiveURL.matcher(URL);
        Matcher matcher_youjizzURL = this.matcher_youjizzURL.matcher(URL);
        Matcher matcher_sonicbowlURL = this.matcher_sonicbowlURL.matcher(URL);
        Matcher matcher_mixcloudURL = this.matcher_mixcloudURL.matcher(URL);
        Matcher matcher_bandcampURL = this.matcher_bandcampURL.matcher(URL);
        Matcher matcher_SpankBangURL = this.matcher_SpankBangURL.matcher(URL);

        if (matcher_NicoVideoURL.find()){
            return new NicoNicoVideo();
            //return new NicoNicoVideoRekari();
        }

        if (matcher_BilibiliComURL.find()){
            return new BilibiliCom();
        }

        if (matcher_BilibiliTvURL.find()){
            return new BilibiliTv();
        }

        if (matcher_YoutubeURL.find()){
            return new YoutubeHLS();
        }

        if (matcher_XvideoURL.find()){
            return new Xvideos();
        }

        if (matcher_TikTokURL.find()){
            return new TikTok();
        }

        if (matcher_TwitterURL.find()){
            return new Twitter();
        }

        if (matcher_OpenrecURL.find()){
            return new OPENREC();
        }

        if (matcher_PornhubURL.find()){
            return new Pornhub();
        }

        if (matcher_TwicastURL.find()){
            try {
                final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                return new Twicast(yamlMapping.string("ClientID"), yamlMapping.string("ClientSecret"));
            } catch (Exception e){
                return new Twicast("", "");
            }
        }

        if (matcher_AbemaURL.find()){
            return new Abema();
        }

        if (matcher_TVerURL2.find()){
            return new TVer_Olympic();
        }

        if (matcher_TVerURL.find()){
            return new TVer();
        }

        if (matcher_IwaraURL.find()){
            return new Iwara();
        }

        if (matcher_PiaproURL.find()){
            return new Piapro();
        }

        if (matcher_SoundCloudURL.find()){
            return new SoundCloud();
        }

        if (matcher_VimeoURL.find()){
            return new Vimeo();
        }

        if (matcher_fc2VideoAdultURL.find()){
            return new FC2Adult();
        }

        if (matcher_fc2VideoURL.find()){
            return new FC2();
        }

        if (matcher_fc2LiveURL.find()){
            return new FC2();
        }

        if (matcher_youjizzURL.find()){
            return new Youjizz();
        }

        if (matcher_sonicbowlURL.find()){
            return new Sonicbowl();
        }

        if (matcher_mixcloudURL.find()){
            return new Mixcloud();
        }

        if (matcher_bandcampURL.find()){
            return new Bandcamp();
        }

        if (matcher_SpankBangURL.find()){
            return new SpankBang();
        }

        try {
            List<ProxyData> list = proxyAPI.getMainProxyList();
            int i = list.isEmpty() ? 0 : new SecureRandom().nextInt(0, list.size() - 1);

            final OkHttpClient client;
            if (list.isEmpty()){
                client = new OkHttpClient();
            } else {
                final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                client = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(list.get(i).getIP(), list.get(i).getPort()))).build();
            }

            Request html = new Request.Builder()
                    .url(URL)
                    .addHeader("User-Agent", Constant.getUserAgent())
                    .build();
            Response response = client.newCall(html).execute();
            if (response.body() != null && response.body().contentType() != null && response.body().contentType().toString().startsWith("image")) {
                response.close();
                return new Image();
            } else if (response.body() != null && response.body().contentType() != null && response.body().contentType().toString().startsWith("video")) {
                response.close();
                return new Video();
            } else if (response.body() != null && response.code() >= 200 && response.code() <= 299) {
                Matcher matcher_peerTube = this.matcher_PeerTube.matcher(response.body().string());
                if (matcher_peerTube.find()){
                    return new PeerTube();
                }
            }
        } catch (Exception e){
            // e.printStackTrace();
        }


        return null;

    }

    /**
     * @return ログデータのキューの件数
     */
    public int getLogDataListCount() {
        return LogDataList.size();
    }

    /**
     * キューを強制的に書き出す
     */
    public void ForceLogDataWrite() {
        File file = new File("./log-write-lock.txt");
        if (file.exists()){
            return;
        }
        try {
            file.createNewFile();
        } catch (Exception e){
            //e.printStackTrace();
        }

        HashMap<String, LogData> map = new HashMap<>(LogDataList);
        LogDataList.clear();

        if (map.isEmpty()){
            file.delete();
            return;
        }

        boolean isRedis1;
        try {
            YamlMapping input = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            isRedis1 = input.string("LogToRedis").toLowerCase(Locale.ROOT).equals("true");
        } catch (IOException e) {
            // e.printStackTrace();
            isRedis1 = false;
        }

        final boolean isRedis = isRedis1;
        final File fiFile = file;

        new Thread(()->{
            System.out.println("[Info] Log Write Start (Count : " + map.size() + ")");
            map.forEach((id, content)->{
                try {
                    if (isRedis){
                        RedisWrite(content);
                    } else {
                        FileWrite(content);
                    }
                } catch (Exception e){
                    LogDataList.put(id, content);
                }
            });
            System.out.println("[Info] Log Write End ("+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +")");
            map.clear();
            fiFile.delete();
        }).start();

    }

    /**
     * @param URL 処理したいURL
     * @return サービスの名前
     */
    private String getServiceName(String URL){
        Matcher matcher_TVerURL = this.matcher_TVerURL.matcher(URL);

        if (matcher_TVerURL.find()){
            return "TVer";
        }

        return null;
    }

    /**
     *
     * @return 対応サービスURLリスト
     */
    public List<String> getServiceURLList() {
        return ServiceURLList;
    }

    private void RedisWrite(LogData logData) throws Exception {
        YamlMapping input = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();

        JedisPool jedisPool = new JedisPool(input.string("RedisServer"), input.integer("RedisPort"));
        Jedis jedis = jedisPool.getResource();
        if (!input.string("RedisPass").isEmpty()){
            jedis.auth(input.string("RedisPass"));
        }

        boolean isFound = jedis.get(logData.getLogID()) != null;
        while (isFound){
            logData.setLogID(UUID.randomUUID() + "-" + new Date().getTime());

            isFound = jedis.get(logData.getLogID()) != null;
            try {
                Thread.sleep(500L);
            } catch (Exception e){
                isFound = false;
            }
        }

        jedis.set("nico-proxy:ExecuteLog:"+logData.getLogID(), new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(logData));

        jedis.close();
        jedisPool.close();
    }

    private void FileWrite(LogData logData) throws Exception {
        File logFolder = new File("./log");
        if (!logFolder.exists()){
            if (!new File("./log").mkdir()){
                throw new Exception("Folder Not Created");
            }
        }
        if (logFolder.isFile()){
            if (!logFolder.delete() || !logFolder.mkdir()){
                throw new Exception("Folder Not Created");
            }
        }

        File file = new File("./log/" + logData.getLogID() + ".json");
        boolean isFound = file.exists();
        while (isFound){
            logData.setLogID(UUID.randomUUID() + "-" + new Date().getTime());

            file = new File("./log/" + logData.getLogID() + ".json");
            isFound = file.exists();
            try {
                Thread.sleep(500L);
            } catch (Exception e){
                isFound = false;
            }
        }

        if (!file.createNewFile()){
            throw new Exception("File Not Created");
        }
        PrintWriter writer = new PrintWriter(file);
        writer.print(new Gson().toJson(logData));
        writer.close();

    }
}
