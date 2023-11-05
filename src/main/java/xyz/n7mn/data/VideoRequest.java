package xyz.n7mn.data;

public class VideoRequest {

    private String RequestCode;
    private String HTTPRequest;
    private String ServerIP;
    private String RequestURL;
    private String TempRequestURL;

    public String getRequestCode() {
        return RequestCode;
    }

    public void setRequestCode(String requestCode) {
        RequestCode = requestCode;
    }

    public String getHTTPRequest() {
        return HTTPRequest;
    }

    public void setHTTPRequest(String HTTPRequest) {
        this.HTTPRequest = HTTPRequest;
    }

    public String getServerIP() {
        return ServerIP;
    }

    public void setServerIP(String serverIP) {
        ServerIP = serverIP;
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
}
