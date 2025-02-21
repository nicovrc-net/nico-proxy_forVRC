package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.NicoNicoVideo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicoVideo implements ServiceAPI {
    private final Gson gson = Function.gson;
    private final String[] SupportURL = {
            "www.nicovideo.jp",
            "live.nicovideo.jp",
            "nico.ms",
            "cas.nicovideo.jp",
            "sm",
            "nm",
            "am",
            "fz",
            "ut",
            "dm",
            "so",
            "ax",
            "ca",
            "cd",
            "cw",
            "fx",
            "ig",
            "na",
            "om",
            "sd",
            "sk",
            "yk",
            "yo",
            "za",
            "zb",
            "zc",
            "zd",
            "ze",
            "nl",
            "ch"
    };
    private final Pattern NicoID1 = Pattern.compile("(http|https)://(live|www)\\.nicovideo\\.jp/watch/(.+)");
    private final Pattern NicoID2 = Pattern.compile("(http|https)://nico\\.ms/(.+)");
    private final Pattern NicoID3 = Pattern.compile("(http|https)://cas\\.nicovideo\\.jp/user/(.+)");
    private final Pattern NicoID4 = Pattern.compile("^(sm\\d+|nm\\d+|am\\d+|fz\\d+|ut\\d+|dm\\d+|so\\d+|ax\\d+|ca\\d+|cd\\d+|cw\\d+|fx\\d+|ig\\d+|na\\d+|om\\d+|sd\\d+|sk\\d+|yk\\d+|yo\\d+|za\\d+|zb\\d+|zc\\d+|zd\\d+|ze\\d+|nl\\d+|ch\\d+|\\d+)");
    private String URL = null;

    private final Pattern matcher_Json = Pattern.compile("<meta name=\"server-response\" content=\"\\{(.+)}\" />");

    private final Pattern matcher_videoError1 = Pattern.compile("この動画は存在しないか、削除された可能性があります。");
    private final Pattern matcher_videoError2 = Pattern.compile("この動画は(.+)の申立により、著作権侵害として削除されました。");

    @Override
    public String[] getCorrespondingURL() {
        return SupportURL;
    }

    @Override
    public void Set(String json) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.URL = jsonElement.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {
        if (URL == null || URL.isEmpty()){
            return "{\"ErrorMessage\": \"URLがありません\"}";
        }

        String url = URL.split("\\?")[0];
        Matcher matcher_normal = NicoID1.matcher(url);
        Matcher matcher_short = NicoID2.matcher(url);
        Matcher matcher_cas = NicoID3.matcher(url);
        Matcher matcher_idOnly = NicoID4.matcher(url);

        boolean isNormal = matcher_normal.find();
        boolean isShort = matcher_short.find();
        boolean isCas = matcher_cas.find();
        boolean isID = matcher_idOnly.find();

        String id = "";

        if (!isNormal && !isShort && !isCas && !isID){
            url = null;
            matcher_normal = null;
            matcher_short = null;
            matcher_cas = null;
            matcher_idOnly = null;
            return "{\"ErrorMessage\": \"URLが間違っているか対応してないURLです。\"}";
        }

        String accessUrl = null;
        if (isID){
            id = matcher_idOnly.group(1);
            accessUrl = "https://nico.ms/" + id;
        }
        if (isNormal){
            id = matcher_normal.group(3);
            accessUrl = "https://nico.ms/" + id;
        }
        if (isShort || isCas){
            accessUrl = url;
        }
        //System.out.println(accessUrl);

        NicoNicoVideo result = new NicoNicoVideo();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            URI uri = new URI(accessUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (send.statusCode() >= 400){
                uri = null;
                request = null;
                client.close();
                client = null;
                return "{\"ErrorMessage\": \"取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")\"}";
            }

            String body = send.body();
            //System.out.println(body);
            Matcher matcher = matcher_Json.matcher(body);
            JsonElement json = null;
            if (matcher.find()){
                json = gson.fromJson("{" + matcher.group(1).replaceAll("&quot;", "\"") + "}", JsonElement.class);
            }
            matcher = null;
            body = null;

            //System.out.println(json);
            //return json.toString();

            uri = null;
            request = null;
            client.close();
            client = null;

            if (json != null){
                if (json.isJsonObject() && json.getAsJsonObject().has("data")){
                    String nicosid = json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("client").getAsJsonObject().get("nicosid").getAsString();
                    // 動画
                    result.setURL(json.getAsJsonObject().get("data").getAsJsonObject().get("metadata").getAsJsonObject().get("jsonLds").getAsJsonArray().get(0).getAsJsonObject().get("@id").getAsString());
                    result.setTitle(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("title").getAsString());
                    result.setDescription(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("description").getAsString());

                    JsonArray array = json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("tag").getAsJsonObject().get("items").getAsJsonArray();
                    String[] tags = new String[array.size()];
                    int i = 0;
                    for (JsonElement element : array) {
                        tags[i] = element.getAsJsonObject().get("name").getAsString();
                        i++;
                    }
                    result.setTags(tags);

                    result.setViewCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("view").getAsLong());
                    result.setCommentCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("comment").getAsLong());
                    result.setMyListCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("mylist").getAsLong());
                    result.setLikeCount(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("count").getAsJsonObject().get("like").getAsLong());

                    result.setDuration(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("duration").getAsLong());

                    result.setThumbnail(json.getAsJsonObject().get("data").getAsJsonObject().get("response").getAsJsonObject().get("video").getAsJsonObject().get("thumbnail").getAsJsonObject().get("player").getAsString());

                    String accessRightKey = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").get("accessRightKey").getAsString();
                    String trackId = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("client").get("watchTrackId").getAsString();

                    StringBuilder videoJson = new StringBuilder();

                    for (JsonElement element : json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("videos")) {
                        //System.out.println(element);
                        if (element.getAsJsonObject().get("isAvailable").getAsBoolean()) {
                            videoJson.append("[\"").append(element.getAsJsonObject().get("id").getAsString()).append("\",\"").append(json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("audios").get(0).getAsJsonObject().get("id").getAsString()).append("\"],");
                        }
                    }

                    String sendJson = "{\"outputs\":["+videoJson.substring(0, videoJson.length() - 1)+"]}";

                    client = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .connectTimeout(Duration.ofSeconds(5))
                            .build();

                    uri = new URI("https://nvapi.nicovideo.jp/v1/watch/"+id+"/access-rights/hls?actionTrackId="+trackId);
                    request = HttpRequest.newBuilder()
                            .uri(uri)
                            .headers("Access-Control-Request-Headers", "content-type,x-access-right-key,x-frontend-id,x-frontend-version,x-niconico-language,x-request-with")
                            .headers("X-Access-Right-Key", accessRightKey)
                            .headers("X-Frontend-Id", "6")
                            .headers("X-Frontend-Version", "0")
                            .headers("X-Niconico-Language", "ja-jp")
                            .headers("X-Request-With", "nicovideo")
                            .headers("Cookie", "nicosid="+nicosid)
                            .headers("User-Agent", Function.UserAgent)
                            .POST(HttpRequest.BodyPublishers.ofString(sendJson))
                            .build();

                    send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (send.statusCode() >= 400){
                        uri = null;
                        request = null;
                        client.close();
                        client = null;

                        matcher = matcher_videoError1.matcher(send.body());
                        if (matcher.find()){
                            matcher = null;
                            return "{\"ErrorMessage\": \"この動画は存在しないか、削除された可能性があります。\"}";
                        }
                        matcher = matcher_videoError2.matcher(send.body());
                        if (matcher.find()){
                            String str = matcher.group(1);
                            matcher = null;
                            return "{\"ErrorMessage\": \"この動画は"+str+"の申立により、著作権侵害として削除されました。\"}";
                        }

                        return "{\"ErrorMessage\": \"動画取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")\"}";
                    }
                    body = send.body();
                    //System.out.println(body);
                    json = gson.fromJson(body, JsonElement.class);

                    List<String> cookieText = new ArrayList<>();
                    if (!send.headers().allValues("Set-Cookie").isEmpty()){
                        cookieText = send.headers().allValues("Set-Cookie");
                    }
                    if (!send.headers().allValues("set-cookie").isEmpty()){
                        cookieText = send.headers().allValues("set-cookie");
                    }

                    if (json.isJsonObject() && json.getAsJsonObject().has("data") && json.getAsJsonObject().get("data").getAsJsonObject().has("contentUrl")){
                        result.setVideoURL(json.getAsJsonObject().get("data").getAsJsonObject().get("contentUrl").getAsString());

                        String[] split = cookieText.get(0).split(";");

                        HashMap<String, String> cookie = new HashMap<>();
                        cookie.put("nicosid", nicosid);
                        if (split[0].startsWith("domand_bid=")){
                            cookie.put("domand_bid", split[0].replaceAll("domand_bid=", ""));
                        }
                        result.setVideoAccessCookie(cookie);
                    } else {
                        return "{\"ErrorMessage\": \"動画取得に失敗しました。\"}";
                    }

                    return gson.toJson(result);

                } else {
                    // ニコ生

                }
            }

            result = null;
            matcher_normal = null;
            matcher_short = null;
            matcher_cas = null;
            matcher_idOnly = null;
            return "";
        } catch (Exception e){
            e.printStackTrace();
            return "{\"ErrorMessage\": \"取得に失敗しました。(HTTPアクセスエラー)\"}";
        }
    }

    @Override
    public String getServiceName() {
        return "ニコニコ";
    }
}
