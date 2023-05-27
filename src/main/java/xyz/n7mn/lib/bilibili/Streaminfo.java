package xyz.n7mn.lib.bilibili;

public class Streaminfo {

    private int quality;
    private String desc_text;
    private String desc_words;
    private boolean intact;
    private boolean no_rexcode;

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public String getDesc_text() {
        return desc_text;
    }

    public void setDesc_text(String desc_text) {
        this.desc_text = desc_text;
    }

    public String getDesc_words() {
        return desc_words;
    }

    public void setDesc_words(String desc_words) {
        this.desc_words = desc_words;
    }

    public boolean isIntact() {
        return intact;
    }

    public void setIntact(boolean intact) {
        this.intact = intact;
    }

    public boolean isNo_rexcode() {
        return no_rexcode;
    }

    public void setNo_rexcode(boolean no_rexcode) {
        this.no_rexcode = no_rexcode;
    }
}
