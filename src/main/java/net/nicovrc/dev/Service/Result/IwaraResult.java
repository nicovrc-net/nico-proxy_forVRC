package net.nicovrc.dev.Service.Result;

public class IwaraResult {

    private String Title;
    private String Description;
    private Long LikeCount;
    private Long ViewCount;

    private String VideoURL;

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

    public Long getLikeCount() {
        return LikeCount;
    }

    public void setLikeCount(Long likeCount) {
        LikeCount = likeCount;
    }

    public Long getViewCount() {
        return ViewCount;
    }

    public void setViewCount(Long viewCount) {
        ViewCount = viewCount;
    }

    public String getVideoURL() {
        return VideoURL;
    }

    public void setVideoURL(String videoURL) {
        VideoURL = videoURL;
    }
}
