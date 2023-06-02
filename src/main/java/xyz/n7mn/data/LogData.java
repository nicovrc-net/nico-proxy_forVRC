package xyz.n7mn.data;

import java.util.Date;

public class LogData {
    private String LogID;
    private Date Date;
    private String HTTPRequest;
    private String RequestIP;
    private String RequestURL;
    private String ResultURL;
    private String ErrorMessage;

    public LogData(){

    }

    public LogData(String logID, Date date, String HTTPRequest, String requestIP, String requestURL, String resultURL, String errorMessage){
        this.LogID = logID;
        this.Date = date;
        this.HTTPRequest = HTTPRequest;
        this.RequestIP = requestIP;
        this.RequestURL = requestURL;
        this.ResultURL = resultURL;
        this.ErrorMessage = errorMessage;
    }

    public String getLogID() {
        return LogID;
    }

    public void setLogID(String logID) {
        LogID = logID;
    }

    public java.util.Date getDate() {
        return Date;
    }

    public void setDate(java.util.Date date) {
        Date = date;
    }

    public String getHTTPRequest() {
        return HTTPRequest;
    }

    public void setHTTPRequest(String HTTPRequest) {
        this.HTTPRequest = HTTPRequest;
    }

    public String getRequestIP() {
        return RequestIP;
    }

    public void setRequestIP(String requestIP) {
        RequestIP = requestIP;
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

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }
}
