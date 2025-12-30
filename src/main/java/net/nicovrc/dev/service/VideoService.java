package net.nicovrc.dev.service;

import net.nicovrc.dev.data.VideoResult;

public interface VideoService {

    void set(String URL);
    VideoResult run();

}
