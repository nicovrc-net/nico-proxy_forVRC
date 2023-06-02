package xyz.n7mn.api.bilibili;

public class Playurl {
    private int quality;
    private int duration;
    private long expire_at;

    private Video[] video;
    private AudioResource[] audio_resource;

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getExpire_at() {
        return expire_at;
    }

    public void setExpire_at(long expire_at) {
        this.expire_at = expire_at;
    }

    public Video[] getVideo() {
        return video;
    }

    public void setVideo(Video[] video) {
        this.video = video;
    }

    public AudioResource[] getAudio_resource() {
        return audio_resource;
    }

    public void setAudio_resource(AudioResource[] audio_resource) {
        this.audio_resource = audio_resource;
    }
}
