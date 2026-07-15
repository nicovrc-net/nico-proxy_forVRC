package net.nicovrc.dev.http;

import java.net.http.HttpClient;
import java.nio.channels.AsynchronousSocketChannel;

public class GetVideo implements Runnable, NicoVRCHTTP {
    private String httpRequest = null;
    private String url = null;
    private AsynchronousSocketChannel ch = null;
    private HttpClient client = null;
    private String proxy = null;

    @Override
    public void run() {

    }

    @Override
    public String getStartURI() {
        return "/video";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public void setURL(String URL) {
        this.url = URL;
    }

    @Override
    public void setHTTPSocket(AsynchronousSocketChannel sock) {
        this.ch = sock;
    }

    @Override
    public void setHTTPClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }
}
