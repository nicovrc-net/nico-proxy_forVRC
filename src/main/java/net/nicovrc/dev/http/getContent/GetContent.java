package net.nicovrc.dev.http.getContent;

import java.net.http.HttpClient;

public interface GetContent {

    ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception;

}
