package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;

import java.net.http.HttpClient;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class GetCacheList implements NicoVRCAPI {


    private final SimpleDateFormat format = Function.sdf;

    @Override
    public String getURI() {
        return "/api/v1/get_cachelist";
    }

    @Override
    public String Run(String httpRequest, HttpClient client) {

        HashMap<String, String> map = new HashMap<>();

        Function.getCacheList().forEach((url, data)->{
            map.put(url, format.format(new Date(data.getCacheDate())));
        });

        return Function.gson.toJson(map);

    }
}
