package net.nicovrc.dev.api;


import java.net.http.HttpClient;

public interface NicoVRCAPI {

    String getURI();
    String Run(String httpRequest, HttpClient client) throws Exception;

}
