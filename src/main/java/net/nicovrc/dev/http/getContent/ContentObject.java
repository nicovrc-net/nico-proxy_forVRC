package net.nicovrc.dev.http.getContent;

public class ContentObject {

    private String HLSText = null;
    private String DummyHLSText = null;
    private byte[] ContentObject = null;
    private String CookieText = null;
    private String RefererText = null;

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

    public byte[] getContentObject() {
        return ContentObject;
    }

    public void setContentObject(byte[] contentObject) {
        ContentObject = contentObject;
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
}
