package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.NicoVideo;
import net.nicovrc.dev.Service.ServiceAPI;

import java.util.ArrayList;
import java.util.List;

public class GetVersion implements NicoVRCAPI {

    private final List<ServiceAPI> siteList = new ArrayList<>();

    public GetVersion(){
        siteList.add(new NicoVideo());
    }

    @Override
    public String getURI() {
        return "/api/v1/get_version";
    }

    @Override
    public String Run(String httpRequest) {

        return "{\"Version\": \""+Function.Version+"\"}";

    }
}
