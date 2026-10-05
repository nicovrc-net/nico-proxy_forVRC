package net.nicovrc.dev.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.bilibiliResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class bilibili_com implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern Support_URL1 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)/");
    private final Pattern Support_URL2 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)");
    private final Pattern Support_URL3 = Pattern.compile("b23\\.tv");

    private final Pattern matcher_json1 = Pattern.compile("<script>window\\.__playinfo__=\\{(.+)\\};");
    private final Pattern matcher_json2 = Pattern.compile("<script>window\\.__INITIAL_STATE__=\\{(.+)\\};");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{
                "www.bilibili.com",
                "b23.tv"
        };
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
        if (url == null || url.isEmpty()){
            return Function.gson.toJson(new ErrorMessage("URLがありません"));
        }

        Matcher matcher1 = Support_URL1.matcher(url);
        Matcher matcher2 = Support_URL2.matcher(url);
        Matcher matcher3 = Support_URL3.matcher(url);

        String VideoID = "";
        if (matcher1.find()){
            VideoID = matcher1.group(1);
        } else if (matcher2.find()) {
            VideoID = matcher2.group(1);
        } else if (matcher3.find()) {
                try  {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI(url))
                            .headers("User-Agent", Function.UserAgent)
                            .GET()
                            .build();

                    HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                    url = send.uri().toURL().toString();

                    Matcher matcher = Support_URL1.matcher(url);
                    if (matcher.find()) {
                        VideoID = matcher.group(1);
                    }
                } catch (Exception e){
                    return Function.gson.toJson(new ErrorMessage("サポートされていないURLです。"));
                }
        } else {
            return Function.gson.toJson(new ErrorMessage("サポートされていないURLです。"));
        }

        try {
            URI uri = new URI("https://www.bilibili.com/video/"+VideoID+"/");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip")
                    .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                    .GET()
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() >= 400 && send.statusCode() != 412) {
                //client.close();
                request = null;
                uri = null;
                return Function.gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")"));
            }

            if (send.statusCode() == 412) {

            /*
            send.headers().map().forEach((name, value) -> {
                System.out.println("--- "+name+" ---");
                System.out.println(value);
            });
             */
/*
            String cookie = send.headers().firstValue("set-cookie").get();
            //System.out.println(cookie);
            String[] split = cookie.split(";")[0].split("=");
            //System.out.println("debug : "+split[0]+"="+split[1]);

            request = HttpRequest.newBuilder()
                    .uri(new URI("https://security.bilibili.com/412"))
                    .headers("User-Agent", Function.UserAgent)
                    .header("Accept", "* /*")
                    .header("Accept-Encoding", "gzip")
                    .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                    .build();
            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            //System.out.println(new String(send.body(), StandardCharsets.UTF_8));

            request = HttpRequest.newBuilder()
                    .uri(new URI("https://security.bilibili.com/th/captcha/get"))
                    .headers("User-Agent", Function.UserAgent)
                    .header("Accept", "* /*")
                    .header("Accept-Encoding", "gzip")
                    .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                    .build();
            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            //System.out.println(new String(send.body(), StandardCharsets.UTF_8));

            request = HttpRequest.newBuilder()
                    .uri(new URI("https://security.bilibili.com/th/captcha/cc/check"))
                    .headers("User-Agent", Function.UserAgent)
                    .header("Accept", "* /*")
                    .header("Accept-Encoding", "gzip")
                    .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("token=&result=1763618"))
                    .build();
            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            System.out.println(new String(send.body(), StandardCharsets.UTF_8));
*/


                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .header("Accept", "*/*")
                        .header("Accept-Encoding", "gzip")
                        .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                        //.header("Cookie", split[0]+"="+split[1])
                        .header("Cookie", "b_lsid=7A92BB74_1A10610DAA2; X-BILI-SEC-TOKEN=3,eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJxIjoiSHZkYXJkQ0R5QUVOeDh0T0ZJTVg5S0UxakN3VGw2NVoiLCJyIjoiN2Q2MjZkMDg2NDIyZjllYTg1MWM4Zjg1NmMwMjE5OGUzOTFiYmEyMjZiMGU0YzFjMGZiMTZkY2FiMWVjZWMwZSIsImlwIjoiMTYwLjI1MS4yMDYuMjUwIiwiZnAiOiI3YzZjZTg0NjE2MWZmNDM5ZWI4YmZlYzBiMDk0Yjc1MyIsInZlcml0eSI6MSwidHlwZSI6IjEiLCJleHAiOjE3OTExMDY3OTEsImlhdCI6MTc5MTEwMzE3MX0.pWWV2WiAx5-hFauXhKm68oIHbeD9WPCwlgBvwyfvgCI")
                        .GET()
                        .build();
                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (send.statusCode() >= 400) {
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")"));
                }
            }

            String html = new String(Function.decompressByte(send.body(), send.headers().firstValue("content-encoding").get()), StandardCharsets.UTF_8);
            Matcher matcher_html1 = matcher_json1.matcher(html);
            Matcher matcher_html2 = matcher_json2.matcher(html);

            boolean isHtmlData = false;
            boolean html1 = matcher_html1.find();
            boolean html2 = matcher_html2.find();

            String jsonText = "{}";

            if (html1) {
                isHtmlData = true;
                jsonText = "{"+matcher_html1.group(1)+"}";
            }

            if (!html1 && html2){
                jsonText = "{"+matcher_html2.group(1)+"}";
            }

            JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);
