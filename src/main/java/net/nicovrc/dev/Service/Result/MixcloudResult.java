package net.nicovrc.dev.Service.Result;

public class MixcloudResult {

    private String Title;
    private Long favCount;
    private Long playCount;
    private Long repostCount;

    private String AudioURL;

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public Long getFavCount() {
        return favCount;
    }

    public void setFavCount(Long favCount) {
        this.favCount = favCount;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long playCount) {
        this.playCount = playCount;
    }

    public Long getRepostCount() {
        return repostCount;
    }

    public void setRepostCount(Long repostCount) {
        this.repostCount = repostCount;
    }

    public String getAudioURL() {
        return AudioURL;
    }

    public void setAudioURL(String audioURL) {
        AudioURL = audioURL;
    }
}
