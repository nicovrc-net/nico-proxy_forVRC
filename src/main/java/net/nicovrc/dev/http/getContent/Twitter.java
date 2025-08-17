package net.nicovrc.dev.http.getContent;

import java.net.http.HttpClient;

public class Twitter implements GetContent{
    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        ContentObject object = new ContentObject();
        object.setHLS(false);
        return object;
    }
}
