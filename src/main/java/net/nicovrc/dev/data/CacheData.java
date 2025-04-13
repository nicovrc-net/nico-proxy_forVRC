package net.nicovrc.dev.data;


public class CacheData {

    private Long CacheDate;
    private boolean isSet;
    private String targetURL;
    private boolean isRedirect;
    private String proxy;
    private String title;
    private byte[] HLS;
    private byte[] dummyHLS;

    public Long getCacheDate() {
        return CacheDate;
    }

    public void setCacheDate(Long cacheDate) {
        CacheDate = cacheDate;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    public String getTargetURL() {
        return targetURL;
    }

    public void setTargetURL(String targetURL) {
        this.targetURL = targetURL;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public void setRedirect(boolean isRedirect) {
        this.isRedirect = isRedirect;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public byte[] getHLS() {
        return HLS;
    }

    public void setHLS(byte[] HLS) {
        this.HLS = HLS;
    }

    public byte[] getDummyHLS() {
        return dummyHLS;
    }

    public void setDummyHLS(byte[] dummyHLS) {
        this.dummyHLS = dummyHLS;
    }
}