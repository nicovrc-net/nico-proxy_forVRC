package net.nicovrc.dev.service;

import net.nicovrc.dev.data.VideoResult;

public class NicoNico implements VideoService{

    private String targetURL = null;
    private final VideoResult result = new VideoResult();

    @Override
    public void set(String URL) {
        this.targetURL = URL;
    }

    @Override
    public VideoResult run() {
        if (targetURL == null || targetURL.isEmpty()){
            result.setErrorMessage("URLが指定されていません");
            return result;
        }

        return result;
    }
}
