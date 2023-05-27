package xyz.n7mn.lib.bilibili;

public class Data {
    private Playurl playurl;
    private Watermark watermark;
    private Object in_stream_ad;

    public Playurl getPlayurl() {
        return playurl;
    }

    public void setPlayurl(Playurl playurl) {
        this.playurl = playurl;
    }

    public Watermark getWatermark() {
        return watermark;
    }

    public void setWatermark(Watermark watermark) {
        this.watermark = watermark;
    }

    public Object getIn_stream_ad() {
        return in_stream_ad;
    }

    public void setIn_stream_ad(Object in_stream_ad) {
        this.in_stream_ad = in_stream_ad;
    }
}
