package net.nicovrc.dev.data;

public class UDPPacket {

    private String RequestURL;
    private String ResultURL;
    private boolean isGetTitle;
    private String HTTPRequest;
    private String ErrorMessage;

    public UDPPacket(String requestURL, boolean isGetTitle){
        this.RequestURL = requestURL;
        this.isGetTitle = isGetTitle;
    }

    public UDPPacket(String requestURL, String HTTPRequest, boolean isGetTitle){
        this.RequestURL = requestURL;
        this.HTTPRequest = HTTPRequest;
        this.isGetTitle = isGetTitle;
    }

    public UDPPacket(String resultURL){
        this.ResultURL = resultURL;
    }

    public UDPPacket(String requestURL, String resultURL, String HTTPRequest, boolean isGetTitle){
        this.RequestURL = requestURL;
        this.ResultURL = resultURL;
        this.isGetTitle = isGetTitle;
        this.HTTPRequest = HTTPRequest;
    }

    public String getRequestURL() {
        return RequestURL;
    }

    public void setRequestURL(String requestURL) {
        RequestURL = requestURL;
    }

    public String getResultURL() {
        return ResultURL;
    }

    public void setResultURL(String resultURL) {
        ResultURL = resultURL;
    }

    public boolean isGetTitle() {
        return isGetTitle;
    }

    public void setGetTitle(boolean getTitle) {
        isGetTitle = getTitle;
    }

    public String getHTTPRequest() {
        return HTTPRequest;
    }

    public void setHTTPRequest(String HTTPRequest) {
        this.HTTPRequest = HTTPRequest;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }
}
