package net.nicovrc.dev.data;

public class VimeoData {

    private String VideoURL;
    private String AudioURL;
    private String BaseURL;
    private long Bandwidth;
    private long AverageBandwidth;
    private String Codecs;
    private String Resolution;
    private String FrameRate;
    private String Audio;

    public String getVideoURL() {
        return VideoURL;
    }

    public void setVideoURL(String videoURL) {
        VideoURL = videoURL;
    }

    public String getAudioURL() {
        return AudioURL;
    }

    public void setAudioURL(String audioURL) {
        AudioURL = audioURL;
    }

    public String getBaseURL() {
        return BaseURL;
    }

    public void setBaseURL(String baseURL) {
        BaseURL = baseURL;
    }

    public long getBandwidth() {
        return Bandwidth;
    }

    public void setBandwidth(long bandwidth) {
        Bandwidth = bandwidth;
    }

    public long getAverageBandwidth() {
        return AverageBandwidth;
    }

    public void setAverageBandwidth(long averageBandwidth) {
        AverageBandwidth = averageBandwidth;
    }

    public String getCodecs() {
        return Codecs;
    }

    public void setCodecs(String codecs) {
        Codecs = codecs;
    }

    public String getResolution() {
        return Resolution;
    }

    public void setResolution(String resolution) {
        Resolution = resolution;
    }

    public String getFrameRate() {
        return FrameRate;
    }

    public void setFrameRate(String frameRate) {
        FrameRate = frameRate;
    }

    public String getAudio() {
        return Audio;
    }

    public void setAudio(String audio) {
        Audio = audio;
    }
}
