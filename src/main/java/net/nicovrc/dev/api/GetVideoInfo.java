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
                        String ClientId = "";
                        String ClientSecret = "";

                        try {
                            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                            ClientId = yamlMapping.string("TwitcastingClientID");
                            ClientSecret = yamlMapping.string("TwitcastingClientSecret");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //System.out.println("a");
                        site.Set("{\"URL\": \"" + inputUrl + "\", \"ClientID\": \"" + ClientId + "\", \"ClientSecret\": \"" + ClientSecret + "\"}");
                    } else if (site.getServiceName().equals("ニコニコ")){
                        String user_session = null;
                        String user_session_secure = null;

                        try {
                            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                            user_session = yamlMapping.string("NicoNico_user_session");
                            user_session_secure = yamlMapping.string("NicoNico_user_session_secure");
                        } catch (Exception e){
                            //e.printStackTrace();
                        }

                        if (user_session != null && user_session_secure != null){
                            site.Set("{\"URL\":\""+inputUrl+"\", \"user_session\":\""+user_session+"\", \"user_session_secure\":\""+user_session_secure+"\"}");
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
