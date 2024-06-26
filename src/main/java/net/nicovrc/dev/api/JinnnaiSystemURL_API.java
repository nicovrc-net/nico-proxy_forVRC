package net.nicovrc.dev.api;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JinnnaiSystemURL_API {

    private final HashMap<Pattern, String> JinnnaiSystem_URL = new HashMap<>();

    public JinnnaiSystemURL_API(){
        // 陣内システムのURL挿入
        JinnnaiSystem_URL.put(Pattern.compile("http://yt\\.8uro\\.net/r\\?v=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://yt\\.8uro\\.net/r\\?v=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://nextnex\\.com/\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://nextnex\\.com/\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://vrc\\.kuroneko6423\\.com/proxy\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://vrc\\.kuroneko6423\\.com/proxy\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://kvvs\\.net/proxy\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://kvvs\\.net/proxy\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://questify\\.dev/\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://questify\\.dev/\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://questing\\.thetechnolus\\.com/v\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://questing\\.thetechnolus\\.com/v\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://questing\\.thetechnolus\\.com/(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://questing\\.thetechnolus\\.com/(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://vq\\.vrcprofile\\.com/\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://vq\\.vrcprofile\\.com/\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://api\\.yamachan\\.moe/proxy\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://api\\.yamachan\\.moe/proxy\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://nicovrc\\.net/proxy/\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://nicovrc\\.net/proxy/\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://nicovrc\\.net/proxy/dummy\\.m3u8\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://nicovrc\\.net/proxy/dummy\\.m3u8\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://nico\\.7mi\\.site/proxy/\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://nico\\.7mi\\.site/proxy/\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://nico\\.7mi\\.site/proxy/dummy\\.m3u8\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://nico\\.7mi\\.site/proxy/dummy\\.m3u8\\?(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://qst\\.akakitune87\\.net/q\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://qst\\.akakitune87\\.net/q\\?url=(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("http://u2b\\.cx/(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://u2b\\.cx/(.+)"), "");
        JinnnaiSystem_URL.put(Pattern.compile("https://k\\.0cm\\.org/\\?url=(.+)"), "");

        JinnnaiSystem_URL.put(Pattern.compile("http://shay\\.loan/(.+)"), "https://youtu.be/");
        JinnnaiSystem_URL.put(Pattern.compile("https://shay\\.loan/(.+)"), "https://youtu.be/");
        JinnnaiSystem_URL.put(Pattern.compile("http://questing\\.thetechnolus.com/watch\\?v=(.+)"), "https://youtu.be/");
        JinnnaiSystem_URL.put(Pattern.compile("https://questing\\.thetechnolus.com/watch\\?v=(.+)"), "https://youtu.be/");
        JinnnaiSystem_URL.put(Pattern.compile("http://questing\\.thetechnolus.com/v/(.+)"), "https://youtu.be/");
        JinnnaiSystem_URL.put(Pattern.compile("https://questing\\.thetechnolus.com/v/(.+)"), "https://youtu.be/");
        JinnnaiSystem_URL.put(Pattern.compile("http://youtube\\.irunu.co/watch\\?v=(.+)"), "https://youtu.be/");
        JinnnaiSystem_URL.put(Pattern.compile("https://youtube\\.irunu.co/watch\\?v=(.+)"), "https://youtu.be/");

        JinnnaiSystem_URL.put(Pattern.compile("http://www\\.nicovideo\\.life/watch\\?v=(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("https://www\\.nicovideo\\.life/watch\\?v=(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("http://live\\.nicovideo\\.life/watch\\?v=(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("https://live\\.nicovideo\\.life/watch\\?v=(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("https://shinchan\\.biz/player\\.html\\?video_id=(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("https://k\\.0cm\\.org/\\?u=nico\\.ms%2F(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("https://www\\.nicozon\\.net/player\\.html\\?video_id=(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("http://suzumebachi\\.xyz:1323/go/(.+)"), "https://nico.ms/");
        JinnnaiSystem_URL.put(Pattern.compile("http://suzumebachi\\.xyz:1323/tmsk/(.+)"), "https://nico.ms/");

    }

    /**
     * @param URL 処理したいURL
     * @return 処理結果
     */
    public String replace(String URL){
        final String tempUrl;
        if (URL.startsWith("http://yt.8uro.net") || URL.startsWith("https://yt.8uro.net") || URL.startsWith("http://k.0cm.org/") || URL.startsWith("https://k.0cm.org/")) {
            tempUrl = URLDecoder.decode(URL, StandardCharsets.UTF_8);
        } else {
            tempUrl = URL;
        }

        String[] temp = {null};

        JinnnaiSystem_URL.forEach((ji, re) -> {
            if (temp[0] != null){
                return;
            }

            // http://api.yamachan.moe/proxy?url=https://nico.ms/sm9
            Matcher matcher = ji.matcher(tempUrl);
            if (matcher.find()){
                //System.out.println(ji + " is found!!");
                //System.out.println(re + matcher.group(1));
                temp[0] = re + matcher.group(1);
            }
        });

        return temp[0] == null ? URL : temp[0];
    }

}
