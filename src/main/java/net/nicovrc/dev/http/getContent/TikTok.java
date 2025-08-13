package net.nicovrc.dev.http.getContent;

import com.google.gson.Gson;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.TikTokResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TikTok implements GetContent {

    private final Gson gson = Function.gson;

    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {
        TikTokResult result = gson.fromJson(json, TikTokResult.class);

        ContentObject object = new ContentObject();
        object.setCookieText(result.getVideoAccessCookie());
        object.setHLS(false);
        object.setRefererText("https://www.tiktok.com/");
        return object;
    }
}
