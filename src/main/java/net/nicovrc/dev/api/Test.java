package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;

import java.net.http.HttpClient;

public class Test implements NicoVRCAPI {
    @Override
    public String getURI() {
        return "/api/v1/test";
    }

    @Override
    public String Run(String httpRequest, HttpClient client) {
        return "{\"Message\": \"OK\", \"Version\": \""+ Function.Version +"\"}";
    }
}
