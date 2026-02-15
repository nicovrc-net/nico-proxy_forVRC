package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

import java.net.http.HttpClient;

public class GetURL implements GetInterface {

    private final String httpRequest;
    private final String URI;
    private final HttpClient client;

    public GetURL(String httpRequest, String uri, HttpClient client){
        this.httpRequest = httpRequest;
        this.URI = uri;
        this.client = client;
    }

    @Override
    public byte[] run() {
        return Function.zeroByte;
    }
}
