package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GetURL implements Runnable, NicoVRCHTTP {

    private Socket sock = null;
    private String URL = null;
    private String httpRequest = null;

    @Override
    public void run() {

        StringBuilder sb = new StringBuilder("--- dummy ---\n");
        sb.append(httpRequest);

        String method = Function.getMethod(httpRequest);
        try {
            Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "text/plain; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getStartURI() {
        return "/?url=";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public void setURL(String URL) {
        this.URL = URL;
    }

    @Override
    public void setHTTPSocket(Socket sock) {
        this.sock = sock;
    }
}
