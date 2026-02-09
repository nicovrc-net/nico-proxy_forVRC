package net.nicovrc.dev.Service;

import java.net.http.HttpClient;

public interface ServiceAPI {

    String[] getCorrespondingURL();
    void Set(String json, HttpClient client);
    String Get();
    String getServiceName();
    String getUseProxy();
}
