package net.nicovrc.dev.Service.Result;

public class ErrorMessage {

    private String ErrorMesage;

    public ErrorMessage(String str){
        ErrorMesage = str;
    }

    public String getErrorMesage() {
        return ErrorMesage;
    }

    public void setErrorMesage(String errorMesage) {
        ErrorMesage = errorMesage;
    }
}
