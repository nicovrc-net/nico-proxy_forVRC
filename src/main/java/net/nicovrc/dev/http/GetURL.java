package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

public class GetURL implements GetInterface {

    private final String URI;

    public GetURL(String uri){
        this.URI = uri;
    }

    @Override
    public byte[] run() {
        return Function.zeroByte;
    }
}
