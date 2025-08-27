package net.nicovrc.dev.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class GetVideoInfo implements NicoVRCAPI {

    private final List<ServiceAPI> siteList = ServiceList.getServiceList();
    private final Pattern pattern_Asterisk = Pattern.compile("\\*");

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
        String[] get = new String[]{"{\"ErrorMessage\": \"対応していないサイトです。\"}"};
        for (ServiceAPI site : siteList){
            String[] urls = site.getCorrespondingURL();
            for (String url : urls){
                Pattern matcher_0 = null;
                if (pattern_Asterisk.matcher(url).find()){
                    //System.out.println(url.replaceAll("\\.", "\\\\.").replaceAll("\\*", "(.+)"));
                    matcher_0 = Pattern.compile(url.replaceAll("\\.", "\\\\.").replaceAll("\\*", "(.+)"));
                }

                if (!isFound[0] && (inputUrl.startsWith("http://"+url) || inputUrl.startsWith("https://"+url) || inputUrl.startsWith(url)) || (matcher_0 != null && matcher_0.matcher(inputUrl).find())){

                    if (url.equals("so") && inputUrl.startsWith("http")){
                        continue;
                    }


                    if (url.startsWith("twitcasting.tv")) {
                        //System.out.println("a");
                        site.Set("{\"URL\": \"" + inputUrl + "\", \"ClientID\": \"" + Function.config_twitcast_ClientId + "\", \"ClientSecret\": \"" + Function.config_twitcast_ClientSecret + "\"}");
                    } else if (site.getServiceName().equals("ニコニコ")){
                        if (Function.config_user_session != null && Function.config_user_session_secure != null && Function.config_nicosid != null){
                            site.Set("{\"URL\":\""+inputUrl+"\", \"user_session\":\""+Function.config_user_session+"\", \"user_session_secure\":\""+Function.config_user_session_secure+"\", \"nicosid\":\""+Function.config_nicosid+"\"}");
                        } else {
                            site.Set("{\"URL\":\""+inputUrl+"\"}");
                        }
                    } else {
                        site.Set("{\"URL\": \""+inputUrl+"\"}");
                    }
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
