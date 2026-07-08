package net.nicovrc.dev.http;

import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.channels.AsynchronousSocketChannel;

public interface NicoVRCHTTP {

    String getStartURI();

    void setHTTPRequest(String httpRequest);
    void setURL(String URL);
    void setHTTPSocket(AsynchronousSocketChannel sock);
    void setHTTPClient(HttpClient client);
    void setProxy(String proxy);

}
