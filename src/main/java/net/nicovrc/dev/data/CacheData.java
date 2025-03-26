package net.nicovrc.dev.data;

import net.nicovrc.dev.Service.ServiceAPI;

public class CacheData {

    private ServiceAPI ServiceAPI;
    private Long CacheDate;
    private boolean isSet;
    private String ResultJson;

    public ServiceAPI getServiceAPI() {
        return ServiceAPI;
    }

    public void setServiceAPI(ServiceAPI serviceAPI) {
        ServiceAPI = serviceAPI;
    }

    public Long getCacheDate() {
        return CacheDate;
    }

    public void setCacheDate(Long cacheDate) {
        CacheDate = cacheDate;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    public String getResultJson() {
        return ResultJson;
    }

    public void setResultJson(String resultJson) {
        ResultJson = resultJson;
    }
}
