package net.nicovrc.dev.data;

public class ProxyData extends xyz.n7mn.nico_proxy.data.ProxyData {
    private String IP;
    private int Port;

    public ProxyData(){

    }
    public ProxyData(String ip, int port){
        this.IP = ip;
        this.Port = port;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    @Override
    public int getPort() {
        return Port;
    }

    @Override
    public void setPort(int port) {
        Port = port;
    }

    @Override
    public String getProxyIP() {
        return IP;
    }

    @Override
    public void setProxyIP(String proxyIP) {
        this.IP = proxyIP;
    }

}
