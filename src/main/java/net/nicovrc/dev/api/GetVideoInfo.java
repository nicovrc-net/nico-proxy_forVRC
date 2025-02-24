package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.NicoVideo;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.bilibili_com;

import java.util.ArrayList;
import java.util.List;

public class GetVideoInfo implements NicoVRCAPI {

    private final List<ServiceAPI> siteList = new ArrayList<>();

    public GetVideoInfo(){
        siteList.add(new NicoVideo());
        siteList.add(new bilibili_com());
    }

    @Override
    public String getURI() {
        return "/api/v1/videoinfo";
    }

    @Override
    public String Run(String httpRequest) {
        String uri = Function.getURI(httpRequest);
        String inputUrl = "";
        if (!uri.equals(getURI())){
            inputUrl = uri.split("&url=")[1];
        }


        boolean[] isFound = {false};
        String[] get = new String[]{""};
        for (ServiceAPI site : siteList){
            String[] urls = site.getCorrespondingURL();
            for (String url : urls){
                if (inputUrl.startsWith("https://"+url)){
                    site.Set("{\"URL\": \""+inputUrl+"\"}");
                    get[0] = site.Get();
                    isFound[0] = true;
                }
                if (!isFound[0] && inputUrl.startsWith(url)){
                    site.Set("{\"URL\": \""+inputUrl+"\"}");
                    get[0] = site.Get();
                    isFound[0] = true;
                }
            }

            if (isFound[0]){
                break;
            }
        }

        return get[0];

    }
}
