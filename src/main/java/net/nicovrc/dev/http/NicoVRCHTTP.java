package net.nicovrc.dev.http;

import java.net.Socket;
import java.net.http.HttpClient;

public interface NicoVRCHTTP {

    String getStartURI();

    void setHTTPRequest(String httpRequest);
    void setURL(String URL);
    void setHTTPSocket(Socket sock);
    void setHTTPClient(HttpClient client);

}
