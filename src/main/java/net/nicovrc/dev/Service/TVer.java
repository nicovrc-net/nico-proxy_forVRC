package net.nicovrc.dev.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.TVerResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVer implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern Support_URLVideo1 = Pattern.compile("https://tver\\.jp/episodes/(.+)");
    private final Pattern Support_URLLive1 = Pattern.compile("https://tver\\.jp/live/(.+)");
    private final Pattern Support_URLLive2 = Pattern.compile("https://tver\\.jp/live/simul/(.+)");
    private final Pattern Support_URLLive3 = Pattern.compile("https://tver.jp/live/special/(.+)");
    private final Pattern Support_Olympic = Pattern.compile("https://tver\\.jp/olympic/(.+)/live/play/(.+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"tver.jp"};
    }

    @Override
    public void setHttpClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setURL(String URL) {
        this.url = URL;
    }

    @Override
    public void setToken(String[] token) {

    }

    @Override
    public void setProxy(String proxy) {

    }

    @Override
    public String get() {
        Matcher matcher1 = Support_URLVideo1.matcher(url);
        Matcher matcher2 = Support_URLLive1.matcher(url);
        Matcher matcher3 = Support_URLLive2.matcher(url);
        Matcher matcher4 = Support_URLLive3.matcher(url);
        Matcher matcher5 = Support_Olympic.matcher(url);

        final boolean video1 = matcher1.find();
        final boolean live1 = matcher2.find();
        final boolean live2 = matcher3.find();
        final boolean live3 = matcher4.find();
        final boolean olympic = matcher5.find();

        if (!video1 && !live1 && !live2 && !live3 && !olympic){
            //client.close();
            return Function.gson.toJson(new ErrorMessage("対応してないURLです。"));
        }

        try {
            if (video1){
                URI uri = new URI("https://statics.tver.jp/content/episode/"+matcher1.group(1)+".json?v=20");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                String text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }

                //System.out.println(send.body());
                //System.out.println(text);

                if (!text.startsWith("{") || !text.endsWith("}")){
                    //client.close();
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }

                JsonElement json = Function.gson.fromJson(text, JsonElement.class);
                String projectID = json.getAsJsonObject().get("streaks").getAsJsonObject().get("projectID").getAsString();
                String videoRefID = json.getAsJsonObject().get("streaks").getAsJsonObject().get("videoRefID").getAsString();
                String channel = json.getAsJsonObject().get("video").getAsJsonObject().get("channelID").getAsString();
                //System.out.println(channel);

                //System.out.println(json);
                if (channel.equals("local")){
                    if (videoRefID.startsWith("cbc") || videoRefID.startsWith("ctc") || videoRefID.startsWith("mxtv")){
                        channel = "mcc";
                    }
                }

                //return send.body();

                request = HttpRequest.newBuilder()
                        .uri(new URI("https://player.tver.jp/player/ad_template.json"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);

                // System.out.println(json);
                String ati = json.getAsJsonObject().get(projectID).getAsJsonObject().get("pc").getAsString();

                //
                request = HttpRequest.newBuilder()
                        .uri(new URI("https://player.tver.jp/player/streaks_info_v2.json"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);

                boolean isFound = false;
                int i = 1;
                while (!isFound){

                    //System.out.println(channel);
                    if (channel.startsWith("ntv")){
                        channel = "ntv";
                    }
                    if (channel.equals("local")){
                        if (videoRefID.startsWith("cbc") || videoRefID.startsWith("ctc") || videoRefID.startsWith("mxtv")){
                            channel = "mcc";
                        }
                    }
                    String key = json.getAsJsonObject().get("tver-"+channel).getAsJsonObject().getAsJsonObject().get("api_key").getAsJsonObject().get("key0"+i).getAsString();

                    request = HttpRequest.newBuilder()
                            .uri(new URI("https://playback.api.streaks.jp/v1/projects/"+projectID+"/medias/ref:"+videoRefID+"?ati="+ati))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Accept-Encoding", "gzip, br")
                            .headers("Origin", "https://tver.jp")
                            .headers("Referer", "https://tver.jp/")
                            .headers("X-Streaks-Api-Key", key)
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                    text = "{}";
                    if (!contentEncoding.isEmpty()){
                        byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                        text = new String(bytes, StandardCharsets.UTF_8);
                    } else {
                        text = new String(send.body(), StandardCharsets.UTF_8);
                    }

                    if (send.statusCode() >= 200 && send.statusCode() <= 399){
                        isFound = true;
                    }

                    i++;
                }

                json = Function.gson.fromJson(text, JsonElement.class);

                //System.out.println(json);

                if (json.isJsonObject() && json.getAsJsonObject().has("sources")){
                    TVerResult result = new TVerResult();
                    result.setURL(url);
                    result.setTitle(json.getAsJsonObject().get("name").getAsString());
                    result.setDescription(json.getAsJsonObject().get("description").getAsString());
                    result.setDuration(json.getAsJsonObject().get("duration").getAsLong());
                    result.setThumbnail(json.getAsJsonObject().get("thumbnail").getAsJsonObject().get("src").getAsString());

                    result.setVideoURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());
                    //return send.body();
                    //client.close();
                    return Function.gson.toJson(result);
                } else {
                    //client.close();
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }
            }

            if (live1 && !live2 && !live3){

                TVerResult result = new TVerResult();

                // ntv
                String id = matcher2.group(1);

                URI uri = new URI("https://service-api.tver.jp/api/v1/callLiveTimeline/"+id);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("x-tver-platform-type", "web")
                        .GET()
                        .build();

                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                String text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }

                //System.out.println(send.body());
                JsonElement json = Function.gson.fromJson(text, JsonElement.class);
                if (!json.getAsJsonObject().has("result")){
                    //client.close();
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }

                String videoId = null;
                int version = 0;
                for (JsonElement element : json.getAsJsonObject().get("result").getAsJsonObject().get("contents").getAsJsonArray()) {
                    long start = element.getAsJsonObject().get("content").getAsJsonObject().get("startAt").getAsLong();
                    long end = element.getAsJsonObject().get("content").getAsJsonObject().get("endAt").getAsLong();

                    long time = new Date().getTime() / 1000;

                    //System.out.println(start + " ---> " + time + " ---> " + end);
                    if (start <= time && end >= time){
                        videoId = element.getAsJsonObject().get("content").getAsJsonObject().get("id").getAsString();
                        version = element.getAsJsonObject().get("content").getAsJsonObject().get("version").getAsInt();
                        result.setTitle(element.getAsJsonObject().get("content").getAsJsonObject().get("seriesTitle").getAsString() + " " + element.getAsJsonObject().get("content").getAsJsonObject().get("title").getAsString());
                        break;
                    }
                }

                uri = new URI("https://statics.tver.jp/content/live/"+videoId+".json?v="+version);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", id)
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);

                String programKey = json.getAsJsonObject().get("mrss").getAsJsonObject().get("programKey").getAsString();
                String programCategory = json.getAsJsonObject().get("mrss").getAsJsonObject().get("programCategory").getAsString();
                String episodeCode = json.getAsJsonObject().get("mrss").getAsJsonObject().get("episodeCode").getAsString();
                String episodeSIEventID = json.getAsJsonObject().get("mrss").getAsJsonObject().get("episodeSIEventID").getAsString();

                result.setDescription(json.getAsJsonObject().get("description").getAsString());

                uri = new URI("https://playback.api.streaks.jp/v1/projects/tver-simul-"+id+"/medias/ref:simul-"+id);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", id)
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }

                if (!text.startsWith("{") || !text.endsWith("}")){
                    //client.close();
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }

                json = Function.gson.fromJson(text, JsonElement.class);

                //System.out.println(json);
                //System.out.println(videoId);

                String hlsURL = json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString();
                String session = json.getAsJsonObject().getAsJsonObject().get("id").getAsString();
                String sessionId = json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();


                uri = new URI("https://ssai.api.streaks.jp/v1/projects/tver-simul-"+id+"/medias/"+session+"/ssai/session");
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"ads_params\":{\"tvcu_pcode\":\"\",\"tvcu_ccode\":\"\",\"tvcu_zcode\":\"\",\"tvcu_gender\":\"\",\"tvcu_gender_code\":\"\",\"tvcu_age\":\"\",\"tvcu_agegrp\":\"\",\"delivery_type\":\"simul\",\"is_dvr\":0,\"rdid\":\"\",\"idtype\":\"\",\"is_lat\":\"\",\"bundle\":\"\",\"interest\":\"\",\"video_id\":\""+videoId+"\",\"device\":\"pc\",\"device_code\":\"0001\",\"tag_type\":\"browser\",\"item_eventid\":\""+episodeSIEventID+"\",\"item_programkey\":\""+programKey+"\",\"item_category\":\""+programCategory+"\",\"item_episodecode\":\""+episodeCode+"\",\"item_originalmeta1\":\"\",\"item_originalmeta2\":\"\",\"ntv_ppid\":\"z75i3v2w5d0c3173d071452f85c1dd9f450d10d1ff57\",\"tbs_ppid\":\"f87wu4in5d0c3173d071452f85c1dd9f450d10d1ff57\",\"tx_ppid\":\"t87wrus65d0c3173d071452f85c1dd9f450d10d1ff57\",\"ex_ppid\":\"n6dsf79v5d0c3173d071452f85c1dd9f450d10d1ff57\",\"cx_ppid_gam\":\"b8a35iwj5d0c3173d071452f85c1dd9f450d10d1ff57\",\"mbs_ppid_gam\":\"x32ck84s5d0c3173d071452f85c1dd9f450d10d1ff57\",\"abc_ppid\":\"c2fq84em5d0c3173d071452f85c1dd9f450d10d1ff57\",\"tvo_ppid\":\"i3wtqjey5d0c3173d071452f85c1dd9f450d10d1ff57\",\"ktv_ppid\":\"g9byn7re5d0c3173d071452f85c1dd9f450d10d1ff57\",\"ytv_ppid\":\"g8kusm765d0c3173d071452f85c1dd9f450d10d1ff57\",\"ntv_ppid2\":\"z75i3v2w_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tbs_ppid2\":\"f87wu4in_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tx_ppid2\":\"t87wrus6_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"ex_ppid2\":\"n6dsf79v_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"cx_ppid2\":\"b8a35iwj_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"mbs_ppid2\":\"x32ck84s_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"abc_ppid2\":\"c2fq84em_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tvo_ppid2\":\"i3wtqjey_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"ktv_ppid2\":\"g9byn7re_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"ytv_ppid2\":\"g8kusm76_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"vr_uuid\":\"993860D1-AC20-4F22-844B-443395E052A0\",\"personalIsLat\":\"0\",\"platformAdUid\":\"33512098-bab6-40be-91fc-8bf15ea01f5b\",\"platformUid\":\"5d0c3173d071452f85c1dd9f450d10d1ff57\",\"memberId\":\"\",\"c\":\"simul\",\"luid\":\"993860D1-AC20-4F22-844B-443395E052A0\",\"platformVrUid\":\"768126a8cc5be8411a221c5bc7d28bc00f61d3b11867f9423b379ec8fc6e35f8\"},\"id\":\""+sessionId+"\"}"))
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);

                //System.out.println(json);
                //System.out.println(send.statusCode());

                result.setLiveURL(hlsURL + "&"+json.getAsJsonArray().get(0).getAsJsonObject().get("query").getAsString());

                //client.close();
                return Function.gson.toJson(result);
            }

            if (live2){
                TVerResult result = new TVerResult();

                String id = matcher3.group(1);
                int version = 0;
                URI uri = null;
                HttpRequest request = null;
                HttpResponse<byte[]> send = null;
                String contentEncoding = null;
                String text = "{}";
                JsonElement json = null;

                try {
                    uri = new URI("https://cf-platform-api.tver.jp/service/api/v1/callLiveEpisode/"+id+"?platform_uid=5d0c3173d071452f85c1dd9f450d10d1ff57&platform_token=asvezo2asd4m6v0vsvcf7rhe8n9mv6odwlov7jas&require_data=mylist");
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Accept-Encoding", "gzip, br")
                            .headers("Origin", "https://tver.jp")
                            .headers("Referer", "https://tver.jp/")
                            .headers("x-tver-platform-type", "web")
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                    if (!contentEncoding.isEmpty()){
                        byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                        text = new String(bytes, StandardCharsets.UTF_8);
                    } else {
                        text = new String(send.body(), StandardCharsets.UTF_8);
                    }

                    json = Function.gson.fromJson(text, JsonElement.class);
                    version = json.getAsJsonObject().get("result").getAsJsonObject().get("episode").getAsJsonObject().get("content").getAsJsonObject().get("version").getAsInt();

                } catch (Exception e) {
                    //e.printStackTrace();
                }

                if (version == 0){
                    uri = new URI("https://platform-api.tver.jp/service/api/v1/callLiveEpisode/"+id+"?platform_uid=5d0c3173d071452f85c1dd9f450d10d1ff57&platform_token=asvezo2asd4m6v0vsvcf7rhe8n9mv6odwlov7jas&require_data=mylist");
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Accept-Encoding", "gzip, br")
                            .headers("Origin", "https://tver.jp")
                            .headers("Referer", "https://tver.jp/")
                            .headers("x-tver-platform-type", "web")
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                    if (!contentEncoding.isEmpty()){
                        byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                        text = new String(bytes, StandardCharsets.UTF_8);
                    } else {
                        text = new String(send.body(), StandardCharsets.UTF_8);
                    }

                    //System.out.println(id);
                    json = Function.gson.fromJson(text, JsonElement.class);
                    //System.out.println(json);
                    version = json.getAsJsonObject().get("result").getAsJsonObject().get("episode").getAsJsonObject().get("content").getAsJsonObject().get("version").getAsInt();

                }

                //System.out.println(version);

                uri = new URI("https://statics.tver.jp/content/live/"+id+".json?v="+version);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", id)
                        .GET()
                        .build();

                if (version == 19){
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Accept-Encoding", "gzip, br")
                            .headers("Origin", "https://tver.jp")
                            .headers("Referer", "https://tver.jp/")
                            //.headers("X-Streaks-Api-Key", id)
                            .GET()
                            .build();
                }

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                //System.out.println(text);
                json = Function.gson.fromJson(text, JsonElement.class);
                //System.out.println(json);

                String projectID = "";
                String mediaID = "";
                String apiKey = "";
                result.setTitle(json.getAsJsonObject().get("title").getAsString());
                result.setDescription(json.getAsJsonObject().get("description").getAsString());

                if (json.getAsJsonObject().has("liveVideo")){
                    projectID = json.getAsJsonObject().get("liveVideo").getAsJsonObject().get("projectID").getAsString();
                    mediaID = json.getAsJsonObject().get("liveVideo").getAsJsonObject().get("mediaID").getAsString();
                    apiKey = json.getAsJsonObject().get("liveVideo").getAsJsonObject().get("apiKey").getAsString();
                } else {
                    projectID = json.getAsJsonObject().get("dvrVideo").getAsJsonObject().get("projectID").getAsString();
                    mediaID = json.getAsJsonObject().get("dvrVideo").getAsJsonObject().get("mediaID").getAsString();
                    apiKey = json.getAsJsonObject().get("dvrVideo").getAsJsonObject().get("apiKey").getAsString();
                }

                String vId = null;
                uri = new URI("https://playback.api.streaks.jp/v1/projects/"+projectID+"/medias/"+mediaID);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", apiKey)
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);
                vId = json.getAsJsonObject().get("id").getAsString();

                String videoId = null;

                uri = new URI("https://statics.tver.jp/content/live/"+id+".json?v="+version);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();
                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);

                projectID = json.getAsJsonObject().get("liveVideo").getAsJsonObject().get("projectID").getAsString();
                mediaID = json.getAsJsonObject().get("liveVideo").getAsJsonObject().get("mediaID").getAsString();

                result.setTitle(json.getAsJsonObject().get("title").getAsString());
                result.setDescription(json.getAsJsonObject().get("description").getAsString());

                request = HttpRequest.newBuilder()
                        .uri(new URI("https://ssai.api.streaks.jp/v1/projects/"+projectID+"/medias/"+vId+"/ssai/session"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Referer", "https://tver.jp/")
                        .headers("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"ads_params\":{\"tvcu_pcode\":\"\",\"tvcu_ccode\":\"\",\"tvcu_zcode\":\"\",\"tvcu_gender\":\"\",\"tvcu_gender_code\":\"\",\"tvcu_age\":\"\",\"tvcu_agegrp\":\"\",\"tvcu_params\":\"custom%255Btvpai%255D%3D%26custom%255Btvpbc%255D%3D%26custom%255Btvpcua%255D%3Dex2%26custom%255Btvpd%255D%3D%26custom%255Btvpg%255D%3D%26custom%255Btvpmc%255D%3D%26custom%255Btvpuid%255D%3D33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tvcu_params_e\":\"custom%25255Btvpai%25255D%253D%2526custom%25255Btvpbc%25255D%253D%2526custom%25255Btvpcua%25255D%253Dex2%2526custom%25255Btvpd%25255D%253D%2526custom%25255Btvpg%25255D%253D%2526custom%25255Btvpmc%25255D%253D%2526custom%25255Btvpuid%25255D%253D33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tvcu_params_ee\":\"custom%2525255Btvpai%2525255D%25253D%252526custom%2525255Btvpbc%2525255D%25253D%252526custom%2525255Btvpcua%2525255D%25253Dex2%252526custom%2525255Btvpd%2525255D%25253D%252526custom%2525255Btvpg%2525255D%25253D%252526custom%2525255Btvpmc%2525255D%25253D%252526custom%2525255Btvpuid%2525255D%25253D33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tvcu_params_eee\":\"custom%252525255Btvpai%252525255D%2525253D%25252526custom%252525255Btvpbc%252525255D%2525253D%25252526custom%252525255Btvpcua%252525255D%2525253Dex2%25252526custom%252525255Btvpd%252525255D%2525253D%25252526custom%252525255Btvpg%252525255D%2525253D%25252526custom%252525255Btvpmc%252525255D%2525253D%25252526custom%252525255Btvpuid%252525255D%2525253D33512098-bab6-40be-91fc-8bf15ea01f5b\",\"delivery_type\":\"simul\",\"is_dvr\":\"0\",\"rdid\":\"\",\"idtype\":\"\",\"is_lat\":\"\",\"bundle\":\"\",\"interest\":\"\",\"video_id\":\"leu50mphiv\",\"device\":\"pc\",\"device_code\":\"0001\",\"tag_type\":\"browser\",\"car\":\"0\",\"item_eventid\":\"46252\",\"item_programkey\":\"202607H002\",\"item_category\":\"5\",\"item_episodecode\":\"00000_0000000_260720B4AC\",\"item_originalmeta1\":\"26712_2119731\",\"item_originalmeta2\":\"\",\"ntv_ppid\":\"z75i3v2w5d0c3173d071452f85c1dd9f450d10d1ff57\",\"tbs_ppid\":\"f87wu4in5d0c3173d071452f85c1dd9f450d10d1ff57\",\"tx_ppid\":\"t87wrus65d0c3173d071452f85c1dd9f450d10d1ff57\",\"ex_ppid\":\"n6dsf79v5d0c3173d071452f85c1dd9f450d10d1ff57\",\"cx_ppid_gam\":\"b8a35iwj5d0c3173d071452f85c1dd9f450d10d1ff57\",\"mbs_ppid_gam\":\"x32ck84s5d0c3173d071452f85c1dd9f450d10d1ff57\",\"abc_ppid\":\"c2fq84em5d0c3173d071452f85c1dd9f450d10d1ff57\",\"tvo_ppid\":\"i3wtqjey5d0c3173d071452f85c1dd9f450d10d1ff57\",\"ktv_ppid\":\"g9byn7re5d0c3173d071452f85c1dd9f450d10d1ff57\",\"ytv_ppid\":\"g8kusm765d0c3173d071452f85c1dd9f450d10d1ff57\",\"ntv_ppid2\":\"z75i3v2w_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tbs_ppid2\":\"f87wu4in_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tx_ppid2\":\"t87wrus6_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"ex_ppid2\":\"n6dsf79v_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"cx_ppid2\":\"b8a35iwj_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"mbs_ppid2\":\"x32ck84s_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"abc_ppid2\":\"c2fq84em_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"tvo_ppid2\":\"i3wtqjey_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"ktv_ppid2\":\"g9byn7re_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"ytv_ppid2\":\"g8kusm76_33512098-bab6-40be-91fc-8bf15ea01f5b\",\"vr_uuid\":\"D733BF66-6B1E-422B-895F-524FF083BD4F\",\"personalIsLat\":\"0\",\"platformAdUid\":\"33512098-bab6-40be-91fc-8bf15ea01f5b\",\"platformUid\":\"5d0c3173d071452f85c1dd9f450d10d1ff57\",\"accountId\":\"\",\"memberId\":\"\",\"memberIdHash\":\"\",\"c\":\"simul\",\"luid\":\"D733BF66-6B1E-422B-895F-524FF083BD4F\",\"platformVrUid\":\"768126a8cc5be8411a221c5bc7d28bc00f61d3b11867f9423b379ec8fc6e35f8\"},\"id\":\"b72f7e116957436b824f95d5138ea7a3\"}"))
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);

                //System.out.println(json.toString());
                String hlsSessionId = json.getAsJsonArray().get(0).getAsJsonObject().get("query").getAsString();

                //System.out.println("https://playback.api.streaks.jp/v1/projects/"+projectID+"/medias/ref:"+mediaID);
                request = HttpRequest.newBuilder()
                        .uri(new URI("https://playback.api.streaks.jp/v1/projects/"+projectID+"/medias/"+mediaID))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);

                //System.out.println(json);

                if (json.isJsonObject() && json.getAsJsonObject().has("sources")){
                    result.setURL(url);
                    //result.setTitle(json.getAsJsonObject().get("name").getAsString());
                    //result.setDescription(json.getAsJsonObject().get("description").getAsString());
                    //result.setDuration(json.getAsJsonObject().get("duration").getAsLong());
                    //result.setThumbnail(json.getAsJsonObject().get("thumbnail").getAsJsonObject().get("src").getAsString());

                    if (!hlsSessionId.isEmpty()){
                        result.setLiveURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString() + "&" + hlsSessionId);
                    } else {
                        result.setLiveURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());
                    }

                    //System.out.println(result.getLiveURL());
                    //return text;
                    //client.close();
                    return Function.gson.toJson(result);
                } else {
                    //client.close();
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。"));
                }
                //
                //json = Function.gson.fromJson(text, JsonElement.class);
                //result.setLiveURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());

                //client.close();
                //return Function.gson.toJson(result);
            }

            if (live3){
                TVerResult result = new TVerResult();

                String id = matcher4.group(1);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://statics.tver.jp/content/live/"+id+".json?v=3"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                String text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                JsonElement json = Function.gson.fromJson(text, JsonElement.class);

                result.setTitle(json.getAsJsonObject().get("title").getAsString());
                result.setDescription(json.getAsJsonObject().get("description").getAsString());
                request = HttpRequest.newBuilder()
                        .uri(new URI("https://playback.api.streaks.jp/v1/projects/tver-splive/medias/ref:"+id))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", id)
                        .GET()
                        .build();
                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                text = "{}";
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }
                json = Function.gson.fromJson(text, JsonElement.class);
                result.setLiveURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());

                //client.close();
                return Function.gson.toJson(result);
            }

            if (olympic){
                // https://tver.jp/olympic/milanocortina2026/live/play/eocqrh3x908m/

                String olympic_code = matcher5.group(1);
                String id = matcher5.group(2).split("/")[0];
                //System.out.println(id);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://olympic-data.tver.jp/api/live/"+id))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/olympic/milanocortina2026/live/play/"+id+"/")
                        .GET()
                        .build();

                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
                String text = null;
                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }

                //System.out.println(text);
                JsonElement json = Function.gson.fromJson(text, JsonElement.class);
                TVerResult tverResult = new TVerResult();
                tverResult.setTitle(json.getAsJsonObject().get("contents").getAsJsonObject().get("live").getAsJsonObject().get("title").getAsString());
                tverResult.setDescription(json.getAsJsonObject().get("contents").getAsJsonObject().get("live").getAsJsonObject().get("description").getAsString());
                tverResult.setURL("https://tver.jp/olympic/"+olympic_code+"/live/play/"+id);
                tverResult.setThumbnail(json.getAsJsonObject().get("contents").getAsJsonObject().get("live").getAsJsonObject().get("picture_l_url").getAsString());

                // https://playback.api.streaks.jp/v1/projects/tver-olympic-live/medias/ref:sp_260201_spc_01_dvr
                long time = new Date().getTime();
                Date dvr_start_date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(json.getAsJsonObject().get("contents").getAsJsonObject().get("live").getAsJsonObject().get("dvr_start_date").getAsString());
                Date dvr_end_date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(json.getAsJsonObject().get("contents").getAsJsonObject().get("live").getAsJsonObject().get("dvr_end_date").getAsString());

                String video_id = json.getAsJsonObject().get("contents").getAsJsonObject().get("live").getAsJsonObject().get("video_id").getAsString() + (dvr_start_date.getTime() <= time && time <= dvr_end_date.getTime() ? "_dvr" : "");
                request = HttpRequest.newBuilder()
                        .uri(new URI("https://playback.api.streaks.jp/v1/projects/tver-olympic-live/medias/ref:"+video_id))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Accept-Encoding", "gzip, br")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", "a35ebb1ca7d443758dc7fcc5d99b1f72")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";

                if (!contentEncoding.isEmpty()){
                    byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(send.body(), StandardCharsets.UTF_8);
                }

                //System.out.println(text);
                json = Function.gson.fromJson(text, JsonElement.class);

                JsonArray sources = json.getAsJsonObject().get("sources").getAsJsonArray();
                tverResult.setVideoURL(sources.get(0).getAsJsonObject().get("src").getAsString());

                //client.close();
                return Function.gson.toJson(tverResult);
            }

        } catch (Exception e){
            e.printStackTrace();
            //client.close();
            return Function.gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }

        return "{}";
    }

    @Override
    public String getServiceName() {
        return "TVer";
    }

}
