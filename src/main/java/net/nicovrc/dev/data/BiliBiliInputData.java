package net.nicovrc.dev.data;

public class BiliBiliInputData {
    private String SiteType;
    private String VideoURL;
    private String AudioURL;
    private String Proxy;
    private long VideoDuration;

    public String getSiteType() {
        return SiteType;
    }

    public void setSiteType(String siteType) {
        SiteType = siteType;
    }

    public String getVideoURL() {
        return VideoURL;
    }

    public void setVideoURL(String videoURL) {
        VideoURL = videoURL;
    }

    public String getAudioURL() {
        return AudioURL;
    }

    public void setAudioURL(String audioURL) {
        AudioURL = audioURL;
    }

    public String getProxy() {
        return Proxy;
    }

    public void setProxy(String proxy) {
        Proxy = proxy;
    }

    public long getVideoDuration() {
        return VideoDuration;
    }

    public void setVideoDuration(long videoDuration) {
        VideoDuration = videoDuration;
    }
}
