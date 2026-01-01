package net.nicovrc.dev.http;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.api.WebAPI;
import net.nicovrc.dev.api.v1.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class GetAPI implements GetInterface {

    private final String URI;
    private final String httpRequest;

    public GetAPI(String httpRequest, String uri){
        this.httpRequest = httpRequest;
        this.URI = uri;
    }

    @Override
    public byte[] run() {
        WebAPI api = null;
        if (URI.startsWith("/api/v1/test")){
            api = new Test();
        }

        if (api != null) {
            final String result = api.run();
            final JsonElement json = Function.gson.fromJson(result, JsonElement.class);

            final StringBuilder sb = new StringBuilder();


            if (json.getAsJsonObject().has("ErrorMessage")){
                sb.append("HTTP/").append(Function.getHTTPVersion(httpRequest)).append(" ").append("400 Bad Request\r\n");
            } else {
                sb.append("HTTP/").append(Function.getHTTPVersion(httpRequest)).append(" ").append("200 OK\r\n");
            }
            sb.append("Access-Control-Allow-Origin: *\r\n");
            sb.append("Content-Length: ").append(result.getBytes(StandardCharsets.UTF_8).length).append("\r\n");
            sb.append("Content-Type: application/json; charset=utf-8\r\n");
            sb.append("Date: ").append(new Date()).append("\r\n\r\n");

            if (!Function.getMethod(httpRequest).equals("HEAD")){
                sb.append(result);
            }

            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        return Function.zeroByte;
    }
}
