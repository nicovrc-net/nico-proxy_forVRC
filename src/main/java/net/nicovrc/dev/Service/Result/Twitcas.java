package net.nicovrc.dev.Service.Result;

public class Twitcas {
    private String URL;
    private String Title;
    private String SubTitle;
    private String Thumbnail;

    private Long Duration;
    private Long max_viewCount;
    private Long current_viewCount;
    private Long total_viewCount;

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

    public String getSubTitle() {
        return SubTitle;
    }

    public void setSubTitle(String subTitle) {
        SubTitle = subTitle;
    }

    public String getThumbnail() {
        return Thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        Thumbnail = thumbnail;
    }

    public Long getDuration() {
        return Duration;
    }

    public void setDuration(Long duration) {
        Duration = duration;
    }

    public Long getMax_viewCount() {
        return max_viewCount;
    }

    public void setMax_viewCount(Long max_viewCount) {
        this.max_viewCount = max_viewCount;
    }

    public Long getCurrent_viewCount() {
        return current_viewCount;
    }

    public void setCurrent_viewCount(Long current_viewCount) {
        this.current_viewCount = current_viewCount;
    }

    public Long getTotal_viewCount() {
        return total_viewCount;
    }

    public void setTotal_viewCount(Long total_viewCount) {
        this.total_viewCount = total_viewCount;
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
