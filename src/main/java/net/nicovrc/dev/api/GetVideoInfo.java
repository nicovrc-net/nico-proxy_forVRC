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

        // Proxy
        String p = null;
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            p = Function.ProxyList.get(i);
        }

        final String targetUrl = inputUrl.split("\\?")[0];
        final Matcher matcher_normal = Function.NicoID1.matcher(targetUrl);
        final Matcher matcher_short = Function.NicoID2.matcher(targetUrl);
        final Matcher matcher_cas = Function.NicoID3.matcher(targetUrl);
        final Matcher matcher_idOnly = Function.NicoID4.matcher(targetUrl);

        final boolean isNormal = matcher_normal.find();
        final boolean isShort = matcher_short.find();
        final boolean isCas = matcher_cas.find();
        final boolean isID = matcher_idOnly.find();

        String id = "";

        if (isID){
            id = matcher_idOnly.group(1);
        } else if (isNormal){
            id = matcher_normal.group(3);
        } else if (isShort) {
            id = matcher_short.group(2);
        }

        if (isID || isNormal || isShort){
            if (id.startsWith("lv") || id.startsWith("so")){
                if (!Function.JP_ProxyList.isEmpty()){
                    int i = Function.JP_ProxyList.size() > 1 ? new SecureRandom().nextInt(0, Function.JP_ProxyList.size()) : 0;
                    p = Function.JP_ProxyList.get(i);
                }
            }
        }

        if (isCas){
            if (!Function.JP_ProxyList.isEmpty()){
                int i = Function.JP_ProxyList.size() > 1 ? new SecureRandom().nextInt(0, Function.JP_ProxyList.size()) : 0;
                p = Function.JP_ProxyList.get(i);
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
