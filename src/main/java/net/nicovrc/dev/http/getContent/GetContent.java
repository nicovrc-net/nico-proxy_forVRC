package net.nicovrc.dev.http.getContent;

import java.net.Socket;
import java.net.http.HttpClient;

public interface GetContent {

    ContentObject run(Socket sock, HttpClient client, String httpRequest, String URL, String json);

}
