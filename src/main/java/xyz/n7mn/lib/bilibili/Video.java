package xyz.n7mn.lib.bilibili;

public class Video {
    private VideoResource video_resource;
    private Streaminfo stream_info;
    private int audio_quality;

    public VideoResource getVideo_resource() {
        return video_resource;
    }

    public void setVideo_resource(VideoResource video_resource) {
        this.video_resource = video_resource;
    }

    public Streaminfo getStream_info() {
        return stream_info;
    }

    public void setStream_info(Streaminfo stream_info) {
        this.stream_info = stream_info;
    }

    public int getAudio_quality() {
        return audio_quality;
    }

    public void setAudio_quality(int audio_quality) {
        this.audio_quality = audio_quality;
    }
}
