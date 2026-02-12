package net.nicovrc.dev.http;

import java.net.Socket;
import java.net.http.HttpClient;

public class GetURL_dummy implements Runnable, NicoVRCHTTP {

    private final GetURL getURL = new GetURL();

    @Override
    public void run() {
        getURL.run();
    }

    @Override
    public String getStartURI() {
        return "/?dummy=";
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

    @Override
    public void setHTTPClient(HttpClient client) {
        getURL.setHTTPClient(client);
    }
}