/*
        if (isHtmlData){

            System.out.println(json);

            return null;
        }
*/
            String avid = "";
            String bvid = "";
            String cid = "";

            try {
                avid = json.getAsJsonObject().get("aid").getAsString();
                bvid = json.getAsJsonObject().get("bvid").getAsString();
                cid = json.getAsJsonObject().get("cid").getAsString();
            } catch (Exception e){
                return Function.gson.toJson(new ErrorMessage("動画の取得に失敗しました。" + e.getMessage()));
            }

            bilibiliResult result = new bilibiliResult();

            result.setURL("https://www.bilibili.com/video/"+bvid+"/");
            result.setTitle(json.getAsJsonObject().get("videoData").getAsJsonObject().get("title").getAsString());
            result.setDescription(json.getAsJsonObject().get("videoData").getAsJsonObject().get("desc").getAsString());
            result.setThumbnail(json.getAsJsonObject().get("videoData").getAsJsonObject().get("pic").getAsString());
            result.setViewCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("view").getAsInt());
            result.setReplyCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("reply").getAsInt());
            result.setLikeCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("like").getAsInt());
            result.setCoinCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("coin").getAsInt());
            result.setFavoriteCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("favorite").getAsInt());
            result.setDuration(json.getAsJsonObject().get("videoData").getAsJsonObject().get("duration").getAsInt());

            //uri = new URI("https://api.bilibili.com/x/player/wbi/playurl?bvid="+bvid+"&cid="+cid+"&fnval=4048&try_look=1&wts=1791030916&w_rid=abad2de942aa1b07ac313dc644ea8d9d");
            uri = new URI("https://api.bilibili.com/x/player/wbi/playurl?avid="+avid+"&bvid="+bvid+"&cid="+cid+"&qn=0&fnver=0&fnval=4048&fourk=1&gaia_source=&from_client=BROWSER&is_main_page=true&need_fragment=false&isGaiaAvoided=false&client_attr=0&version_name=4.10.4&app_id=100&session=bea6a57fe31194bf5fce97ee4f0dc942&web_location=1315873&dm_img_list=[]&dm_img_str=V2ViR0wgMS&dm_cover_img_str=QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDk4MCBEaXJlY3QzRDExIHZzXzVfMCBwc181XzApLCBvciBzaW1pbGFyR29vZ2xlIEluYy4gKE5WSURJQS&dm_img_inter=%7B%22ds%22:[],%22wh%22:[5773,6976,105],%22of%22:[331,662,331]%7D&x-bili-device-req-json=%7B%22platform%22:%22web%22,%22device%22:%22pc%22,%22mobi_app%22:%22web_cn%22%7D&x-bili-locale-json=%7B%22c_locale%22:%7B%22language%22:%22zh%22,%22script%22:%22Hans%22%7D,%22always_translate%22:false%7D&w_rid=77dde55e1e434e02143c8084dba6ad41&wts=1791025255");
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "*/*")
                    .headers("Accept-Encoding", "gzip")
                    .headers("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                    .headers("Cookie", "b_lsid=940AF4BD_1A10611B318; buvid3=0B9DC0B2-9AEF-3837-C001-5E51FA5A61B591250infoc; b_nut=1791103191; _uuid=B59E4A2C-6E75-A5C4-655C-9EB86210ED7FA85477infoc; CURRENT_FNVAL=4048; CURRENT_QUALITY=0; buvid_fp=0f7691c97448244aa89b60c729662e74")
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (send.statusCode() >= 400){
                //client.close();
                request = null;
                uri = null;
                return Function.gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")"));
            }

            String s = "";
            if (send.headers().firstValue("content-encoding").isPresent()){
                s = send.headers().firstValue("content-encoding").get();
            }
            jsonText = new String(Function.decompressByte(send.body(), s), StandardCharsets.UTF_8);

            json = Function.gson.fromJson(jsonText, JsonElement.class);

            if (!json.getAsJsonObject().get("data").getAsJsonObject().has("dash")){
                Thread.sleep(2000L);
                uri = new URI("https://api.bilibili.com/x/player/wbi/playurl?avid="+avid+"&bvid="+bvid+"&cid="+cid+"&qn=0&fnver=0&fnval=4048&fourk=1&gaia_source=&from_client=BROWSER&is_main_page=true&need_fragment=false&isGaiaAvoided=false&client_attr=0&version_name=4.10.4&app_id=100&session=bea6a57fe31194bf5fce97ee4f0dc942&web_location=1315873&dm_img_list=[]&dm_img_str=V2ViR0wgMS&dm_cover_img_str=QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDk4MCBEaXJlY3QzRDExIHZzXzVfMCBwc181XzApLCBvciBzaW1pbGFyR29vZ2xlIEluYy4gKE5WSURJQS&dm_img_inter=%7B%22ds%22:[],%22wh%22:[5773,6976,105],%22of%22:[331,662,331]%7D&x-bili-device-req-json=%7B%22platform%22:%22web%22,%22device%22:%22pc%22,%22mobi_app%22:%22web_cn%22%7D&x-bili-locale-json=%7B%22c_locale%22:%7B%22language%22:%22zh%22,%22script%22:%22Hans%22%7D,%22always_translate%22:false%7D&w_rid=77dde55e1e434e02143c8084dba6ad41&wts=1791025255");
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "*/*")
                        .headers("Accept-Encoding", "gzip")
                        .headers("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                        .headers("Cookie", "b_lsid=940AF4BD_1A10611B318; buvid3=0B9DC0B2-9AEF-3837-C001-5E51FA5A61B591250infoc; b_nut=1791103191; _uuid=B59E4A2C-6E75-A5C4-655C-9EB86210ED7FA85477infoc; CURRENT_FNVAL=4048; CURRENT_QUALITY=0; buvid_fp=0f7691c97448244aa89b60c729662e74")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (send.statusCode() >= 400){
                    //client.close();
                    request = null;
                    uri = null;
                    return Function.gson.toJson(new ErrorMessage("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")"));
                }

                s = "";
                if (send.headers().firstValue("content-encoding").isPresent()){
                    s = send.headers().firstValue("content-encoding").get();
                }
                jsonText = new String(Function.decompressByte(send.body(), s), StandardCharsets.UTF_8);

                json = Function.gson.fromJson(jsonText, JsonElement.class);
            }

            JsonArray arrayVideo = json.getAsJsonObject().get("data").getAsJsonObject().get("dash").getAsJsonObject().get("video").getAsJsonArray();
            JsonArray arrayAudio = json.getAsJsonObject().get("data").getAsJsonObject().get("dash").getAsJsonObject().get("audio").getAsJsonArray();

            String videoUrl = null;
            long maxVideoBitrate = -1;
            String audioUrl = null;
            long maxAudioBitrate = -1;

            for (JsonElement jsonElement : arrayVideo) {

                String tempURL = jsonElement.getAsJsonObject().get("baseUrl").getAsString();
                uri = new URI(tempURL);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .header("Accept", "*/*")
                        .header("Accept-Encoding", "gzip")
                        .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                        .header("Referer", "https://www.bilibili.com/")
                        .header("Range", "bytes=0-2775")
                        .GET()
                        .build();
                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (send.statusCode() >= 400){
                    tempURL = jsonElement.getAsJsonObject().get("backupUrl").getAsJsonArray().get(0).getAsString();
                }

                if (maxVideoBitrate <= jsonElement.getAsJsonObject().get("bandwidth").getAsInt()) {
                    videoUrl = tempURL;
                    maxVideoBitrate = jsonElement.getAsJsonObject().get("bandwidth").getAsInt();
                }

            }

            for (JsonElement jsonElement : arrayAudio) {

                String tempURL = jsonElement.getAsJsonObject().get("baseUrl").getAsString();
                uri = new URI(tempURL);
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .header("Accept", "*/*")
                        .header("Accept-Encoding", "gzip")
                        .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                        .header("Referer", "https://www.bilibili.com/")
                        .header("Range", "bytes=0-2775")
                        .GET()
                        .build();
                send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (send.statusCode() >= 400){
                    tempURL = jsonElement.getAsJsonObject().get("backupUrl").getAsJsonArray().get(0).getAsString();
                }

                if (maxAudioBitrate <= jsonElement.getAsJsonObject().get("bandwidth").getAsInt()) {
                    audioUrl = tempURL;
                    maxAudioBitrate = jsonElement.getAsJsonObject().get("bandwidth").getAsInt();
                }

            }

            result.setVideoURL(videoUrl);
            result.setAudioURL(audioUrl);

            return Function.gson.toJson(result);
        } catch (Exception e){
            return Function.gson.toJson(new ErrorMessage("内部エラーです。" + e.getMessage()));
        }

    }

    @Override
    public String getServiceName() {
        return "bilibili.com";
    }

}
