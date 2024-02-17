package net.nicovrc.dev.data;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.ShareService;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Locale;

public class Image implements ShareService {
    public final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData requestVideoData) throws Exception {

        final OkHttpClient client;
        if (requestVideoData.getProxy() != null){
            client = new OkHttpClient();
        } else {
            client = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(requestVideoData.getProxy().getProxyIP(), requestVideoData.getProxy().getPort()))).build();
        }

        Request request = new Request.Builder()
                .url(requestVideoData.getURL())
                .build();

        Response response = client.newCall(request).execute();
        if (response.body() != null && response.body().contentType().type().toLowerCase(Locale.ROOT).startsWith("video")){
            response.close();
            return new ResultVideoData(requestVideoData.getURL(), null, false, false, false, null);
        }
        response.close();

        return new ResultVideoData("https://i2v.nicovrc.net/?url="+requestVideoData.getURL(), null, true, false, true, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData requestVideoData) throws Exception {
        return getVideo(requestVideoData);
    }

    @Override
    public String getTitle(RequestVideoData requestVideoData) throws Exception {
        return null;
    }

    @Override
    public String getServiceName() {
        return "画像";
    }

    @Override
    public String getVersion() {
        return null;
    }
}
