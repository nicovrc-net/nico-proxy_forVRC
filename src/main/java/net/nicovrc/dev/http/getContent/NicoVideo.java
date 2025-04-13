package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.NicoNicoVideo;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicoVideo implements GetContent {

    private final Gson gson = Function.gson;

    private final Pattern hls_video = Pattern.compile("#EXT-X-STREAM-INF:BANDWIDTH=(\\d+),AVERAGE-BANDWIDTH=(\\d+),CODECS=\"(.+)\",RESOLUTION=(.+),FRAME-RATE=(.+),AUDIO=\"(.+)\"");
    private final Pattern hls_audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"(.+)\"");

    private final Pattern hlslive_video = Pattern.compile("#EXT-X-STREAM-INF:BANDWIDTH=(\\d+),AVERAGE-BANDWIDTH=(\\d+),CODECS=\"(.+)\",RESOLUTION=(.+),FRAME-RATE=(.+),AUDIO=\"(.+)\"");
    private final Pattern hlslive_audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"(.+)\"");

    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        String dummy_hlsText = null;
        String hlsText = null;
        String cookieText = null;

        try {
            NicoNicoVideo result = gson.fromJson(json, NicoNicoVideo.class);
            if (result != null) {
                if (result.getVideoURL() != null) {
                    // ニコ動
                    final StringBuilder sb = new StringBuilder();
                    result.getVideoAccessCookie().forEach((name, data) -> {
                        sb.append(name).append("=").append(data).append(";");
                    });
                    cookieText = sb.substring(0, sb.length() - 1);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI(result.getVideoURL()))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Cookie", cookieText)
                            .GET()
                            .build();

                    HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    String hls = send.body().replaceAll("https://delivery\\.domand\\.nicovideo\\.jp", "/https/cookie:[" + sb.substring(0, sb.length() - 1) + "]/delivery.domand.nicovideo.jp");

                    // LibVLC以外
                    sb.setLength(0);

                    Matcher matcher1 = hls_video.matcher(hls);
                    Matcher matcher2 = hls_audio.matcher(hls);
                    String tempHLS = "";
                    if (matcher1.find() && matcher2.find()) {
                        tempHLS = "#EXTM3U\n" +
                                "#EXT-X-VERSION:6\n" +
                                "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"" + matcher2.group(2) + "\"\n" +
                                "#EXT-X-STREAM-INF:BANDWIDTH=" + matcher1.group(1) + ",AVERAGE-BANDWIDTH=" + matcher1.group(2) + ",CODECS=\"" + matcher1.group(3) + "\",RESOLUTION=" + matcher1.group(4) + ",FRAME-RATE=" + matcher1.group(5) + ",AUDIO=\"audio\"\n" +
                                "dummy";

                    }

                    String[] split = tempHLS.split("\n");
                    split[split.length - 1] = "/dummy.m3u8?url=" + URL + "&dummy=true";

                    for (String str : split) {
                        sb.append(str).append("\n");
                    }

                    dummy_hlsText = sb.toString();
                    sb.setLength(0);
                    send = null;
                    request = null;

                    // ダミー出力以外の場合とLibVLCの場合
                    long MaxBandWidth = -1;
                    String MediaText = "";
                    int i = 0;
                    for (String str : hls.split("\n")) {
                        Matcher matcher = hls_video.matcher(str);
                        if (matcher.find()) {
                            long l = Long.parseLong(matcher.group(2));
                            //System.out.println(l);
                            if (MaxBandWidth <= l) {
                                MaxBandWidth = l;
                                MediaText = hls.split("\n")[i + 1];
                            }
                        }
                        i++;
                    }

                    sb.setLength(0);
                    matcher1 = hls_video.matcher(hls);
                    matcher2 = hls_audio.matcher(hls);
                    if (matcher1.find() && matcher2.find()) {
                        tempHLS = "#EXTM3U\n" +
                                "#EXT-X-VERSION:6\n" +
                                "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"" + matcher2.group(2) + "\"\n" +
                                "#EXT-X-STREAM-INF:BANDWIDTH=" + matcher1.group(1) + ",AVERAGE-BANDWIDTH=" + matcher1.group(2) + ",CODECS=\"" + matcher1.group(3) + "\",RESOLUTION=" + matcher1.group(4) + ",FRAME-RATE=" + matcher1.group(5) + ",AUDIO=\"audio\"\n" +
                                "dummy";

                    }

                    split = tempHLS.split("\n");
                    split[split.length - 1] = MediaText;

                    for (String str : split) {
                        sb.append(str).append("\n");
                    }
                    //System.out.println(sb);
                    hlsText = sb.toString();
                    sb.setLength(0);

                } else if (result.getLiveURL() != null) {
                    // ニコ生
                    String liveURL = result.getLiveURL();
                    if (result.getLiveAccessCookie() != null && !result.getLiveAccessCookie().isEmpty()) {
                        // 新鯖

                        final StringBuilder sb = new StringBuilder();
                        result.getLiveAccessCookie().forEach((name, value) -> {
                            sb.append(name).append("=").append(value).append("; ");
                        });

                        cookieText = sb.substring(0, sb.length() - 2);

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(liveURL))
                                .headers("User-Agent", Function.UserAgent)
                                .headers("Cookie", cookieText)
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").get() : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();
                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl")) {
                            String s = new String(body, StandardCharsets.UTF_8);
                            //System.out.println(s);
                            s = s.replaceAll("https://", "/https/cookie:[" + (sb.substring(0, sb.length() - 2) == null || sb.substring(0, sb.length() - 2).isEmpty() ? "" : sb.substring(0, sb.length() - 2)) + "]/");
                            body = s.getBytes(StandardCharsets.UTF_8);
                        }

                        hlsText = new String(body, StandardCharsets.UTF_8);

                        sb.setLength(0);

                        String hls = new String(body, StandardCharsets.UTF_8);
                        Matcher matcher1 = hlslive_video.matcher(hls);
                        Matcher matcher2 = hlslive_audio.matcher(hls);

                        if (matcher1.find() && matcher2.find()) {
                            hls = "#EXTM3U\n" +
                                    "#EXT-X-VERSION:6\n" +
                                    "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                    "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"main\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"" + matcher2.group(2) + "\"\n" +
                                    "#EXT-X-STREAM-INF:BANDWIDTH=" + matcher1.group(1) + ",AVERAGE-BANDWIDTH=" + matcher1.group(2) + ",CODECS=\"" + matcher1.group(3) + "\",RESOLUTION=" + matcher1.group(4) + ",FRAME-RATE=" + matcher1.group(5) + ",AUDIO=\"main\"\n" +
                                    "dummy";
                        }

                        String[] split = hls.split("\n");
                        split[split.length - 1] = "/dummy.m3u8?url=" + URL + "&dummy=true";

                        for (String str : split) {
                            sb.append(str).append("\n");
                        }

                        dummy_hlsText = sb.toString();

                    } else {
                        // dmc
                        String[] split = liveURL.split("/");

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(liveURL))
                                .headers("User-Agent", Function.UserAgent)
                                .GET()
                                .build();

                        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        String contentType = send.headers().firstValue("Content-Type").isEmpty() ? send.headers().firstValue("content-type").isPresent() ? send.headers().firstValue("content-type").get() : "" : send.headers().firstValue("Content-Type").get();
                        byte[] body = send.body();
                        if (contentType.toLowerCase(Locale.ROOT).equals("application/vnd.apple.mpegurl")) {
                            // https://liveedge231.dmc.nico/hlslive/ht2_nicolive/nicolive-production-pg130607675867723_4ad3364a300b5325d4b25e4013a11ea9a50ef78c26d1d643c642f4ab14b905d4/4/mp4/playlist.m3u8?ht2_nicolive=131256034.ggfv0cb1a7_ss31uh_1u5gfs7lsf63i&__poll__=0
                            String s = new String(body, StandardCharsets.UTF_8);
                            //System.out.println(liveURL.replaceAll(split[split.length - 1].replaceAll("\\?", "\\\\?"), ""));
                            String s1 = "/https/cookie:[]/" + (liveURL.replaceAll(split[split.length - 1].replaceAll("\\?", "\\\\?"), "").replaceAll("https://", ""));
                            s = s.replaceAll("1/ts/playlist\\.m3u8", s1 + "/1/ts/playlist.m3u8");
                            s = s.replaceAll("2/ts/playlist\\.m3u8", s1 + "/2/ts/playlist.m3u8");
                            s = s.replaceAll("3/ts/playlist\\.m3u8", s1 + "/3/ts/playlist.m3u8");
                            s = s.replaceAll("4/ts/playlist\\.m3u8", s1 + "/4/ts/playlist.m3u8");
                            s = s.replaceAll("5/ts/playlist\\.m3u8", s1 + "/5/ts/playlist.m3u8");
                            s = s.replaceAll("6/ts/playlist\\.m3u8", s1 + "/6/ts/playlist.m3u8");
                            body = s.getBytes(StandardCharsets.UTF_8);
                        }
                        hlsText = new String(body, StandardCharsets.UTF_8);
                    }

                } else {
                    File file = new File("./error-video/error_404_2.mp4");
                    byte[] content = new byte[0];
                    if (file.exists()) {
                        FileInputStream stream = new FileInputStream(file);
                        content = stream.readAllBytes();
                        stream.close();
                        stream = null;
                    }
                    //System.out.println(content.length);
                    file = null;
                    content = null;
                }
            }
        } catch (Exception e){
            // e.printStackTrace();
            try {
                byte[] content = null;
                File file = new File("./error-video/error_000.mp4");
                if (file.exists()){
                    FileInputStream stream = new FileInputStream(file);
                    content = stream.readAllBytes();
                    stream.close();
                    stream = null;
                }

                content = null;
            } catch (Exception ex){
                // ex.printStackTrace();
            }
        }

        ContentObject object = new ContentObject();
        object.setHLSText(hlsText);
        object.setDummyHLSText(dummy_hlsText);
        object.setCookieText(cookieText);
        return object;
    }
}
