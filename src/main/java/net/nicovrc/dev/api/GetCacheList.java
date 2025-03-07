package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public class GetCacheList implements NicoVRCAPI {


    private final Pattern matcher = Pattern.compile("\\.");

    @Override
    public String getURI() {
        return "/api/v1/get_cachelist";
    }

    @Override
    public String Run(String httpRequest) {

        HashMap<String, String> map = new HashMap<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        Function.CacheList.forEach((url, data)->{
            map.put(url, format.format(new Date(data.getCacheDate())));
        });

        return Function.gson.toJson(map);

    }
}
