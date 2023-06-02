package xyz.n7mn.api;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.api.bilibili.BilibiliTvData;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class BilibiliTv implements ShareService{
    @Override
    public String getVideo(String url, ProxyData proxy) throws Exception {
        // https://www.bilibili.tv/en/video/4786094886751232
        String s = url.split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.length() == 0){
            id = strings[strings.length - 2];
        }
        Request api = new Request.Builder()
                .url("https://api.bilibili.tv/intl/gateway/web/playurl?s_locale=en_US&platform=web&aid="+id)
                .build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = proxy != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getProxyIP(), proxy.getPort()))).build() : new OkHttpClient();
        final String JsonText;
        String tempText;

        try {
            Response response = client.newCall(api).execute();
            tempText = response.body().string();
            response.close();
        } catch (Exception e){
            throw new Exception("api.bilibili.tv "+ e.getMessage() + (proxy != null ? "(Use Proxy : "+proxy.getProxyIP()+")" : ""));
        }
        JsonText = tempText;

        BilibiliTvData data = new Gson().fromJson(JsonText, BilibiliTvData.class);
        if (data.getData() == null){
            //System.out.println(JsonText);
            throw new Exception("api.bilibili.tv Not APIData");
        }
        String videoURL = data.getData().getPlayurl().getVideo()[0].getVideo_resource().getUrl();
        String audioURL = data.getData().getPlayurl().getAudio_resource()[0].getUrl();

        Request m3u8 = new Request.Builder()
                .url("https://nico.7mi.site/m3u8/?vi="+videoURL+"&music="+audioURL)
                .build();

        String URL = null;
        try {
            //System.out.println("https://nico.7mi.site/m3u8/?vi="+videoURL+"&music="+audioURL);

            Response response = client.newCall(m3u8).execute();
            String s1 = response.body().string();

            URL = "https://nico.7mi.site/m3u8/video_"+s1+".m3u8";

        } catch (Exception e){
            throw new Exception("bilibili.tv m3u8 Create Error (" + e.getMessage()+")");
        }

        return URL;
    }

    @Override
    public String getLive(String url, ProxyData proxy) throws Exception {
        // 存在しないので実装しない
        return null;
    }

    @Override
    public String getServiceName() {
        return "bilibili.tv";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
