package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;

import java.util.HashMap;
import java.util.List;

public class GetSupportList implements NicoVRCAPI {

    private final List<ServiceAPI> siteList = ServiceList.getServiceList();

    @Override
    public String getURI() {
        return "/api/v1/get_supportlist";
    }

    @Override
    public String Run(String httpRequest) {

        HashMap<String, String[]> map = new HashMap<>();
        siteList.forEach((value)->{
            map.put(value.getServiceName(), value.getCorrespondingURL());
        });

        return Function.gson.toJson(map);

    }
}
