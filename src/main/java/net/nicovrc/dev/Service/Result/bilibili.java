package net.nicovrc.dev.Service.Result;

import java.util.HashMap;

public class bilibili {
    private String URL;
    private String Title;
    private String Description;
    private String Thumbnail;
    private long ViewCount;
    private long ReplyCount;
    private long LikeCount;
    private long CoinCount;
    private long FavoriteCount;
    private long Duration;

    private String VideoURL;
    private HashMap<String, String> VideoAccessCookie;
    //private String LiveURL;
    //private HashMap<String, String> LiveAccessCookie;


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

    public long getViewCount() {
        return ViewCount;
    }

    public void setViewCount(long viewCount) {
        ViewCount = viewCount;
    }

    public long getReplyCount() {
        return ReplyCount;
    }

    public void setReplyCount(long replyCount) {
        ReplyCount = replyCount;
    }

    public long getLikeCount() {
        return LikeCount;
    }

    public void setLikeCount(long likeCount) {
        LikeCount = likeCount;
    }

    public long getCoinCount() {
        return CoinCount;
    }

    public void setCoinCount(long coinCount) {
        CoinCount = coinCount;
    }

    public long getFavoriteCount() {
        return FavoriteCount;
    }

    public void setFavoriteCount(long favoriteCount) {
        FavoriteCount = favoriteCount;
    }

    public long getDuration() {
        return Duration;
    }

    public void setDuration(long duration) {
        Duration = duration;
    }

    public String getVideoURL() {
        return VideoURL;
    }

    public void setVideoURL(String videoURL) {
        VideoURL = videoURL;
    }

    public HashMap<String, String> getVideoAccessCookie() {
        return VideoAccessCookie;
    }

    public void setVideoAccessCookie(HashMap<String, String> videoAccessCookie) {
        VideoAccessCookie = videoAccessCookie;
    }
}
