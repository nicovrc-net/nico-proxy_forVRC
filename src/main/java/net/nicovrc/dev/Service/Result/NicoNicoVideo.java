package net.nicovrc.dev.Service.Result;

import java.util.Date;
import java.util.HashMap;

public class NicoNicoVideo {
    private String URL;
    private String ContentID;
    private String Title;
    private String Description;
    private String[] Tags;
    private String Thumbnail;
    private long ViewCount;
    private long CommentCount;
    private long MyListCount;
    private long LikeCount;
    private String startTime;
    private long Duration;

    private String VideoURL;
    private HashMap<String, String> VideoAccessCookie;
    private String LiveURL;
    private HashMap<String, String> LiveAccessCookie;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getContentID() {
        return ContentID;
    }

    public void setContentID(String contentID) {
        ContentID = contentID;
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

    public String[] getTags() {
        return Tags;
    }

    public void setTags(String[] tags) {
        Tags = tags;
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

    public long getCommentCount() {
        return CommentCount;
    }

    public void setCommentCount(long commentCount) {
        CommentCount = commentCount;
    }

    public long getMyListCount() {
        return MyListCount;
    }

    public void setMyListCount(long myListCount) {
        MyListCount = myListCount;
    }

    public long getLikeCount() {
        return LikeCount;
    }

    public void setLikeCount(long likeCount) {
        LikeCount = likeCount;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
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

    public void setVideoAccessCookie(HashMap<String, String> cookie) {
        VideoAccessCookie = cookie;
    }

    public String getLiveURL() {
        return LiveURL;
    }

    public void setLiveURL(String liveURL) {
        LiveURL = liveURL;
    }

    public HashMap<String, String> getLiveAccessCookie() {
        return LiveAccessCookie;
    }

    public void setLiveAccessCookie(HashMap<String, String> liveAccessCookie) {
        LiveAccessCookie = liveAccessCookie;
    }
}
