package xyz.n7mn.api;

public class ProxyData {
    private String ProxyIP;
    private int Port;

    public ProxyData(){

    }

    public ProxyData(String proxyIP, int port){
        this.ProxyIP = proxyIP;
        this.Port = port;
    }

    public String getProxyIP() {
        return ProxyIP;
    }

    public void setProxyIP(String proxyIP) {
        ProxyIP = proxyIP;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }
}
