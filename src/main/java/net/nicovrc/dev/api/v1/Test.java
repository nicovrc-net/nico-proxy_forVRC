package net.nicovrc.dev.api.v1;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.api.WebAPI;
import net.nicovrc.dev.api.v1.result.TestResult;

public class Test implements WebAPI {

    private final String resultJson;

    public Test(){
        resultJson = Function.gson.toJson(new TestResult());
    }

    @Override
    public String run() {
        return resultJson;
    }
}
