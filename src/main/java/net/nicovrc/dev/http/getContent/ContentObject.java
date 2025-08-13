package net.nicovrc.dev.http.getContent;

public class ContentObject {

    private String HLSText = null;
    private String DummyHLSText = null;
    private String CookieText = null;
    private String RefererText = null;
    private boolean isHLS = true;

    public String getHLSText() {
        return HLSText;
    }

    public void setHLSText(String HLSText) {
        this.HLSText = HLSText;
    }

    public String getDummyHLSText() {
        return DummyHLSText;
    }

    public void setDummyHLSText(String dummyHLSText) {
        DummyHLSText = dummyHLSText;
    }

    public String getCookieText() {
        return CookieText;
    }

    public void setCookieText(String cookieText) {
        CookieText = cookieText;
    }

    public String getRefererText() {
        return RefererText;
    }

    public void setRefererText(String refererText) {
        RefererText = refererText;
    }

    public boolean isHLS() {
        return isHLS;
    }

    public void setHLS(boolean HLS) {
        isHLS = HLS;
    }
}
