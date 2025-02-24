package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;

public class GetVersion implements NicoVRCAPI {

    @Override
    public String getURI() {
        return "/api/v1/get_version";
    }

    @Override
    public String Run(String httpRequest) {

        return "{\"Version\": \""+Function.Version+"\"}";

    }
}
