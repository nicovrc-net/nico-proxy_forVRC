package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicoVideo implements ServiceAPI {
    private final Gson gson = Function.gson;
    private final String[] SupportURL = {"www.nicovideo.jp", "live.nicovideo.jp", "nico.ms", "cas.nicovideo.jp"};
    private final Pattern NicoID1 = Pattern.compile("(http|https)://(live|www)\\.nicovideo\\.jp/watch/(.+)");
    private final Pattern NicoID2 = Pattern.compile("(http|https)://nico\\.ms/(.+)");
    private final Pattern NicoID3 = Pattern.compile("(http|https)://cas\\.nicovideo\\.jp/user/(.+)");
    private String URL = null;

    @Override
    public String[] getCorrespondingURL() {
        return SupportURL;
    }

    @Override
    public void Set(String json) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.URL = jsonElement.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {
        if (URL == null || URL.isEmpty()){
            return "{\"ErrorMessage\": \"URLがありません\"}";
        }

        String url = URL.split("\\?")[0];
        Matcher matcher_normal = NicoID1.matcher(url);
        Matcher matcher_short = NicoID2.matcher(url);
        Matcher matcher_cas = NicoID3.matcher(url);

        boolean isNormal = matcher_normal.find();
        boolean isShort = matcher_short.find();
        boolean isCas = matcher_cas.find();

        if (!isNormal && !isShort && !isCas){
            url = null;
            matcher_normal = null;
            matcher_short = null;
            matcher_cas = null;
            return "{\"ErrorMessage\": \"URLが間違っているか対応してないURLです。\"}";
        }



        matcher_normal = null;
        matcher_short = null;
        matcher_cas = null;



        return "";
    }

}
