package net.nicovrc.dev.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.api.*;
import net.nicovrc.dev.data.WebhookData;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NicoVRCWebAPI implements Runnable, NicoVRCHTTP {

    private String HTTPRequest = null;
    private String URL = null;
    private final HashMap<String, NicoVRCAPI> apiList = new HashMap<>();
    private Socket sock = null;
    private final Gson gson = Function.gson;

    private final String contentType_text = "text/plain; charset=utf-8";

    private final byte[] errorAPINotFound = "API Not Found".getBytes(StandardCharsets.UTF_8);

    public NicoVRCWebAPI(){
        // WebAPIを追加する
        GetVideoInfo getVideoInfo = new GetVideoInfo();
        Test test = new Test();
        GetVersion getVersion = new GetVersion();
        GetSupportList getSupportList = new GetSupportList();
        GetCacheList getCacheList = new GetCacheList();
        AddCache addCache = new AddCache();

        apiList.put(getVideoInfo.getURI().substring(0, Math.min(getVideoInfo.getURI().length(), 10)), getVideoInfo);
        apiList.put(test.getURI().substring(0, Math.min(test.getURI().length(), 10)), test);
        apiList.put(getVersion.getURI().substring(0, Math.min(getVersion.getURI().length(), 10)), getVersion);
        apiList.put(getSupportList.getURI().substring(0, Math.min(getSupportList.getURI().length(), 10)), getSupportList);
        apiList.put(getCacheList.getURI().substring(0, Math.min(getCacheList.getURI().length(), 10)), getCacheList);
        apiList.put(addCache.getURI().substring(0, Math.min(addCache.getURI().length(), 10)), addCache);
    }

    @Override
    public void run() {
        try {
            Date date = new Date();
            final String method = Function.getMethod(HTTPRequest);
            final String httpVersion = Function.getHTTPVersion(HTTPRequest);
            final boolean isHead = method != null && method.equals("HEAD");

            String b_contentEncoding = Function.getContentEncoding(HTTPRequest);
            String sendContentEncoding = "";
            if (b_contentEncoding != null && b_contentEncoding.matches(".*br.*")){
                sendContentEncoding = "br";
            } else if (b_contentEncoding != null && b_contentEncoding.matches(".*gzip.*")){
                sendContentEncoding = "gzip";
            }

            System.out.println("[API Access ("+Function.sdf.format(date)+")] " + URL);

            String[] split = UUID.randomUUID().toString().split("-");
            Function.APIAccessLog.put(new Date().getTime() + "-"+ split[0]+split[1], HTTPRequest);

            WebhookData webhookData = new WebhookData();
            webhookData.setURL(URL);
            webhookData.setHTTPRequest(HTTPRequest);

            if (apiList.isEmpty()){
                // 何もAPI実装されてなければ意味ないので
                byte[] bytes = Function.compressByte(errorAPINotFound, sendContentEncoding);
                Function.sendHTTPRequest(sock, httpVersion, 404, contentType_text, sendContentEncoding, bytes == null ? errorAPINotFound : bytes, isHead);
                return;
            }

            String result = null;

            NicoVRCAPI vrcapi = apiList.get(URL.substring(0, Math.min(URL.length(), 10)));
            if (vrcapi != null){
                try {
                    result = vrcapi.Run(HTTPRequest);
                    webhookData.setAPIURI(vrcapi.getURI());
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            }

            //System.out.println(result);
            webhookData.setDate(date);
            Function.WebhookData.put(split[0]+split[1], webhookData);

            if (result == null){
                byte[] bytes = Function.compressByte(errorAPINotFound, sendContentEncoding);
                Function.sendHTTPRequest(sock, httpVersion, 404, contentType_text, sendContentEncoding, bytes == null ? errorAPINotFound : bytes, isHead);
                return;
            }

            JsonElement json;
            try {
                json = gson.fromJson(result, JsonElement.class);
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

            byte[] bytes = Function.compressByte(result.getBytes(StandardCharsets.UTF_8), sendContentEncoding);
            Function.sendHTTPRequest(sock, httpVersion, code, "application/json; charset=utf-8", sendContentEncoding, "*", bytes == null ? result.getBytes(StandardCharsets.UTF_8) : bytes, isHead);
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
