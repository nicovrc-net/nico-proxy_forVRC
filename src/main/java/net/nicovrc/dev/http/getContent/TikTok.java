package net.nicovrc.dev.http.getContent;

import com.google.Function.gson.Function.gson;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.TikTokResult;

import java.net.http.HttpClient;

public class TikTok implements GetContent {

    private final Function.gson Function.gson = Function.gson;

    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {
        TikTokResult result = Function.gson.fromJson(json, TikTokResult.class);

        ContentObject object = new ContentObject();
        object.setCookieText(result.getVideoAccessCookie());
        object.setHLS(false);
        object.setRefererText("https://www.tiktok.com/");
        return object;
    }
}
