package xyz.n7mn.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class VideoInfo {
    // video_id
    private String VideoId;
    // title
    private String Title;
    // description
    private String Description;
    // thumbnail_url
    private String ThumbnailUrl;
    // first_retrieve
    private Date FirstRetrieve;
    // length
    private String VideoLength;
    // movie_type
    private String MovieType;
    // size_high
    private int SizeHigh;
    // size_low
    private int SizeLow;
    // view_counter
    private long ViewCounter;
    // comment_num
    private long CommentNum;
    // mylist_counter
    private long MyListCounter;
    // last_res_body
    private String LastResBody;
    // watch_url
    private String VideoURL;
    // thumb_type
    private String ThumbType;
    // embeddable
    private boolean Embeddable;
    // no_live_play
    private boolean NoLivePlay;
    // tag (<tag lock="1">陰陽師</tag> / <tag>ぷよぷよ禁止令</tag>)
    private Map<String, Boolean> Tag;
    //genre
    private String Genre;
    // user_id
    private long UserId;
    // user_nickname
    private String UserNickname;
    // user_icon_url
    private String UserIconUrl;

    private VideoInfo(){

    }

    public static VideoInfo newInstance(String XmlText) throws Exception {
        VideoInfo info = new VideoInfo();
        info.XmlImport(XmlText);
        return info;
    }

    private void XmlImport(String XmlText) throws ParseException {

        Matcher matcher1 = Pattern.compile("<video_id>(.*)</video_id>").matcher(XmlText);
        if (matcher1.find()){
            this.VideoId = matcher1.group(1);
        }
        Matcher matcher2 = Pattern.compile("<title>(.*)</title>").matcher(XmlText);
        if (matcher2.find()){
            this.Title = matcher2.group(1);
        }
        Matcher matcher3 = Pattern.compile("<description>(.*)</description>").matcher(XmlText);
        if (matcher3.find()){
            this.Description = matcher3.group(1);
        }
        Matcher matcher4 = Pattern.compile("<thumbnail_url>(.*)</title>").matcher(XmlText);
        if (matcher4.find()){
            this.ThumbnailUrl = matcher4.group(1);
        }
        Matcher matcher5 = Pattern.compile("<first_retrieve>(.*)</description>").matcher(XmlText);
        if (matcher5.find()){
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss+09:00");
            this.FirstRetrieve = format.parse(matcher5.group(1));
        }
        Matcher matcher6 = Pattern.compile("<length>(.*)</length>").matcher(XmlText);
        if (matcher6.find()){
            this.VideoLength = matcher6.group(1);
        }
        Matcher matcher7 = Pattern.compile("<movie_type>(.*)</movie_type>").matcher(XmlText);
        if (matcher7.find()){
            this.MovieType = matcher7.group(1);
        }
        Matcher matcher8 = Pattern.compile("<size_high>(.*)</size_high>").matcher(XmlText);
        if (matcher8.find()){
            this.SizeHigh = Integer.parseInt(matcher8.group(1));
        }
        Matcher matcher9 = Pattern.compile("<size_low>(.*)</size_low>").matcher(XmlText);
        if (matcher9.find()){
            this.SizeLow = Integer.parseInt(matcher9.group(1));
        }
        Matcher matcher10 = Pattern.compile("<view_counter>(.*)</view_counter>").matcher(XmlText);
        if (matcher10.find()){
            this.ViewCounter = Long.parseLong(matcher10.group(1));
        }
        Matcher matcher11 = Pattern.compile("<comment_num>(.*)</comment_num>").matcher(XmlText);
        if (matcher11.find()){
            this.CommentNum = Long.parseLong(matcher11.group(1));
        }
        Matcher matcher12 = Pattern.compile("<mylist_counter>(.*)</mylist_counter>").matcher(XmlText);
        if (matcher12.find()){
            this.MyListCounter = Long.parseLong(matcher12.group(1));
        }
        Matcher matcher13 = Pattern.compile("<last_res_body>(.*)</last_res_body>").matcher(XmlText);
        if (matcher13.find()){
            this.LastResBody = matcher13.group(1);
        }
        Matcher matcher14 = Pattern.compile("<watch_url>(.*)</watch_url>").matcher(XmlText);
        if (matcher14.find()){
            this.VideoId = matcher14.group(1);
        }
        Matcher matcher15 = Pattern.compile("<thumb_type>(.*)</thumb_type>").matcher(XmlText);
        if (matcher15.find()){
            this.ThumbType = matcher15.group(1);
        }
        Matcher matcher16 = Pattern.compile("<embeddable>(.*)</embeddable>").matcher(XmlText);
        if (matcher16.find()){
            this.Embeddable = matcher16.group(1).equals("1");
        }
        Matcher matcher17 = Pattern.compile("<no_live_play>(.*)</no_live_play>").matcher(XmlText);
        if (matcher17.find()){
            this.NoLivePlay = matcher17.group(1).equals("1");
        }
        Tag = new HashMap<>();
        Matcher matcher18 = Pattern.compile("<tag lock=\"1\">(.*)</tag>").matcher(XmlText);
        while (matcher18.find()){
            Tag.put(matcher18.group(1), true);
        }
        Matcher matcher18_2 = Pattern.compile("<tag>(.*)</tag>").matcher(XmlText);
        while (matcher18_2.find()){
            Tag.put(matcher18_2.group(1), false);
        }
        Matcher matcher19 = Pattern.compile("<genre>(.*)</genre>").matcher(XmlText);
        if (matcher19.find()){
            this.Genre = matcher19.group(1);
        }
        Matcher matcher20 = Pattern.compile("<user_id>(.*)</user_id>").matcher(XmlText);
        if (matcher20.find()){
            this.UserId = Long.parseLong(matcher20.group(1));
        }
        Matcher matcher21 = Pattern.compile("<user_nickname>(.*)</user_nickname>").matcher(XmlText);
        if (matcher21.find()){
            this.UserNickname = matcher21.group(1);
        }
        Matcher matcher22 = Pattern.compile("<user_icon_url>(.*)</user_icon_url>").matcher(XmlText);
        if (matcher22.find()){
            this.UserIconUrl = matcher22.group(1);
        }

    }

    public String getVideoId() {
        return VideoId;
    }

    public void setVideoId(String videoId) {
        VideoId = videoId;
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

    public String getThumbnailUrl() {
        return ThumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        ThumbnailUrl = thumbnailUrl;
    }

    public Date getFirstRetrieve() {
        return FirstRetrieve;
    }

    public void setFirstRetrieve(Date firstRetrieve) {
        FirstRetrieve = firstRetrieve;
    }

    public String getVideoLength() {
        return VideoLength;
    }

    public long getVideoLengthBySec() {
        return (Long.parseLong(VideoLength.split(":")[0]) * 60) + Long.parseLong(VideoLength.split(":")[1]);
    }

    public void setVideoLength(String videoLength) {
        VideoLength = videoLength;
    }

    public String getMovieType() {
        return MovieType;
    }

    public void setMovieType(String movieType) {
        MovieType = movieType;
    }

    public int getSizeHigh() {
        return SizeHigh;
    }

    public void setSizeHigh(int sizeHigh) {
        SizeHigh = sizeHigh;
    }

    public int getSizeLow() {
        return SizeLow;
    }

    public void setSizeLow(int sizeLow) {
        SizeLow = sizeLow;
    }

    public long getViewCounter() {
        return ViewCounter;
    }

    public void setViewCounter(long viewCounter) {
        ViewCounter = viewCounter;
    }

    public long getCommentNum() {
        return CommentNum;
    }

    public void setCommentNum(long commentNum) {
        CommentNum = commentNum;
    }

    public long getMyListCounter() {
        return MyListCounter;
    }

    public void setMyListCounter(long myListCounter) {
        MyListCounter = myListCounter;
    }

    public String getLastResBody() {
        return LastResBody;
    }

    public void setLastResBody(String lastResBody) {
        LastResBody = lastResBody;
    }

    public String getVideoURL() {
        return VideoURL;
    }

    public void setVideoURL(String videoURL) {
        VideoURL = videoURL;
    }

    public String getThumbType() {
        return ThumbType;
    }

    public void setThumbType(String thumbType) {
        ThumbType = thumbType;
    }

    public boolean isEmbeddable() {
        return Embeddable;
    }

    public void setEmbeddable(boolean embeddable) {
        Embeddable = embeddable;
    }

    public boolean isNoLivePlay() {
        return NoLivePlay;
    }

    public void setNoLivePlay(boolean noLivePlay) {
        NoLivePlay = noLivePlay;
    }

    public Map<String, Boolean> getTag() {
        return Tag;
    }

    public void setTag(Map<String, Boolean> tag) {
        Tag = tag;
    }

    public String getGenre() {
        return Genre;
    }

    public void setGenre(String genre) {
        Genre = genre;
    }

    public long getUserId() {
        return UserId;
    }

    public void setUserId(long userId) {
        UserId = userId;
    }

    public String getUserNickname() {
        return UserNickname;
    }

    public void setUserNickname(String userNickname) {
        UserNickname = userNickname;
    }

    public String getUserIconUrl() {
        return UserIconUrl;
    }

    public void setUserIconUrl(String userIconUrl) {
        UserIconUrl = userIconUrl;
    }
}
