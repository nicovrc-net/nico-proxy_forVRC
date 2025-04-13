package net.nicovrc.dev.http.getContent;

public class ContentObject {

    private String HLSText = null;
    private String DummyHLSText = null;
    private byte[] ContentObject = null;

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
}
