package net.nicovrc.dev.Service.Result;

public class PornhubResult {

    private String Title;
    private String Thumbnail;
    private Long Duration;

    private String VideoURL;
    private String VideoHLSURL;

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
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

    public String getVideoURL() {
        return VideoURL;
    }

    public void setVideoURL(String videoURL) {
        VideoURL = videoURL;
    }

    public String getVideoHLSURL() {
        return VideoHLSURL;
    }

    public void setVideoHLSURL(String videoHLSURL) {
        VideoHLSURL = videoHLSURL;
    }
}
