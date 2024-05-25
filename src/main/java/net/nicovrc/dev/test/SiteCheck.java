package net.nicovrc.dev.test;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import net.nicovrc.dev.api.ConversionAPI;
import net.nicovrc.dev.api.ProxyAPI;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.*;
import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

public class SiteCheck {

    public static HashMap<String, String> Run(List<net.nicovrc.dev.data.ProxyData> MainProxyList, List<net.nicovrc.dev.data.ProxyData> JPProxyList){

        final HashMap<String, String> list = new HashMap<>();

        final ProxyData proxyData;
        final ProxyData proxyData_jp;

        if (!MainProxyList.isEmpty()){
            proxyData = new ProxyData(MainProxyList.get(0).getIP(), MainProxyList.get(0).getPort());
        } else {
            proxyData = null;
        }

        if (!JPProxyList.isEmpty()){
            proxyData_jp = new ProxyData(JPProxyList.get(0).getIP(), JPProxyList.get(0).getPort());
        } else {
            proxyData_jp = null;
        }


        ResultVideoData video = null;

        try {
            NicoNicoVideo nicoNicoVideo = new NicoNicoVideo();
            video = nicoNicoVideo.getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/sm9", proxyData));
            video = nicoNicoVideo.getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/so38016254", proxyData_jp));
            video = nicoNicoVideo.getLive(new RequestVideoData("https://live.nicovideo.jp/watch/ch1072", proxyData_jp));
            nicoNicoVideo.cancelWebSocket();

            list.put("nicovideo.jp", "OK");
        } catch (Exception e){
            list.put("nicovideo.jp", "NG");
        }

        try {
            video = new BilibiliCom().getVideo(new RequestVideoData("https://www.bilibili.com/video/BV1y5411Y7A2/", proxyData));

            list.put("bilibili.com", "OK");
        } catch (Exception e){
            list.put("bilibili.com", "NG");
        }

        try {
            video = new BilibiliTv().getVideo(new RequestVideoData("https://www.bilibili.tv/en/video/4791213337477632", proxyData));

            list.put("bilibili.tv", "OK");
        } catch (Exception e){
            list.put("bilibili.tv", "NG");
        }

        try {
            video = new YoutubeHLS().getVideo(new RequestVideoData("https://www.youtube.com/watch?v=HRaqMxo7YeM", proxyData));

            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
            final OkHttpClient client = proxyData == null ? new OkHttpClient() : builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyData.getProxyIP(), proxyData.getPort()))).build();
            Request build = new Request.Builder()
                    .url("https://www.youtube.com/channel/UCq85Ke7_2X_Zu28OTnfLKsA/live")
                    .build();

            Response response = client.newCall(build).execute();
            String url = response.request().url().toString();
            response.close();

            video = new YoutubeHLS().getLive(new RequestVideoData(url, proxyData));
            list.put("youtube.com", "OK");
        } catch (Exception e){
            list.put("youtube.com", "NG");
        }

        try {
            video = new Xvideos().getVideo(new RequestVideoData("https://www.xvideos.com/video.kmmooaf338a/_megumi_2", proxyData));

            list.put("xvideos.com", "OK");
        } catch (Exception e){
            list.put("xvideos.com", "NG");
        }

        try {
            video = new TikTok().getVideo(new RequestVideoData("https://www.tiktok.com/@komedascoffee/video/7258220227773746433", proxyData));

            list.put("tiktok.com", "OK");
        } catch (Exception e){
            list.put("tiktok.com", "NG");
        }

        try {
            video = new Twitter().getVideo(new RequestVideoData("https://x.com/lears_VR/status/1790014952809582667", proxyData));

            list.put("x.com", "OK");
        } catch (Exception e){
            list.put("x.com", "NG");
        }

        try {
            video = new OPENREC().getVideo(new RequestVideoData("https://www.openrec.tv/movie/fviKjvGVH2p", proxyData));

            list.put("openrec.tv", "OK");
        } catch (Exception e){
            list.put("openrec.tv", "NG");
        }

        try {
            video = new Xvideos().getVideo(new RequestVideoData("https://jp.pornhub.com/view_video.php?viewkey=ph630e13a63f969", proxyData));

            list.put("pornhub.com", "OK");
        } catch (Exception e){
            list.put("pornhub.com", "NG");
        }

        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            video = new Twicast(yamlMapping.string("ClientID"), yamlMapping.string("ClientSecret")).getVideo(new RequestVideoData("https://twitcasting.tv/twitcasting_jp", proxyData));

            list.put("twitcasting.tv", "OK");
        } catch (Exception e){
            list.put("twitcasting.tv", "NG");
        }

        try {
            video = new Abema().getVideo(new RequestVideoData("https://abema.tv/now-on-air/abema-news", proxyData));

            list.put("abema.tv", "OK");
        } catch (Exception e){
            list.put("abema.tv", "NG");
        }

        try {
            video = new TVer().getVideo(new RequestVideoData("https://tver.jp/live/ntv", proxyData));

            list.put("tver.jp", "OK");
        } catch (Exception e){
            list.put("tver.jp", "NG");
        }

        try {
            video = new Iwara().getVideo(new RequestVideoData("https://www.iwara.tv/video/vwvOcGMRQyvlwD", proxyData));

            list.put("iwara.tv", "OK");
        } catch (Exception e){
            list.put("iwara.tv", "NG");
        }

        try {
            video = new Piapro().getVideo(new RequestVideoData("https://piapro.jp/t/KQn0", proxyData_jp));

            list.put("piapro.jp", "OK");
        } catch (Exception e){
            list.put("piapro.jp", "NG");
        }

        try {
            video = new SoundCloud().getVideo(new RequestVideoData("https://soundcloud.com/baron1_3/penguin3rd", proxyData));

            list.put("soundcloud.com", "OK");
        } catch (Exception e){
            list.put("soundcloud.com", "NG");
        }

        video = null;
        System.gc();
        return list;

    }
}
