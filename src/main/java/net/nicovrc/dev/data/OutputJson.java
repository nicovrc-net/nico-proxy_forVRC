package net.nicovrc.dev.data;

import java.util.List;

public class OutputJson {

    private long ServerCount;
    private long MainProxyCount;
    private long SubProxyCount;
    private long CacheCount;
    private long WaitingWebhookSendCount;
    private long WaitingLogWriteCount;
    private List<String> ServiceURLList;

    public OutputJson(long serverCount, long mainProxyCount, long subProxyCount, long cacheCount, long waitingWebhookSendCount, long waitingLogWriteCount, List<String> serviceURLList) {
        ServerCount = serverCount;
        MainProxyCount = mainProxyCount;
        SubProxyCount = subProxyCount;
        CacheCount = cacheCount;
        WaitingWebhookSendCount = waitingWebhookSendCount;
        WaitingLogWriteCount = waitingLogWriteCount;
        ServiceURLList = serviceURLList;
    }

    public long getServerCount() {
        return ServerCount;
    }

    public void setServerCount(long serverCount) {
        ServerCount = serverCount;
    }

    public long getMainProxyCount() {
        return MainProxyCount;
    }

    public void setMainProxyCount(long mainProxyCount) {
        MainProxyCount = mainProxyCount;
    }

    public long getSubProxyCount() {
        return SubProxyCount;
    }

    public void setSubProxyCount(long subProxyCount) {
        SubProxyCount = subProxyCount;
    }

    public long getCacheCount() {
        return CacheCount;
    }

    public void setCacheCount(long cacheCount) {
        CacheCount = cacheCount;
    }

    public long getWaitingWebhookSendCount() {
        return WaitingWebhookSendCount;
    }

    public void setWaitingWebhookSendCount(long waitingWebhookSendCount) {
        WaitingWebhookSendCount = waitingWebhookSendCount;
    }

    public long getWaitingLogWriteCount() {
        return WaitingLogWriteCount;
    }

    public void setWaitingLogWriteCount(long waitingLogWriteCount) {
        WaitingLogWriteCount = waitingLogWriteCount;
    }

    public List<String> getServiceURLList() {
        return ServiceURLList;
    }

    public void setServiceURLList(List<String> serviceURLList) {
        ServiceURLList = serviceURLList;
    }
}
