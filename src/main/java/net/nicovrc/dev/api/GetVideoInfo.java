package net.nicovrc.dev.api;

public class GetVideoInfo implements NicoVRCAPI {
    @Override
    public String getURI() {
        return "/api/v1/videoinfo";
    }

    @Override
    public String Run() {
        return "";
    }
}
