package net.nicovrc.dev.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;

import java.io.File;
import java.util.List;

public class GetVideoInfo implements NicoVRCAPI {

    private final List<ServiceAPI> siteList = ServiceList.getServiceList();

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
                if (!isFound[0] && (inputUrl.startsWith("http://"+url) || inputUrl.startsWith("https://"+url) || inputUrl.startsWith(url))){

                    if (url.equals("so") && inputUrl.startsWith("http")){
                        continue;
                    }

                    if (url.startsWith("twitcasting.tv")){
                        String ClientId = "";
                        String ClientSecret = "";

                        try {
                            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                            ClientId = yamlMapping.string("TwitcastingClientID");
                            ClientSecret = yamlMapping.string("TwitcastingClientSecret");
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        //System.out.println("a");
                        site.Set("{\"URL\": \""+inputUrl+"\", \"ClientID\": \""+ClientId+"\", \"ClientSecret\": \""+ClientSecret+"\"}");
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
