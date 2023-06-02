package xyz.n7mn.api;

public interface ShareService {

    String getVideo(String url, ProxyData proxy) throws Exception;
    String getLive(String url, ProxyData proxy) throws Exception;

    String getServiceName();
    String getVersion();

}
