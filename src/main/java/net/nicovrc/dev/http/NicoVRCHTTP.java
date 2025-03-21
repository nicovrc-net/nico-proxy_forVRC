package net.nicovrc.dev.http;

import java.net.Socket;

public interface NicoVRCHTTP {

    String getStartURI();

    void setHTTPRequest(String httpRequest);
    void setURL(String URL);
    void setHTTPSocket(Socket sock);

}
