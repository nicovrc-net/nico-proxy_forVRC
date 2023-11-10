package xyz.n7mn.data;

public class UDPJsonRequest {
    private String RequestCode;
    private String HTTPRequest;
    private String RequestServerIP;
    private String RequestURL;
    private String tempRequestURL;

    public String getRequestCode() {
        return RequestCode;
    }

    public void setRequestCode(String requestCode) {
        RequestCode = requestCode;
    }

    public String getHTTPRequest() {
        return HTTPRequest;
    }

    public String getRequestServerIP() {
        return RequestServerIP;
    }

    public String getRequestURL() {
        return RequestURL;
    }

    public String getTempRequestURL() {
        return tempRequestURL;
    }

    public void setHTTPRequest(String HTTPRequest) {
        this.HTTPRequest = HTTPRequest;
    }

    public void setRequestServerIP(String requestServerIP) {
        RequestServerIP = requestServerIP;
    }

    public void setRequestURL(String requestURL) {
        RequestURL = requestURL;
    }

    public void setTempRequestURL(String tempRequestURL) {
        this.tempRequestURL = tempRequestURL;
    }
}
