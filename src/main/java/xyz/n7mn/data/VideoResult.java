package xyz.n7mn.data;

public class VideoResult {

    private String ResultCode;

    private String Title;
    private String ResultURL;
    private String ErrorMessage;

    public String getResultCode() {
        return ResultCode;
    }

    public String getTitle() {
        return Title;
    }

    public String getResultURL() {
        return ResultURL;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setResultCode(String resultCode) {
        ResultCode = resultCode;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public void setResultURL(String resultURL) {
        ResultURL = resultURL;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }
}
