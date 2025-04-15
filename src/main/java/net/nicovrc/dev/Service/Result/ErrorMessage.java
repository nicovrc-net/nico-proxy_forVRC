package net.nicovrc.dev.Service.Result;

public class ErrorMessage {

    private String ErrorMessage;

    public ErrorMessage(String str){
        ErrorMessage = str;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }
}
