package net.nicovrc.dev.data;

import xyz.n7mn.nico_proxy.ShareService;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class Video implements ShareService {
    @Override
    public ResultVideoData getVideo(RequestVideoData requestVideoData) throws Exception {
        return new ResultVideoData(requestVideoData.getURL(), null, true, false, true, null);
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
        return "動画";
    }

    @Override
    public String getVersion() {
        return null;
    }
}
