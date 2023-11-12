package xyz.n7mn.data;

public class YoutubeRequestData {

    private String YoutubeURL;
    private String VideoURL;
    private String AudioURL;
    private String CaptionURL;
    private String Proxy;


    public void setYoutubeURL(String youtubeURL) {
        this.YoutubeURL = youtubeURL;
    }

    public void setVideoURL(String videoURL) {
        this.VideoURL = videoURL;
    }

    public void setAudioURL(String audioURL) {
        this.AudioURL = audioURL;
    }

    public void setCaptionURL(String captionURL) {
        this.CaptionURL = captionURL;
    }

    public void setProxy(String proxy) {
        this.Proxy = proxy;
    }
}
