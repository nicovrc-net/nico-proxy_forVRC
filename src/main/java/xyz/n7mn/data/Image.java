package xyz.n7mn.data;

import xyz.n7mn.nico_proxy.ShareService;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class Image implements ShareService {
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        return new ResultVideoData("https://i2v.nicovrc.net/?url="+data.getURL(), null, true, false, true, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return getVideo(data);
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }
}
