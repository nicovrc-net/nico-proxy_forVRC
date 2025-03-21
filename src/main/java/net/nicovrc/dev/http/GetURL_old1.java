package net.nicovrc.dev.http;

import java.net.Socket;

public class GetURL_old1 implements Runnable, NicoVRCHTTP {

    private final GetURL getURL = new GetURL();

    @Override
    public void run() {
        getURL.run();
    }

    @Override
    public String getStartURI() {
        return "/proxy/?";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {
        getURL.setHTTPRequest(httpRequest);
    }

    @Override
    public void setURL(String URL) {
        getURL.setURL(URL);
    }

    @Override
    public void setHTTPSocket(Socket sock) {
        getURL.setHTTPSocket(sock);
    }
}
