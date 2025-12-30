package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

public class GetAPI implements GetInterface {

    private final String URI;

    public GetAPI(String uri){
        this.URI = uri;
    }

    @Override
    public byte[] run() {
        return Function.zeroByte;
    }
}
