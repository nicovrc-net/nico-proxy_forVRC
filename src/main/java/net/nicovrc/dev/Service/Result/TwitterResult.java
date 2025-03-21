package net.nicovrc.dev.Service.Result;

public class TwitterResult {

    private String URL;
    private String TweetText;
    private String Thumbnail;
    private Long ReplyCount;
    private Long RetweetCount;
    private Long FavoriteCount;
    private Long BookmarkCount;
    private Long QuoteCount;
    private Long Duration;

    private String VideoURL;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getTweetText() {
        return TweetText;
    }

    public void setTweetText(String tweetText) {
        TweetText = tweetText;
    }

    public String getThumbnail() {
        return Thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        Thumbnail = thumbnail;
    }

    public Long getReplyCount() {
        return ReplyCount;
    }

    public void setReplyCount(Long replyCount) {
        ReplyCount = replyCount;
    }

    public Long getRetweetCount() {
        return RetweetCount;
    }

    public void setRetweetCount(Long retweetCount) {
        RetweetCount = retweetCount;
    }

    public Long getFavoriteCount() {
        return FavoriteCount;
    }

    public void setFavoriteCount(Long favoriteCount) {
        FavoriteCount = favoriteCount;
    }

    public Long getBookmarkCount() {
        return BookmarkCount;
    }

    public void setBookmarkCount(Long bookmarkCount) {
        BookmarkCount = bookmarkCount;
    }

    public Long getQuoteCount() {
        return QuoteCount;
    }

    public void setQuoteCount(Long quoteCount) {
        QuoteCount = quoteCount;
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
}
