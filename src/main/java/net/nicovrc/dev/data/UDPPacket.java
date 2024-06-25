package net.nicovrc.dev.data;

import net.nicovrc.dev.api.Constant;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

public class UDPPacket {

    private String RequestURL;
    private String TempRequestURL;
    private String ResultURL;
    private boolean isGetTitle;
    private String HTTPRequest;
    private String ErrorMessage;

    private String RequestID;

    public UDPPacket(){

    }

    public UDPPacket(String requestURL){
        this.RequestURL = requestURL;
    }

    public UDPPacket(String requestURL, String tempRequestURL){
        this.RequestURL = requestURL;
        this.TempRequestURL = tempRequestURL;

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

    public String getTempRequestURL() {
        return TempRequestURL;
    }

    public void setTempRequestURL(String tempRequestURL) {
        TempRequestURL = tempRequestURL;
    }

    public void setRequestID(String requestID) {
        RequestID = requestID;
    }

    public String getRequestID() {
        return RequestID;
    }
}
