package net.nicovrc.dev.data;

public class CacheData {
    private long ExpiryDate;
    private String CacheUrl;

    public CacheData(long expiryDate, String cacheUrl){
        this.ExpiryDate = expiryDate;
        this.CacheUrl = cacheUrl;
    }

    public long getExpiryDate() {
        return ExpiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        ExpiryDate = expiryDate;
    }

    public String getCacheUrl() {
        return CacheUrl;
    }

    public void setCacheUrl(String cacheUrl) {
        CacheUrl = cacheUrl;
    }
}
