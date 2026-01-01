package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

public class GetVideo implements GetInterface {

    private final String httpRequest;
    private final String URI;

    public GetVideo(String httpRequest, String uri){
        this.httpRequest = httpRequest;
        this.URI = uri;
    }

    @Override
    public byte[] run() {
        return Function.zeroByte;
    }
}
