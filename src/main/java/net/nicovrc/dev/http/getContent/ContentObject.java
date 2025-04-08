package net.nicovrc.dev.http.getContent;

public class ContentObject {

    private byte[] HLSText = null;
    private byte[] DummyHLSText = null;
    private byte[] ContentObject = null;

    public byte[] getHLSText() {
        return HLSText;
    }

    public void setHLSText(byte[] HLSText) {
        this.HLSText = HLSText;
    }

    public byte[] getDummyHLSText() {
        return DummyHLSText;
    }

    public void setDummyHLSText(byte[] dummyHLSText) {
        DummyHLSText = dummyHLSText;
    }

    public byte[] getContentObject() {
        return ContentObject;
    }

    public void setContentObject(byte[] contentObject) {
        ContentObject = contentObject;
    }
}
