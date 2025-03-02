package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class GetSupportList implements NicoVRCAPI {

    private final List<ServiceAPI> siteList = ServiceList.getServiceList();

    private final Pattern matcher = Pattern.compile("\\.");

    @Override
    public String getURI() {
        return "/api/v1/get_supportlist";
    }

    @Override
    public String Run(String httpRequest) {

        HashMap<String, String[]> map = new HashMap<>();
        siteList.forEach((value)->{
            if (!value.getServiceName().equals("ニコニコ")){
                map.put(value.getServiceName(), value.getCorrespondingURL());
            } else {

                String[] tempList = null;
                int i = 0;
                for (String str : value.getCorrespondingURL()){
                    if (matcher.matcher(str).find()){
                        i++;
                    }
                }
                tempList = new String[i + 1];
                i = 0;
                for (String str : value.getCorrespondingURL()){
                    if (matcher.matcher(str).find()){
                        tempList[i] = str;
                        i++;
                    }
                }

                map.put(value.getServiceName(), tempList);
            }
        });

        return Function.gson.toJson(map);

    }
}
