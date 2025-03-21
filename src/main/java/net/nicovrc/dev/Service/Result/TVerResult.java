package net.nicovrc.dev.Service.Result;

public class TVerResult {

    private String URL;
    private String Title;
    private String Description;
    private String Thumbnail;
    private Long Duration;

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

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
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

    public String getLiveURL() {
        return LiveURL;
    }

    public void setLiveURL(String liveURL) {
        LiveURL = liveURL;
    }
}
