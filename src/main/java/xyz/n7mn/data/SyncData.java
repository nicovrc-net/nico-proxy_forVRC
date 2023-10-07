package xyz.n7mn.data;

public class SyncData {

    private String RequestURL;
    private String ResultURL;

    public SyncData(String requestURL, String resultURL){
        this.RequestURL = requestURL;
        this.ResultURL = resultURL;
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
}
