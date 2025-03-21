package net.nicovrc.dev.Service.Result;

import java.util.HashMap;

public class TikTokResult {

    private String URL;
    private String Description;
    private Long DiggCount;
    private Long CommentCount;
    private Long CollectCount;
    private Long ShareCount;
    private Long PlayCount;
    private Long Duration;

    private String VideoURL;
    private String VideoAccessCookie;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public Long getDiggCount() {
        return DiggCount;
    }

    public void setDiggCount(Long diggCount) {
        DiggCount = diggCount;
    }

    public Long getCommentCount() {
        return CommentCount;
    }

    public void setCommentCount(Long commentCount) {
        CommentCount = commentCount;
    }

    public Long getCollectCount() {
        return CollectCount;
    }

    public void setCollectCount(Long collectCount) {
        CollectCount = collectCount;
    }

    public Long getShareCount() {
        return ShareCount;
    }

    public void setShareCount(Long shareCount) {
        ShareCount = shareCount;
    }

    public Long getPlayCount() {
        return PlayCount;
    }

    public void setPlayCount(Long playCount) {
        PlayCount = playCount;
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

    public String getVideoAccessCookie() {
        return VideoAccessCookie;
    }

    public void setVideoAccessCookie(String videoAccessCookie) {
        VideoAccessCookie = videoAccessCookie;
    }
}
