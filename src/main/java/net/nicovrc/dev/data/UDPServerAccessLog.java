package net.nicovrc.dev.data;

public class UDPServerAccessLog {

    private String RequestURL;
    private String TempRequestURL;
    private boolean isTitleGet;

    public UDPServerAccessLog(String requestURL, String tempRequestURL, boolean isTitleGet){
        this.RequestURL = requestURL;
        this.TempRequestURL = tempRequestURL;
        this.isTitleGet = isTitleGet;
    }

    public String getRequestURL() {
        return RequestURL;
    }

    public void setRequestURL(String requestURL) {
        RequestURL = requestURL;
    }

    public String getTempRequestURL() {
        return TempRequestURL;
    }

    public void setTempRequestURL(String tempRequestURL) {
        TempRequestURL = tempRequestURL;
    }

    public boolean isTitleGet() {
        return isTitleGet;
    }

    public void setTitleGet(boolean titleGet) {
        isTitleGet = titleGet;
    }
}
