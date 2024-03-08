package net.nicovrc.dev.data;

public class ProxyData {
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

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }
}
