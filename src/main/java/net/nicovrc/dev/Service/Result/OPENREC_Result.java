package net.nicovrc.dev.Service.Result;

public class OPENREC_Result {

    private String URL;
    private String Title;
    private String Introduction;
    private String Thumbnail;
    private Long LiveViews;
    private Long TotalViews;

    private boolean isLive;

    private String VideoURL;
    private String LiveURL;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getIntroduction() {
        return Introduction;
    }

    public void setIntroduction(String introduction) {
        Introduction = introduction;
    }

    public String getThumbnail() {
        return Thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        Thumbnail = thumbnail;
    }

    public Long getLiveViews() {
        return LiveViews;
    }

    public void setLiveViews(Long liveViews) {
        LiveViews = liveViews;
    }

    public Long getTotalViews() {
        return TotalViews;
    }

    public void setTotalViews(Long totalViews) {
        TotalViews = totalViews;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public String getVideoURL() {
        return VideoURL;
    }

    public void setVideoURL(String videoURL) {
        VideoURL = videoURL;
    }

    public String getLiveURL() {
        return LiveURL;
    }

    public void setLiveURL(String liveURL) {
        LiveURL = liveURL;
    }
}
