package xyz.n7mn.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BilibiliCom implements ShareService{
    @Override
    public String getVideo(String url, ProxyData proxy) throws Exception {

        String s = url.split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.length() == 0 || id.startsWith("?")){
            id = strings[strings.length - 2];
        }

        //System.out.println("debug id : "+id);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        final OkHttpClient client = proxy != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getProxyIP(), proxy.getPort()))).build() : new OkHttpClient();
        final String HtmlText;
        Request request_html = new Request.Builder()
                .url("https://api.bilibili.com/x/web-interface/view?bvid="+id)
                .build();

        try {
            Response response = client.newCall(request_html).execute();
            HtmlText = response.body().string();
        } catch (IOException e) {
            throw new Exception("api.bilibili.com/x/web-interface " + e.getMessage() + (proxy == null ? "" : "(Use Proxy : "+proxy.getProxyIP()+")"));
        }

        //
        Matcher matcher = Pattern.compile("\"cid\":(\\d+),").matcher(HtmlText);
        if (!matcher.find()){
            throw new Exception("api.bilibili.com (Not cid Found)");
        }

        String cid = matcher.group(1);

        //System.out.println(cid);

        final String ResultText;
        Request request_api = new Request.Builder()
                .url("https://api.bilibili.com/x/player/playurl?bvid="+id+"&cid="+cid)
                .build();

        try {
            Response response2 = client.newCall(request_api).execute();
            ResultText = response2.body().string();
        } catch (IOException e) {
            throw new Exception("api.bilibili.com/x/player " + e.getMessage() + (proxy == null ? "" : "(Use Proxy : "+proxy.getProxyIP()+")"));
        }

        Matcher matcher2 = Pattern.compile("\"url\":\"(.*)\",\"backup_url\"").matcher(ResultText);
        Matcher matcher3 = Pattern.compile(",\"backup_url\":\\[(.*)\\]\\}\\],\"support_formats\":").matcher(ResultText);
        final String temp_url;
        if (matcher2.find()){
            temp_url = matcher2.group(1).replaceAll("\\\\u0026","&");
        } else {
            temp_url = null;
        }

        String temp = null;
        if (matcher3.find()){
            temp = matcher3.group(1).replaceAll("\\\\u0026", "&");
        }

        if (temp != null){
            temp = temp.split("\"")[1];
            //System.out.println(temp);
        }


        if (!temp_url.startsWith("https://upos-hz-mirrorakam.akamaized.net/")){
            return temp;
        }

        return temp_url;
    }

    @Override
    public String getLive(String url, ProxyData proxy) throws Exception {
        // 現時点(2023/5/31)では実装しない
        return null;
    }

    @Override
    public String getServiceName() {
        return "bilibili";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
