package net.nicovrc.dev.data;

import okhttp3.OkHttpClient;
import xyz.n7mn.nico_proxy.ShareService;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class Image implements ShareService {
    public final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData requestVideoData) throws Exception {

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
