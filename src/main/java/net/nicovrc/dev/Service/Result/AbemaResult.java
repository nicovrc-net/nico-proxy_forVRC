package net.nicovrc.dev.Service.Result;

public class AbemaResult {

    private String URL;
    private String Title;
    private String Content;
    private String Thumbnail;

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

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }

    public String getThumbnail() {
        return Thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        Thumbnail = thumbnail;
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
