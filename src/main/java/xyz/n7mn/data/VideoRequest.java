package xyz.n7mn.data;

import java.util.List;

public class VideoRequest {

    private String RequestCode;
    private String HTTPRequest;
    private String ServerIP;
    private String RequestURL;
    private String TempRequestURL;

    private List<String> ProxyListVideo;
    private List<String> ProxyListOfficial;

    private String TwitcastClientId;
    private String TwitcastClientSecret;

    public VideoRequest(){

    }

    public VideoRequest(String requestCode, String HTTPRequest, String serverIP, String requestURL, String tempRequestURL, List<String> proxyListVideo, List<String> proxyListOfficial, String twitcastClientId, String twitcastClientSecret){
        this.RequestCode = requestCode;
        this.HTTPRequest = HTTPRequest;
        this.ServerIP = serverIP;
        this.RequestURL = requestURL;
        this.TempRequestURL = tempRequestURL;
        this.ProxyListVideo = proxyListVideo;
        this.ProxyListOfficial = proxyListOfficial;
        this.TwitcastClientId = twitcastClientId;
        this.TwitcastClientSecret = twitcastClientSecret;
    }

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

    public List<String> getProxyListVideo() {
        return ProxyListVideo;
    }

    public void setProxyListVideo(List<String> proxyList_Video) {
        ProxyListVideo = proxyList_Video;
    }

    public List<String> getProxyListOfficial() {
        return ProxyListOfficial;
    }

    public void setProxyListOfficial(List<String> proxyList_Official) {
        ProxyListOfficial = proxyList_Official;
    }

    public String getTwitcastClientId() {
        return TwitcastClientId;
    }

    public void setTwitcastClientId(String twitcastClientId) {
        TwitcastClientId = twitcastClientId;
    }

    public String getTwitcastClientSecret() {
        return TwitcastClientSecret;
    }

    public void setTwitcastClientSecret(String twitcastClientSecret) {
        TwitcastClientSecret = twitcastClientSecret;
    }
}
