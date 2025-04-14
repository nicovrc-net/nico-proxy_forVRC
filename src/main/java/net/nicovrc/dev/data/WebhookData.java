package net.nicovrc.dev.data;

import java.util.Date;

public class WebhookData {

    private String URL = null;
    private String APIURI = null;
    private String Result = null;
    private String HTTPRequest = null;
    private Date Date = null;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getAPIURI() {
        return APIURI;
    }

    public void setAPIURI(String APIURI) {
        this.APIURI = APIURI;
    }

    public String getResult() {
        return Result;
    }

    public void setResult(String result) {
        Result = result;
    }

    public String getHTTPRequest() {
        return HTTPRequest;
    }

    public void setHTTPRequest(String HTTPRequest) {
        this.HTTPRequest = HTTPRequest;
    }

    public Date getDate() {
        return Date;
    }

    public void setDate(Date date) {
        Date = date;
    }
}
