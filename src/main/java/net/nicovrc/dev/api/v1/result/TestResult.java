package net.nicovrc.dev.api.v1.result;

import net.nicovrc.dev.Function;

public class TestResult implements APIResult {

    private final String Message = "OK";
    private final String Version = Function.Version;

    public String getMessage() {
        return Message;
    }

    public String getVersion() {
        return Version;
    }

    @Override
    public void setErrorMessage() {
        // なにもしない
    }

    @Override
    public String getErrorMessage() {
        return null;
    }
}
