package net.nicovrc.dev.http.getContent;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.bilibiliResult;

import java.net.http.HttpClient;

public class bilibili_com implements GetContent{
    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        bilibiliResult result = Function.gson.fromJson(json, bilibiliResult.class);

        ContentObject object = new ContentObject();
        object.setHLS(false);
        object.setRefererText(result.getURL());
        return object;
    }
}