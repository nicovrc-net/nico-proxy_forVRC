package net.nicovrc.dev.api;

public class Test implements NicoVRCAPI {
    @Override
    public String getURI() {
        return "/api/v1/test";
    }

    @Override
    public String Run(String httpRequest) {
        return "{\"ErrorMessage\": \"実装中...\"}";
    }
}
