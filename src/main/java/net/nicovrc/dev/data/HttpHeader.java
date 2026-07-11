package net.nicovrc.dev.data;

import net.nicovrc.dev.Function;

import java.util.Date;

public class HttpHeader {

    String httpVersion;
    int code;
    String contentType;
    String contentEncoding;
    String AccessControlAllowOrigin;

    byte[] httpBody;

    String redirectUrl;
    long rangeStart;
    long rangeEnd;
    long rangeSize;

    final StringBuffer sb_header = new StringBuffer();
    final String httpHeader;

    public HttpHeader(){
        this.httpVersion = "1.1";
        this.code = 200;
        this.contentType = Function.contentType_textPlain;
        this.contentEncoding = null;
        this.AccessControlAllowOrigin = null;
        this.httpBody = "".getBytes();
        this.redirectUrl = null;
        this.rangeStart = -1;
        this.rangeEnd = -1;
        this.rangeSize = -1;

        sb_header.setLength(0);

        boolean isRange = false;

        //System.out.println(code);

        sb_header.append("HTTP/").append(httpVersion);
        sb_header.append(" ").append(code).append(" ");
        switch (code) {
            case 200 -> sb_header.append("OK");
            case 206 -> sb_header.append("Partial Content");
            case 302 -> sb_header.append("Found");
            case 400 -> sb_header.append("Bad Request");
            case 403 -> sb_header.append("Forbidden");
            case 404 -> sb_header.append("Not Found");
            case 405 -> sb_header.append("Method Not Allowed");
            case 503 -> sb_header.append("Service Unavailable");
        }
        sb_header.append("\r\n");

        sb_header.append("Content-Length: ").append(httpBody.length).append("\r\n");
        sb_header.append("Content-Type: ").append(contentType).append("\r\n");

        sb_header.append("Date: ").append(new Date()).append("\r\n");

        sb_header.append("\r\n");
        httpHeader = sb_header.toString();
        sb_header.setLength(0);
    }

    public HttpHeader(String httpVersion, int code, String contentType, String contentEncoding, String AccessControlAllowOrigin, byte[] httpBody, String redirectUrl, long rangeStart, long rangeEnd, long rangeSize) {
        this.httpVersion = httpVersion;
        this.code = code;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.AccessControlAllowOrigin = AccessControlAllowOrigin;
        this.httpBody = httpBody;
        this.redirectUrl = redirectUrl;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.rangeSize = rangeSize;

        sb_header.setLength(0);

        boolean isRange = rangeSize >= 0;

        //System.out.println(code);

        sb_header.append("HTTP/").append(httpVersion == null ? "1.1" : httpVersion);
        sb_header.append(" ").append(code).append(" ");
        switch (code) {
            case 200 -> sb_header.append("OK");
            case 206 -> sb_header.append("Partial Content");
            case 302 -> sb_header.append("Found");
            case 400 -> sb_header.append("Bad Request");
            case 403 -> sb_header.append("Forbidden");
            case 404 -> sb_header.append("Not Found");
            case 405 -> sb_header.append("Method Not Allowed");
            case 503 -> sb_header.append("Service Unavailable");
        }
        sb_header.append("\r\n");

        if (code != 302){
            if (AccessControlAllowOrigin != null){
                sb_header.append("Access-Control-Allow-Origin: ").append(AccessControlAllowOrigin).append("\r\n");
            }
            if (isRange){
                sb_header.append("Accept-Ranges: bytes\r\n");
            }
            sb_header.append("Content-Length: ").append(httpBody.length).append("\r\n");
            if (contentEncoding != null && !contentEncoding.isEmpty()) {
                sb_header.append("Content-Encoding: ").append(contentEncoding).append("\r\n");
            }
            sb_header.append("Content-Type: ").append(contentType).append("\r\n");

            if (isRange){
                sb_header.append("Content-Ranges: ").append(rangeStart).append("-").append(rangeEnd).append("/").append(rangeSize).append("\r\n");
            }
        }

        sb_header.append("Date: ").append(new Date()).append("\r\n");

        if (code == 302 && redirectUrl != null){
            sb_header.append("Location: ").append(redirectUrl).append("\r\n");
        }

        sb_header.append("\r\n");
        httpHeader = sb_header.toString();
        sb_header.setLength(0);
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getAccessControlAllowOrigin() {
        return AccessControlAllowOrigin;
    }

    public void setAccessControlAllowOrigin(String accessControlAllowOrigin) {
        AccessControlAllowOrigin = accessControlAllowOrigin;
    }

    public byte[] getHttpBody() {
        return httpBody;
    }

    public void setHttpBody(byte[] httpBody) {
        this.httpBody = httpBody;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public boolean isRange(){
        return rangeSize >= 0;
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(long rangeStart) {
        this.rangeStart = rangeStart;
    }

    public long getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(long rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public long getRangeSize() {
        return rangeSize;
    }

    public void setRangeSize(long rangeSize) {
        this.rangeSize = rangeSize;
    }

    @Override
    public String toString(){
        //System.out.println(httpHeader);
        return httpHeader;
    }
}
