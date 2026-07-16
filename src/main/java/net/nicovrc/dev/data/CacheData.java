package net.nicovrc.dev.data;


import net.nicovrc.dev.Function;

import java.util.Date;
import java.util.UUID;

public class CacheData {

    private final String cacheId;

    private Long CacheDate = null;
    private String URL = null;
    private String OriginURL = null;
    private String RedirectURL = null;
    private String proxy = null;
    private String title = null;
    private byte[] HLS = null;
    private byte[] Data = null;
    private String CookieText = null;
    private String RefererText = null;
    private String contentType = null;

    private boolean isRange = false;
    private Long RangeStart = null;
    private Long RangeEnd = null;
    private Long RangeLength = null;

    public CacheData() {
        this.cacheId = UUID.randomUUID().toString() + "_" + new Date().getTime();
    }

    public String getCacheId() {
        return cacheId;
    }

    public Long getCacheDate() {
        return CacheDate;
    }

    public void setCacheDate(Long cacheDate) {
        CacheDate = cacheDate;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getRedirectURL() {
        return RedirectURL;
    }

    public String getOriginURL() {
        return OriginURL;
    }

    public void setOriginURL(String OriginURL) {
        this.OriginURL = OriginURL;
    }

    public void setRedirectURL(String RedirectURL) {
        this.RedirectURL = RedirectURL;
    }

    public boolean isRedirect() {
        return RedirectURL != null;
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
        if (HLS == null){
            return Function.zeroByte;
        }
        return HLS;
    }

    public void setHLS(byte[] HLS) {
        this.HLS = HLS;
    }

    public byte[] getData() {
        return Data;
    }

    public void setData(byte[] data) {
        this.Data = data;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isHLS() {
        return HLS != null;
    }

    public boolean isRange() {
        return isRange;
    }

    public void setRange(boolean range) {
        this.isRange = range;
    }

    public Long getRangeStart() {
        return RangeStart;
    }

    public void setRangeStart(Long rangeStart) {
        this.RangeStart = rangeStart;
    }

    public Long getRangeEnd() {
        return RangeEnd;
    }

    public void setRangeEnd(Long rangeEnd) {
        this.RangeEnd = rangeEnd;
    }

    public Long getRangeLength() {
        return RangeLength;
    }

    public void setRangeLength(Long rangeLength) {
        this.RangeLength = rangeLength;
    }
}