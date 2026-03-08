package net.nicovrc.dev.api;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetVideoInfo implements NicoVRCAPI {

    private final List<ServiceAPI> siteList = ServiceList.getServiceList();
    private final Pattern pattern_Asterisk = Pattern.compile("\\*");

    @Override
    public String getURI() {
        return "/api/v1/videoinfo";
    }

    @Override
    public String Run(String httpRequest, HttpClient client) {
        String uri = Function.getURI(httpRequest);
        String inputUrl = "";
        if (!uri.equals(getURI())){
            if (uri.split("&url=").length > 1){
                inputUrl = uri.split("&url=")[1];
            } else {
                inputUrl = uri.split("\\?url=")[1];
            }
        }

        HttpClient httpClient = client;
        try {
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
                            site.Set("{\"URL\": \"" + inputUrl + "\", \"ClientID\": \"" + Function.config_twitcast_ClientId + "\", \"ClientSecret\": \"" + Function.config_twitcast_ClientSecret + "\"}", httpClient);
                        } else if (site.getServiceName().equals("ニコニコ")){
                            if (Function.config_user_session != null && Function.config_nicosid != null){
                                site.Set("{\"URL\":\""+inputUrl+"\", \"user_session\":\""+Function.config_user_session+"\", \"nicosid\":\""+Function.config_nicosid+"\"}", httpClient);
                            } else {
                                site.Set("{\"URL\":\""+inputUrl+"\"}", httpClient);
                            }
                        } else {
                            site.Set("{\"URL\": \""+inputUrl+"\"}", httpClient);
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

        } catch (Exception e){
            e.printStackTrace();
        }

        return "{\"ErrorMessage\": \"対応していないサイトです。\"}";


    }
}
