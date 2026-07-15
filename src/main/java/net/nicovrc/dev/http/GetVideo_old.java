package net.nicovrc.dev.http;

import java.net.http.HttpClient;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.regex.Pattern;

@Deprecated
public class GetVideo_old implements Runnable, NicoVRCHTTP {

    private String httpRequest = null;
    private String URL = null;
    private AsynchronousSocketChannel ch = null;
    private HttpClient client = null;

    private final Pattern matcher_cookie = Pattern.compile("cookie:\\[(.*)\\]");
    private final Pattern matcher_referer = Pattern.compile("referer:\\[(.*)\\]");
    private final Pattern matcher_url1 = Pattern.compile("^/https/(cookie|referer):\\[(.*)\\]/(.*)");
    private final Pattern matcher_url2 = Pattern.compile("^/https/(cookie|referer):\\[(.*)\\]/(cookie|referer):\\[(.*)\\]/(.*)");

    private final Pattern matcher_twitcasting = Pattern.compile("twitcasting\\.tv");
    private final Pattern matcher_abema = Pattern.compile("(.+)-abematv\\.akamaized\\.net");
    private final Pattern matcher_vimeo = Pattern.compile("vimeocdn\\.com");
    private final Pattern matcher_fc2 = Pattern.compile("(.+)\\.live\\.fc2\\.com");
    private final Pattern matcher_tiktok = Pattern.compile("tiktok\\.com");
    private final Pattern matcher_bilicom = Pattern.compile("bilibili\\.com");

    private final Pattern matcher_bili_range1 = Pattern.compile("[r|R]ange: bytes=(\\d+)-(\\d+)");
    private final Pattern matcher_bili_range2 = Pattern.compile("[r|R]ange: bytes=(\\d+)-");

    private final String http = "https://";

    @Override
    public void run() {
        if (client == null){
            return;
        }

        if (ch == null){
            return;
        }
    }

    @Override
    public String getStartURI() {
        return "/https/";
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
    public void setHTTPSocket(AsynchronousSocketChannel ch) {
        this.ch = ch;
    }

    @Override
    public void setHTTPClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setProxy(String proxy) {

    }
}
