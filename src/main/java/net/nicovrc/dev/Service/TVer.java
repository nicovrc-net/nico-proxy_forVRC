package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.TVerResult;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVer implements ServiceAPI {

    private String url = null;
    private String proxy = null;

    private final Pattern Support_URLVideo1 = Pattern.compile("https://tver\\.jp/episodes/(.+)");
    private final Pattern Support_URLLive1 = Pattern.compile("https://tver\\.jp/live/(.+)");
    private final Pattern Support_URLLive2 = Pattern.compile("https://tver\\.jp/live/simul/(.+)");
    private final Pattern Support_URLLive3 = Pattern.compile("https://tver.jp/live/special/(.+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"tver.jp"};
    }

    @Override
    public void Set(String json) {
        JsonElement element = Function.gson.fromJson(json, JsonElement.class);
        if (element.isJsonObject() && element.getAsJsonObject().has("URL")){
            url = element.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {
        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            proxy = Function.ProxyList.get(i);
        }

        HttpClient client;
        if (proxy == null){
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        } else {
            String[] s = proxy.split(":");
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .proxy(ProxySelector.of(new InetSocketAddress(s[0], Integer.parseInt(s[1]))))
                    .build();
        }

        Matcher matcher1 = Support_URLVideo1.matcher(url);
        Matcher matcher2 = Support_URLLive1.matcher(url);
        Matcher matcher3 = Support_URLLive2.matcher(url);
        Matcher matcher4 = Support_URLLive3.matcher(url);

        final boolean video1 = matcher1.find();
        final boolean live1 = matcher2.find();
        final boolean live2 = matcher3.find();
        final boolean live3 = matcher4.find();

        if (!video1 && !live1 && !live2 && !live3){
            return "{\"ErrorMessage\": \"対応してないURLです。\"}";
        }

        try {
            URI uri = new URI(url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Referer", "https://tver.jp/")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            //System.out.println(send.request().uri());
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            if (video1){
                URI uri = new URI("https://statics.tver.jp/content/episode/"+matcher1.group(1)+".json?v=20");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (!send.body().startsWith("{") || !send.body().endsWith("}")){
                    return "{\"ErrorMessage\": \"取得に失敗しました。\"}";
                }

                JsonElement json = Function.gson.fromJson(send.body(), JsonElement.class);
                String projectID = json.getAsJsonObject().get("streaks").getAsJsonObject().get("projectID").getAsString();
                String videoRefID = json.getAsJsonObject().get("streaks").getAsJsonObject().get("videoRefID").getAsString();

                //return send.body();

                request = HttpRequest.newBuilder()
                        .uri(new URI("https://player.tver.jp/player/ad_template.json"))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);

                String ati = json.getAsJsonObject().get(projectID).getAsJsonObject().get("pc").getAsString();

                request = HttpRequest.newBuilder()
                        .uri(new URI("https://playback.api.streaks.jp/v1/projects/"+projectID+"/medias/ref:"+videoRefID+"?ati="+ati))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);

                if (json.isJsonObject() && json.getAsJsonObject().has("sources")){
                    TVerResult result = new TVerResult();
                    result.setURL(url);
                    result.setTitle(json.getAsJsonObject().get("name").getAsString());
                    result.setDescription(json.getAsJsonObject().get("description").getAsString());
                    result.setDuration(json.getAsJsonObject().get("duration").getAsLong());
                    result.setThumbnail(json.getAsJsonObject().get("thumbnail").getAsJsonObject().get("src").getAsString());

                    result.setVideoURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());
                    //return send.body();
                    return Function.gson.toJson(result);
                } else {
                    return "{\"ErrorMessage\": \"取得に失敗しました。\"}";
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
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("x-tver-platform-type", "web")
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                //System.out.println(send.body());
                JsonElement json = Function.gson.fromJson(send.body(), JsonElement.class);
                if (!json.getAsJsonObject().has("result")){
                    return "{\"ErrorMessage\": \"取得に失敗しました。\"}";
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
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", id)
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);

                result.setDescription(json.getAsJsonObject().get("description").getAsString());

                uri = new URI("https://playback.api.streaks.jp/v1/projects/tver-simul-"+id+"/medias/ref:simul-"+id);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", id)
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (!send.body().startsWith("{") || !send.body().endsWith("}")){
                    return "{\"ErrorMessage\": \"取得に失敗しました。\"}";
                }

                json = Function.gson.fromJson(send.body(), JsonElement.class);
                String hlsURL = json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString();
                String session = json.getAsJsonObject().getAsJsonObject().get("id").getAsString();
                //


                uri = new URI("https://ssai.api.streaks.jp/v1/projects/tver-simul-"+id+"/medias/"+session+"/ssai/session");
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"ads_params\":{\"tvcu_pcode\":\"\",\"tvcu_ccode\":\"\",\"tvcu_zcode\":\"\",\"tvcu_gender\":\"\",\"tvcu_gender_code\":\"\",\"tvcu_age\":\"\",\"tvcu_agegrp\":\"\",\"delivery_type\":\"simul\",\"is_dvr\":0,\"rdid\":\"\",\"idtype\":\"\",\"is_lat\":\"\",\"bundle\":\"\",\"iuid\":\"pbt4bylcc1g799128136\",\"interest\":\"\",\"video_id\":\"lenk0yvgh8\",\"device\":\"pc\",\"device_code\":\"0001\",\"tag_type\":\"browser\",\"item_eventid\":\"62357\",\"item_programkey\":\"00005\",\"item_category\":\"99\",\"item_episodecode\":\"cfca1a05-c507-42fb-8d62-50596518ac0c\",\"item_originalmeta1\":\"\",\"item_originalmeta2\":\"\",\"ntv_ppid\":\"z75i3v2w34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"tbs_ppid\":\"f87wu4in34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"tx_ppid\":\"t87wrus634cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"ex_ppid\":\"n6dsf79v34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"cx_ppid_gam\":\"b8a35iwj34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"mbs_ppid_gam\":\"x32ck84s34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"abc_ppid\":\"c2fq84em34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"tvo_ppid\":\"i3wtqjey34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"ktv_ppid\":\"g9byn7re34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"ytv_ppid\":\"g8kusm7634cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"ntv_ppid2\":\"z75i3v2w_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"tbs_ppid2\":\"f87wu4in_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"tx_ppid2\":\"t87wrus6_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"ex_ppid2\":\"n6dsf79v_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"cx_ppid2\":\"b8a35iwj_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"mbs_ppid2\":\"x32ck84s_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"abc_ppid2\":\"c2fq84em_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"tvo_ppid2\":\"i3wtqjey_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"ktv_ppid2\":\"g9byn7re_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"ytv_ppid2\":\"g8kusm76_2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"vr_uuid\":\"23768E3B-100F-4820-93DC-0AE4948DCE66\",\"personalIsLat\":\"0\",\"platformAdUid\":\"2b210aa5-ea11-4c56-9d7a-b81c29924bd8\",\"platformUid\":\"34cbcb764b3c4ed6b7a4c037e74ebc93a724\",\"memberId\":\"\",\"c\":\"simul\",\"luid\":\"23768E3B-100F-4820-93DC-0AE4948DCE66\",\"platformVrUid\":\"c0af47d1f9082f50dbb8d5beceb35af4c04915da46b551f884551a1648a77121\"},\"id\":\"ed089bc4b7f342b3bc8a7a2567039442\"}"))
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);

                result.setLiveURL(hlsURL + "&"+json.getAsJsonArray().get(0).getAsJsonObject().get("query").getAsString());

                return Function.gson.toJson(result);
            }

            if (live2){
                TVerResult result = new TVerResult();

                String id = matcher3.group(1);
                int version = 0;

                URI uri = new URI("https://cf-platform-api.tver.jp/service/api/v1/callLiveEpisode/"+id+"?platform_uid=2be39f5922194c42807d683a0303e1106ecb&platform_token=mc1yxsex0zc8h6lh9ezpu7vd6k9kww6vqysyo79g&require_data=mylist");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("x-tver-platform-type", "web")
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonElement json = Function.gson.fromJson(send.body(), JsonElement.class);
                version = json.getAsJsonObject().get("result").getAsJsonObject().get("episode").getAsJsonObject().get("content").getAsJsonObject().get("version").getAsInt();

                uri = new URI("https://statics.tver.jp/content/live/"+id+".json?v="+version);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Origin", "https://tver.jp")
                        .headers("Referer", "https://tver.jp/")
                        .headers("X-Streaks-Api-Key", id)
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                json = Function.gson.fromJson(send.body(), JsonElement.class);
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
                }

                if (!json.getAsJsonObject().get("dvr").getAsJsonObject().get("allow").getAsBoolean()){

                    uri = new URI("https://playback.api.streaks.jp/v1/projects/"+projectID+"/medias/"+mediaID);
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Origin", "https://tver.jp")
                            .headers("Referer", "https://tver.jp/")
                            .headers("X-Streaks-Api-Key", apiKey)
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                } else {

                    uri = new URI("https://service-api.tver.jp/api/v1/callEpisodeStatusCheck?episode_id="+id+"&type=live");
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Origin", "https://tver.jp")
                            .headers("Referer", "https://tver.jp/")
                            .headers("x-tver-platform-type", "web")
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    json = Function.gson.fromJson(send.body(), JsonElement.class);
                    String videoId = json.getAsJsonObject().get("result").getAsJsonObject().get("content").getAsJsonObject().get("id").getAsString();

                    uri = new URI("https://statics.tver.jp/content/episode/"+videoId+".json?v=20");
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Referer", "https://tver.jp/")
                            .GET()
                            .build();
                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    json = Function.gson.fromJson(send.body(), JsonElement.class);

                    projectID = json.getAsJsonObject().get("streaks").getAsJsonObject().get("projectID").getAsString();
                    String videoRefID = json.getAsJsonObject().get("streaks").getAsJsonObject().get("videoRefID").getAsString();

                    request = HttpRequest.newBuilder()
                            .uri(new URI("https://player.tver.jp/player/ad_template.json"))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Referer", "https://tver.jp/")
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    json = Function.gson.fromJson(send.body(), JsonElement.class);

                    String ati = json.getAsJsonObject().get(projectID).getAsJsonObject().get("pc").getAsString();

                    request = HttpRequest.newBuilder()
                            .uri(new URI("https://playback.api.streaks.jp/v1/projects/"+projectID+"/medias/ref:"+videoRefID+"?ati="+ati))
                            .headers("User-Agent", Function.UserAgent)
                            .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                            .headers("Origin", "https://tver.jp")
                            .headers("Referer", "https://tver.jp/")
                            .GET()
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    json = Function.gson.fromJson(send.body(), JsonElement.class);

                    if (json.isJsonObject() && json.getAsJsonObject().has("sources")){
                        result = new TVerResult();
                        result.setURL(url);
                        result.setTitle(json.getAsJsonObject().get("name").getAsString());
                        result.setDescription(json.getAsJsonObject().get("description").getAsString());
                        result.setDuration(json.getAsJsonObject().get("duration").getAsLong());
                        result.setThumbnail(json.getAsJsonObject().get("thumbnail").getAsJsonObject().get("src").getAsString());

                        result.setVideoURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());
                        //return send.body();
                        return Function.gson.toJson(result);
                    } else {
                        return "{\"ErrorMessage\": \"取得に失敗しました。\"}";
                    }
                }
                //


                json = Function.gson.fromJson(send.body(), JsonElement.class);
                result.setLiveURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());

                return Function.gson.toJson(result);
            }
            TVerResult result = new TVerResult();

            String id = matcher4.group(1);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://statics.tver.jp/content/live/"+id+".json?v=3"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Origin", "https://tver.jp")
                    .headers("Referer", "https://tver.jp/")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonElement json = Function.gson.fromJson(send.body(), JsonElement.class);

            result.setTitle(json.getAsJsonObject().get("title").getAsString());
            result.setDescription(json.getAsJsonObject().get("description").getAsString());
            request = HttpRequest.newBuilder()
                    .uri(new URI("https://playback.api.streaks.jp/v1/projects/tver-splive/medias/ref:"+id))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Origin", "https://tver.jp")
                    .headers("Referer", "https://tver.jp/")
                    .headers("X-Streaks-Api-Key", id)
                    .GET()
                    .build();
            send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            json = Function.gson.fromJson(send.body(), JsonElement.class);
            result.setLiveURL(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString());

            return Function.gson.toJson(result);
        } catch (Exception e){
            e.printStackTrace();
            return "{\"ErrorMessage\": \"内部エラーです。 ("+e.getMessage().replaceAll("\"","\\\\\"")+"\"}";
        }

    }

    @Override
    public String getServiceName() {
        return "TVer";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
