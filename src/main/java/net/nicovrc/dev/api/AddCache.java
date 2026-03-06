package net.nicovrc.dev.api;


import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.data.CacheData;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddCache implements NicoVRCAPI {

    private final Pattern matcher_postData = Pattern.compile("\\{(.+)\\}");

    @Override
    public String getURI() {
        return "/api/v1/add_cache";
    }

    @Override
    public String Run(String httpRequest, HttpClient client) {

        try {
            Matcher matcher = matcher_postData.matcher(httpRequest);

            String group = matcher.group(1);
            if (group.equals("\"scheme\":\"https\"")){
                if (matcher.find()){
                    group = matcher.group(1);
                }
            }

            JsonElement json = Function.gson.fromJson("{"+group+"}", JsonElement.class);
            if (json.isJsonObject() && json.getAsJsonObject().has("addUrl")){

                String addUrl = json.getAsJsonObject().get("addUrl").getAsString();
                String cacheUrl = json.getAsJsonObject().get("cacheUrl").getAsString();
                boolean isRedirect = json.getAsJsonObject().get("isRedirect").getAsBoolean();
                String title = json.getAsJsonObject().has("title") ? json.getAsJsonObject().get("title").getAsString() : "";
                String hlsText = json.getAsJsonObject().has("hlsText") ? json.getAsJsonObject().get("hlsText").getAsString() : "";
                String dummyHlsText = json.getAsJsonObject().has("hlsDummyText") ? json.getAsJsonObject().get("hlsDummyText").getAsString() : "";
                String proxy = json.getAsJsonObject().has("proxy") ? json.getAsJsonObject().get("proxy").getAsString() : "";
                String cookieText = json.getAsJsonObject().has("cookie") ? json.getAsJsonObject().get("cookie").getAsString() : "";
                String refererText = json.getAsJsonObject().has("referer") ? json.getAsJsonObject().get("referer").getAsString() : "";

                CacheData cacheData = new CacheData();
                cacheData.setCacheDate(new Date().getTime());
                cacheData.setSet(true);
                cacheData.setTargetURL(cacheUrl);
                cacheData.setRedirect(isRedirect);
                cacheData.setProxy(proxy);
                cacheData.setTitle(title);
                cacheData.setHLS(hlsText.getBytes(StandardCharsets.UTF_8));
                cacheData.setDummyHLS(dummyHlsText.getBytes(StandardCharsets.UTF_8));
                cacheData.setCookieText(cookieText);
                cacheData.setRefererText(refererText);
                Function.addCache(addUrl, cacheData);

                group = null;
                json = null;
                matcher = null;
                return "{\"Message\": \"OK\"}";

            } else {
                group = null;
                json = null;
                matcher = null;
                return "{\"Message\": \"NG\"}";
            }

        } catch (Exception e){
            return "{\"Message\": \"NG\"}";
        }

    }
}
