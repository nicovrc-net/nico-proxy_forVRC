package net.nicovrc.dev.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.gson.Gson;
import net.nicovrc.dev.data.*;
import okhttp3.*;
import xyz.n7mn.nico_proxy.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;
import xyz.n7mn.nico_proxy.data.TokenJSON;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionAPI {

    private final String ver = "2.0-20240211";

    private final ProxyAPI proxyAPI;

    public ConversionAPI(ProxyAPI proxyAPI){
        this.proxyAPI = proxyAPI;
    }

    public String get(String HTTPRequest, String RequestURL, String TempRequestURL, boolean isTitleGet) throws Exception {
        //System.out.println("Debug : " + TempRequestURL);
        String result = null;

        final String request;

        if (HTTPRequest == null){
            request = new Gson().toJson(new UDPServerAccessLog(RequestURL, TempRequestURL, isTitleGet));
        } else {
            request = HTTPRequest;
        }
        String ErrorMessage = null;
        String ResultURL = null;

        ShareService Service = getService(TempRequestURL);
        String ServiceName = null;

        if (Service != null){
            ServiceName = Service.getServiceName();
        }

        if (ServiceName == null){
            ServiceName = getServiceName(TempRequestURL);
        }
        //System.out.println("Debug1-1 : " + ServiceName);

        try {
            if (Service == null){
                return null;
            }
            if (ServiceName == null){
                return null;
            }
            //System.out.println("Debug1-2 : " + ServiceName);

            final List<ProxyData> list = proxyAPI.getMainProxyList();
            final List<ProxyData> list_jp = proxyAPI.getJPProxyList();
            int main_count = list.isEmpty() ? 0 : new SecureRandom().nextInt(0, list.size() - 1);
            int jp_count = list.isEmpty() ? 0 : new SecureRandom().nextInt(0, list_jp.size() - 1);
            //System.out.println("Debug1-3 : " + ServiceName);

            final xyz.n7mn.nico_proxy.data.ProxyData proxyData = list.isEmpty() ? null : new xyz.n7mn.nico_proxy.data.ProxyData(list.get(main_count).getIP(), list.get(main_count).getPort());
            final xyz.n7mn.nico_proxy.data.ProxyData proxyData_jp = list.isEmpty() ? null : new xyz.n7mn.nico_proxy.data.ProxyData(list.get(jp_count).getIP(), list.get(jp_count).getPort());
            boolean isUseJPProxy = false;

            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
            OkHttpClient client = proxyData == null ? new OkHttpClient() : builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(list.get(main_count).getIP(), list.get(main_count).getPort()))).build();

            Request img = new Request.Builder()
                    .url(TempRequestURL)
                    .build();
            Response response = client.newCall(img).execute();
            if (response.body() != null){
                isUseJPProxy = Pattern.compile("この動画は投稿\\( アップロード \\)された地域と同じ地域からのみ視聴できます。").matcher(response.body().string()).find();
            }
            response.close();

            if (isTitleGet){
                return Service.getTitle(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
            }

            //System.out.println("Debug3 : "+TempRequestURL);
            TempRequestURL = TempRequestURL.split("\\?")[0];

            if (ServiceName.equals("ニコニコ動画")){
                ResultVideoData video = null;

                if (Pattern.compile("sm|nm").matcher(TempRequestURL).find()){
                    // 通常動画
                    video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                } else if (Pattern.compile("so").matcher(TempRequestURL).find()){
                    // 公式動画 or 配信
                    try {
                        video = Service.getVideo(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                    } catch (Exception e){
                        if (e.getMessage().equals("www.nicovideo.jp Not Found")){
                            video = Service.getLive(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                        } else {
                            throw e;
                        }
                    }
                } else {
                    // 配信
                    video = Service.getLive(new RequestVideoData(TempRequestURL, isUseJPProxy ? proxyData_jp : proxyData));
                }

                if (Pattern.compile("dmc\\.nico").matcher(video.getVideoURL()).find()){
                    if (!video.isStream()) {
                        final String JsonData = video.getTokenJson();
                        String finalTempRequestURL = TempRequestURL;
                        new Thread(() -> {
                            try {
                                // ハートビート信号
                                Request request_html = new Request.Builder()
                                        .url(finalTempRequestURL)
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

                                TokenJSON json = new Gson().fromJson(JsonData, TokenJSON.class);

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

                    final ResultVideoData finalVideo = video;
                    final Socket socket = new Socket();
                    new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), HTTPRequest, socket.getInetAddress().getHostAddress(), RequestURL, finalVideo.getVideoURL(), null))).start();
                    socket.close();
                    return video.getVideoURL();
                }


                NicoVideoInputData nicoVideoInputData = new NicoVideoInputData();
                nicoVideoInputData.setVideoURL(video.getVideoURL());
                nicoVideoInputData.setAudioURL(video.getAudioURL());
                nicoVideoInputData.setCookie(video.getTokenJson());

                if (proxyData != null && !isUseJPProxy){
                    nicoVideoInputData.setProxy(proxyData.getProxyIP() + ":" + proxyData.getPort());
                }
                if (proxyData_jp != null && isUseJPProxy){
                    nicoVideoInputData.setProxy(proxyData_jp.getProxyIP() + ":" + proxyData_jp.getPort());
                }

                String jsonText = new Gson().toJson(nicoVideoInputData);
                String SystemIP = "";
                try {
                    YamlMapping mapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                    SystemIP = mapping.string("NicoVideoSystem");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                System.out.println("Debug : "+SystemIP);

                Socket sock = new Socket(SystemIP, 25250);
                sock.setSoTimeout(4000);
                OutputStream outputStream = sock.getOutputStream();
                InputStream inputStream = sock.getInputStream();
                outputStream.write(jsonText.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                byte[] bytes = inputStream.readAllBytes();
                sock.close();

                final Socket socket = new Socket();
                new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), HTTPRequest, socket.getInetAddress().getHostAddress(), RequestURL, new String(bytes, StandardCharsets.UTF_8), null))).start();
                socket.close();
                return new String(bytes, StandardCharsets.UTF_8);
            }

        } catch (Exception e){
            ResultURL = null;
            ErrorMessage = ServiceName + " : " + e.getMessage();
            e.printStackTrace();


            final Socket socket = new Socket();
            final String finalErrorMessage = ErrorMessage;
            new Thread(() -> LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), HTTPRequest, socket.getInetAddress().getHostAddress(), RequestURL, null, finalErrorMessage))).start();
            socket.close();
            throw new Exception(ErrorMessage);
        }

        return result;
    }

    public String get(String RequestURL, String TempRequestURL, boolean isTitleGet) throws Exception{
        return get(null, RequestURL, TempRequestURL, isTitleGet);
    }

    public String getVer(){
        return ver;
    }

    public void LogWrite(LogData data){

    }

    private ShareService getService(String URL){

        Matcher matcher_NicoVideoURL = Pattern.compile("(\\.nicovideo\\.jp|nico\\.ms)").matcher(URL);
        Matcher matcher_BilibiliComURL = Pattern.compile("bilibili\\.com").matcher(URL);
        Matcher matcher_BilibiliTvURL = Pattern.compile("bilibili\\.tv").matcher(URL);
        Matcher matcher_YoutubeURL = Pattern.compile("(youtu\\.be|youtube\\.com)").matcher(URL);
        Matcher matcher_XvideoURL = Pattern.compile("xvideo").matcher(URL);
        Matcher matcher_TikTokURL = Pattern.compile("tiktok").matcher(URL);
        Matcher matcher_TwitterURL = Pattern.compile("(x|twitter)\\.com/(.*)/status/(.*)").matcher(URL);
        Matcher matcher_OpenrecURL = Pattern.compile("openrec").matcher(URL);
        Matcher matcher_PornhubURL = Pattern.compile("pornhub\\.com").matcher(URL);
        Matcher matcher_TwicastURL = Pattern.compile("twitcasting\\.tv").matcher(URL);
        Matcher matcher_AbemaURL = Pattern.compile("abema\\.tv").matcher(URL);
        Matcher matcher_TVerURL = Pattern.compile("tver\\.jp").matcher(URL);

        if (matcher_NicoVideoURL.find()){
            return new NicoNicoVideo();
        }

        if (matcher_BilibiliComURL.find()){
            return new BilibiliCom();
        }

        if (matcher_BilibiliTvURL.find()){
            return new BilibiliTv();
        }

        if (matcher_YoutubeURL.find()){
            return new Youtube();
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

        if (matcher_TVerURL.find()){
            return new TVer();
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
                    .build();
            Response response = client.newCall(html).execute();
            if (response.body() != null && response.body().contentType().toString().startsWith("image")) {
                response.close();
                return new Image();
            } else if (response.body() != null && response.body().contentType().toString().startsWith("video")) {
                response.close();
                return new Video();
            }
            response.close();
        } catch (Exception e){
            // e.printStackTrace();
        }

        return null;

    }

    private String getServiceName(String URL){
        Matcher matcher_YoutubeURL = Pattern.compile("(youtu\\.be|youtube\\.com)").matcher(URL);
        Matcher matcher_TVerURL = Pattern.compile("tver\\.jp").matcher(URL);

        if (matcher_YoutubeURL.find()){
            return "Youtube";
        }
        if (matcher_TVerURL.find()){
            return "TVer";
        }

        return null;
    }
}
