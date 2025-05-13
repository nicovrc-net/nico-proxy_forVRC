package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;

public class AddCache implements NicoVRCAPI {
    @Override
    public String getURI() {
        return "/api/v1/add_cache";
    }

    @Override
    public String Run(String httpRequest) {
        return "{\"Message\": \"作成中...\"}";
    }
}
