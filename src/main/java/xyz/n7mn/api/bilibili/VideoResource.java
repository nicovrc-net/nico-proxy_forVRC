package xyz.n7mn.api.bilibili;

public class VideoResource {

    private String id;
    private int quality;
    private int bandwidth;
    private int codec_id;
    private int duration;
    private long size;
    private String md5;
    private String url;
    private String backup_url;
    private int container;
    private int start_with_sap;
    private String codecs;
    private String sar;
    private String frame_rate;
    private SegmentBase segment_base;
    private int width;
    private int height;
    private String mime_type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public int getCodec_id() {
        return codec_id;
    }

    public void setCodec_id(int codec_id) {
        this.codec_id = codec_id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBackup_url() {
        return backup_url;
    }

    public void setBackup_url(String backup_url) {
        this.backup_url = backup_url;
    }

    public int getContainer() {
        return container;
    }

    public void setContainer(int container) {
        this.container = container;
    }

    public int getStart_with_sap() {
        return start_with_sap;
    }

    public void setStart_with_sap(int start_with_sap) {
        this.start_with_sap = start_with_sap;
    }

    public String getCodecs() {
        return codecs;
    }

    public void setCodecs(String codecs) {
        this.codecs = codecs;
    }

    public String getSar() {
        return sar;
    }

    public void setSar(String sar) {
        this.sar = sar;
    }

    public String getFrame_rate() {
        return frame_rate;
    }

    public void setFrame_rate(String frame_rate) {
        this.frame_rate = frame_rate;
    }

    public SegmentBase getSegment_base() {
        return segment_base;
    }

    public void setSegment_base(SegmentBase segment_base) {
        this.segment_base = segment_base;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }
}
