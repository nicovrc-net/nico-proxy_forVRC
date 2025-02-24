package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

public class Youtube implements ServiceAPI {

    private String url = null;

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"youtu.be", "www.youtube.com"};
    }

    @Override
    public void Set(String json) {
        JsonElement element = Function.gson.fromJson(json, JsonElement.class);
        if (element.isJsonObject() && element.getAsJsonObject().has("URL")){
            url = element.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {
        return "{\"VideoURL\": \"https://yt.8uro.net/r?v="+url+"&o=nicovrc\"}";
    }

    @Override
    public String getServiceName() {
        return "Youtube";
    }

    @Override
    public String getUseProxy() {
        return null;
    }
}
