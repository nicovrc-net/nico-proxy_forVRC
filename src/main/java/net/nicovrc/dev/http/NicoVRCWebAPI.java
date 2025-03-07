package net.nicovrc.dev.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.api.*;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NicoVRCWebAPI implements Runnable, NicoVRCHTTP {

    private String HTTPRequest = null;
    private String URL = null;
    private final List<NicoVRCAPI> list = new ArrayList<>();
    private Socket sock = null;
    private final Gson gson = Function.gson;

    public NicoVRCWebAPI(){
        // WebAPIを追加する
        list.add(new GetVideoInfo());
        list.add(new Test());
        list.add(new GetVersion());
        list.add(new GetSupportList());
        list.add(new GetCacheList());
    }

    @Override
    public void run() {
        try {
            Date date = new Date();
            System.out.println("[API Access ("+Function.sdf.format(date)+")] " + URL);
            if (list.isEmpty()){
                // 何もAPI実装されてなければ意味ないので
                return;
            }

            final String[] result = {null};
            list.forEach((api)->{

                if (URL.startsWith(api.getURI())){
                    try {
                        result[0] = api.Run(HTTPRequest);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            //System.out.println(result[0]);

            String method = Function.getMethod(HTTPRequest);
            String httpVersion = Function.getHTTPVersion(HTTPRequest);
            if (result[0] == null){
                Function.sendHTTPRequest(sock, httpVersion, 404, "text/plain; charset=utf-8","API Not Found".getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
                method = null;
                httpVersion = null;
                return;
            }

            JsonElement json = null;
            try {
                json = gson.fromJson(result[0], JsonElement.class);
            } catch (Exception e){
                json = null;
            }

            int code = 200;
            if (json != null){
                if (json.isJsonObject() && json.getAsJsonObject().has("ErrorMessage")){
                    code = 404;
                } else {
                    code = 200;
                }
            }

            Function.sendHTTPRequest(sock, httpVersion, code, "application/json; charset=utf-8", "*",result[0].getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
            method = null;
            httpVersion = null;
            json = null;

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getStartURI() {
        return "/api/";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {
        this.HTTPRequest = httpRequest;
        this.URL = Function.getURI(httpRequest);
    }

    @Override
    @Deprecated
    public void setURL(String URL) {
        this.URL = URL;
    }

    @Override
    public void setHTTPSocket(Socket sock) {
        this.sock = sock;
    }
}
