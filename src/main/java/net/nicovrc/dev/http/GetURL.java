package net.nicovrc.dev.http;

import java.net.Socket;

public class GetURL implements Runnable, NicoVRCHTTP {

    @Override
    public void run() {

    }

    @Override
    public String getStartURI() {
        return "/?url=";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {

    }

    @Override
    public void setURL(String URL) {

    }

    @Override
    public void setHTTPSocket(Socket sock) {

    }
}
