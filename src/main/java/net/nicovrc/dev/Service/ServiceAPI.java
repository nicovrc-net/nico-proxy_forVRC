package net.nicovrc.dev.Service;

import java.net.http.HttpClient;

public interface ServiceAPI {

    void setHttpClient(HttpClient client);
    void setURL(String URL);
    void setToken(String[] token);
    void setProxy(String proxy);
    String get();
    String getServiceName();
    String[] getCorrespondingURL();
}
