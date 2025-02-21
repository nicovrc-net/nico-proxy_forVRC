package net.nicovrc.dev.http;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;
import net.nicovrc.dev.api.NicoVRCAPI;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GetURL implements Runnable, NicoVRCHTTP {

    private Socket sock = null;
    private String URL = null;
    private String httpRequest = null;

    private final List<ServiceAPI> list = ServiceList.getServiceList();

    @Override
    public void run() {

        StringBuilder sb = new StringBuilder("--- dummy ---\n");
        sb.append(httpRequest);

        String method = Function.getMethod(httpRequest);

        try {
            URL = URL.replaceAll("^(/\\?url=|/\\?vi=|/proxy/\\?)", "");

            ServiceAPI api = null;
            for (ServiceAPI vrcapi : list) {
                boolean isFound = false;
                for (String str : vrcapi.getCorrespondingURL()) {
                    if (URL.startsWith("https://"+str) || URL.startsWith("http://"+str) || URL.startsWith(str)){
                        api = vrcapi;
                        isFound = true;
                    }
                }

                if (isFound){
                    break;
                }
            }
            sb.append(URL).append("\n\n");

            String json = null;
            String ServiceName = null;
            if (api != null){
                api.Set("{\"URL\":\""+URL+"\"}");
                json = api.Get();
                ServiceName = api.getServiceName();
            }

            System.out.println(json);
            if (json != null){
                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "text/plain; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
            } else {
                File file = new File("./error-video/error_404.mp4");
                byte[] content = new byte[0];
                if (file.exists()){
                    FileInputStream stream = new FileInputStream(file);
                    content = stream.readAllBytes();
                    stream.close();
                    stream = null;
                }
                //System.out.println(content.length);
                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 404, "video/mp4", content, method != null && method.equals("HEAD"));
                file = null;
                content = null;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public String getStartURI() {
        return "/?url=";
    }

    @Override
    public void setHTTPRequest(String httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public void setURL(String URL) {
        this.URL = URL;
    }

    @Override
    public void setHTTPSocket(Socket sock) {
        this.sock = sock;
    }
}
