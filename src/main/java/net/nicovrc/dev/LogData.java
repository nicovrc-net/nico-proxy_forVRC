package net.nicovrc.dev;

import java.util.Date;
import java.util.UUID;

public class LogData {

    private String LogID;
    private Long UnixTime;
    private String HTTPRequest;
    private String RequestURL;
    private String ResultURL;
    private String ResultTitle;
    private String ErrorMessage;

    public LogData(){
        LogID = UUID.randomUUID().toString() + "_" + new Date().getTime();
        UnixTime = new Date().getTime();
    }

    public LogData(String logID, Long unixTime, String HTTPRequest, String requestURL, String resultURL, String resultTitle, String errorMessage) {
        LogID = logID;
        UnixTime = unixTime;
        this.HTTPRequest = HTTPRequest;
        RequestURL = requestURL;
        ResultURL = resultURL;
        ResultTitle = resultTitle;
        ErrorMessage = errorMessage;
    }

    public String getLogID() {
        return LogID;
    }

    public void setLogID(String logID) {
        LogID = logID;
    }

    public Long getUnixTime() {
        return UnixTime;
    }

    public void setUnixTime(Long unixTime) {
        UnixTime = unixTime;
    }

    public String getHTTPRequest() {
        return HTTPRequest;
    }

    public void setHTTPRequest(String HTTPRequest) {
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

    public String getResultTitle() {
        return ResultTitle;
    }

    public void setResultTitle(String resultTitle) {
        ResultTitle = resultTitle;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }
}
