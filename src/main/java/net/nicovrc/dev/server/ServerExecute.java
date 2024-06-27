package net.nicovrc.dev.server;

import com.google.gson.Gson;
import net.nicovrc.dev.api.*;
import net.nicovrc.dev.data.LogData;
import net.nicovrc.dev.data.ServerData;
import net.nicovrc.dev.data.UDPPacket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerExecute {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Pattern matcher_NicoID = Pattern.compile("^\\d+");
    private static final Pattern matcher_RedirectURL = Pattern.compile("(api\\.nicoad\\.nicovideo\\.jp|b23\\.tv|nico\\.ms|cas\\.nicovideo\\.jp|live2\\.nicovideo\\.jp|abema\\.app|goo\\.gl)");
    private static final Pattern matcher_NicovideoURL = Pattern.compile("(ext|commons)\\.nicovideo\\.jp");
    private static final Pattern matcher_TitleUA = Pattern.compile("(x-nicovrc-titleget: yes|user-agent: unityplayer/)");
    private static final Pattern matcher_YoutubeURL = Pattern.compile("youtube\\.com");
    private static final Pattern matcher_NicoVideoURL = Pattern.compile("www\\.nicovideo\\.jp");
    private static final Pattern matcher_dmcnico = Pattern.compile("dmc\\.nico");
    private static final Pattern matcher_NicoDuration = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">");
    private static final Pattern matcher_Cache1dayURL = Pattern.compile("(abema\\.tv|tiktok\\.com|tver\\.jp|youtu\\.be|youtube\\.com)");
    private static final Pattern matcher_Referer = Pattern.compile("([Rr])eferer: (.+)");
    private static final Pattern matcher_Origin = Pattern.compile("([Oo])rigin: (.+)");
    private static final Pattern matcher_AccessIdentifier = Pattern.compile("\\?access=(.+) HTTP");

    public static void run(CacheAPI CacheAPI, ConversionAPI ConversionAPI, ServerAPI ServerAPI, JinnnaiSystemURL_API JinnnaiAPI, OkHttpClient HttpClient, InputStream in, OutputStream out, Socket sock, DatagramSocket socket, InetSocketAddress address, String httpRequest, String httpVersion, String RequestURL, boolean isWebhook, String WebhookURL, ArrayList<String> WebhookList) throws Exception{

        // 処理鯖に投げるための事前準備
        Matcher getTitle = matcher_TitleUA.matcher(httpRequest.toLowerCase(Locale.ROOT));
        boolean isTitleGet = getTitle.find();
        try {
            UDPPacket packet = new Gson().fromJson(httpRequest, UDPPacket.class);
            isTitleGet = packet.isGetTitle();
        } catch (Exception e){
            //e.printStackTrace();
        }

        // 加工用
        //System.out.println(RequestURL);
        //long start1 = new Date().getTime();
        String TempURL = JinnnaiAPI.replace(RequestURL);
        //System.out.println(TempURL);


        // RequestURL(処理しようとしているURL)が空だったらさっさと301リダイレクトしてしまう
        if (RequestURL.isEmpty()){
            if (socket == null){
                SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=Not Found"+"\nDate: " + new Date() + "\n\n");
            } else {
                UDPPacket packet = new UDPPacket();
                packet.setResultURL("https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=Not Found");
                packet.setGetTitle(isTitleGet);
                SendResult(socket, address, packet);
            }

            SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, "https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg=Not Found", httpRequest, false, false);
            System.out.println("["+sdf.format(new Date())+"] リクエスト (エラー) : " + RequestURL + " ---> Not Found");

            return;
        }

        // sm|nm|am|fz|ut|dm
        if (TempURL.startsWith("sm") || TempURL.startsWith("nm") || TempURL.startsWith("so") || TempURL.startsWith("lv") || matcher_NicoID.matcher(TempURL).find()){
            // 先頭がsm/nm/so/lv/数字のみの場合は先頭に「https://nico.ms/」を追加する
            TempURL = "https://nico.ms/"+TempURL;
        } else if (TempURL.startsWith("ch")){
            // 公式チャンネルのIDの場合はlive.nicovideo.jpを追加
            TempURL = "https://live.nicovideo.jp/watch/" + TempURL;
        }
        //long end1 = new Date().getTime();
        //System.out.println("jinnai : " + (end1 - start1));

        //System.out.println(TempURL);

        // リダイレクト先のURLを渡す
        //long start2 = new Date().getTime();
        final Matcher redirectUrl = matcher_RedirectURL.matcher(TempURL);
        if (redirectUrl.find()){
            try {
                Request request = new Request.Builder()
                        .url(TempURL)
                        .head()
                        .build();
                Response response = HttpClient.newCall(request).execute();
                if (response.body() != null) {
                    TempURL = response.request().url().toString();
                }
                response.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        //long end2 = new Date().getTime();
        //System.out.println("redirect : " + (end2 - start2));
        //System.out.println(TempURL);

        // 置き換え用
        final Matcher replaceUrl = matcher_NicovideoURL.matcher(TempURL);
        if (replaceUrl.find()){
            TempURL = TempURL.replaceAll("ext", "www").replaceAll("commons", "www").replaceAll("thumb", "watch").replaceAll("works", "watch");
        }

        final Matcher matcher1 = matcher_AccessIdentifier.matcher(httpRequest);
        final boolean isNotVRCFlag = matcher1.find() && matcher1.group(1).equals("no_vrc");
        final String[] split = TempURL.split("\\?");
        final String cacheTempURL = split.length >= 2 ? (split[0] + ((matcher_YoutubeURL.matcher(TempURL).find() || isNotVRCFlag) ? "?" + split[1] : "")) : split[0];

        //System.out.println(cacheTempURL);

        if (!isTitleGet){
            // キャッシュ対象の場合はキャッシュチェック
            String cacheUrl = CacheAPI.getCache(cacheTempURL);
            //System.out.println("cache : " + cacheUrl + " / " + cacheTempURL);
            //System.out.println("a");
            if (cacheUrl != null && cacheUrl.equals("pre")){
                //System.out.println("a-2");
                // キャッシュにはあるが処理中の場合 一旦待機してから内容を返す
                while (cacheUrl == null || cacheUrl.equals("pre")){
                    cacheUrl = CacheAPI.getCache(TempURL.split("\\?")[0]);
                    try {
                        Thread.sleep(100L);
                    } catch (Exception e){
                        //e.printStackTrace();
                    }
                }

                System.out.println("["+sdf.format(new Date())+"] リクエスト (キャッシュ) : " + RequestURL + " ---> " + cacheUrl);
                SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, cacheUrl, httpRequest, true, false);

                if (socket == null){
                    SendResult(out,  "HTTP/" + httpVersion + " 302 Found\nLocation: " + cacheUrl + "\nDate: " + new Date() + "\n\n");
                    out.close();
                    in.close();
                    sock.close();
                } else {
                    UDPPacket packet = new UDPPacket();
                    packet.setResultURL(cacheUrl);
                    packet.setGetTitle(false);
                    SendResult(socket, address, packet);
                }

                final String finalCacheUrl = cacheUrl;
                new Thread(()-> ConversionAPI.LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), httpRequest, "Cache", RequestURL, finalCacheUrl, null))).start();
                return;
            } else if (cacheUrl != null && (cacheUrl.startsWith("http://") || cacheUrl.startsWith("https://"))){
                //System.out.println("a-3");
                // 処理中ではなくURLが入っている場合はその結果を返す
                System.out.println("["+sdf.format(new Date())+"] リクエスト (キャッシュ) : " + RequestURL + " ---> " + cacheUrl);
                SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, cacheUrl, httpRequest, true, false);

                if (socket == null){
                    SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: " + cacheUrl + "\nDate: " + new Date() + "\n\n");
                    out.close();
                    in.close();
                    sock.close();
                } else {
                    UDPPacket packet = new UDPPacket();
                    packet.setResultURL(cacheUrl);
                    packet.setGetTitle(false);
                    SendResult(socket, address, packet);
                }

                final String finalCacheUrl = cacheUrl;
                new Thread(() -> ConversionAPI.LogWrite(new LogData(UUID.randomUUID() + "-" + new Date().getTime(), new Date(), httpRequest, "Cache", RequestURL, finalCacheUrl, null))).start();
                return;
            }
            //System.out.println("b");
            // キャッシュない場合は処理中を表す「pre」をキャッシュリストに入れる
            CacheAPI.setCache(cacheTempURL, "pre", new Date().getTime() + 5000L);
        }

        HashMap<String, ServerData> list = ServerAPI.getList();
        if (list.isEmpty()){
            // 処理鯖が設定されていない場合は内部で処理する
            try {
                //System.out.println(TempURL);
                //System.out.println(isTitleGet);
                String ResultURL = ConversionAPI.get(httpRequest, RequestURL, TempURL, isTitleGet);
                if (ResultURL == null){
                    throw new Exception("Not Found");
                }

                if (!isTitleGet){
                    System.out.println("["+sdf.format(new Date())+"] リクエスト : " + RequestURL + " ---> " + ResultURL);
                    SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, ResultURL, httpRequest, false, false);
                    if (socket == null){
                        SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: "+ ResultURL +"\nDate: " + new Date() + "\n\n");
                    } else {
                        //System.out.println("udp send");
                        UDPPacket packet = new UDPPacket();
                        packet.setResultURL(ResultURL);
                        packet.setGetTitle(false);
                        SendResult(socket, address, packet);
                    }
                } else {
                    System.out.println("["+sdf.format(new Date())+"] リクエスト (タイトル取得) : " + RequestURL + " ---> " + ResultURL);
                    SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, ResultURL, httpRequest, false, true);
                    if (socket == null){
                        SendResult(out, "HTTP/" + httpVersion + " 200 OK\nContent-Type: text/plain; charset=utf-8\n\n"+ResultURL);
                    } else {
                        UDPPacket packet = new UDPPacket();
                        packet.setResultURL(ResultURL);
                        packet.setGetTitle(true);
                        SendResult(socket, address, packet);
                    }
                }

                long eTime = 3600000L;
                if (matcher_NicoVideoURL.matcher(TempURL).find()){
                    eTime = new Date().getTime() + 86400000L;
                    if (matcher_dmcnico.matcher(ResultURL).find()){
                        eTime = -1;
                    }
                } else if (matcher_Cache1dayURL.matcher(TempURL).find()) {
                    eTime = new Date().getTime() + 86400000L;
                }

                //System.out.println(cacheTempURL);
                CacheAPI.removeCache(cacheTempURL);
                if (!isTitleGet && !ResultURL.startsWith("https://i2v.nicovrc.net")){

                    CacheAPI.setCache(cacheTempURL, ResultURL, eTime);
                }

            } catch (Exception e){
                if (socket == null){
                    SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg="+e.getMessage()+"\nDate: " + new Date() + "\n\n");
                } else {
                    UDPPacket packet = new UDPPacket();
                    packet.setResultURL("https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg="+e.getMessage());
                    packet.setGetTitle(isTitleGet);
                    SendResult(socket, address, packet);
                }

                CacheAPI.removeCache(cacheTempURL);
                SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, "https://i2v.nicovrc.net/?url=https://nicovrc.net/php/mojimg.php?msg="+e.getMessage(), httpRequest, false, false);
                System.out.println("["+sdf.format(new Date())+"] リクエスト (エラー) : " + RequestURL + " ---> " + e.getMessage());
            }
        } else {
            // 処理鯖が設定されている場合は処理鯖へ投げてその結果を返す
            UDPPacket packet = new UDPPacket(RequestURL, TempURL);
            packet.setGetTitle(isTitleGet);
            packet.setHTTPRequest(httpRequest);

            final String s = UUID.randomUUID().toString().split("-")[0] + "_" + new Date().getTime() + "_" + Constant.getVersion();
            String temp = "";
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                byte[] sha256Byte = sha256.digest(s.getBytes());
                HexFormat hex = HexFormat.of().withLowerCase();

                temp = hex.formatHex(sha256Byte);
            } catch (Exception e){
                temp = s;
            }
            packet.setRequestID(temp);

            UDPPacket result = ServerAPI.SendServer(packet);
            System.out.println(result.getErrorMessage());
            if (result.getResultURL() != null){
                if (isTitleGet){
                    System.out.println("["+sdf.format(new Date())+"] リクエスト (タイトル取得) : " + RequestURL + " ---> " + result.getResultURL());
                    //SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, result.getResultURL(), false, true);
                    if (socket == null){
                        SendResult(out, "HTTP/" + httpVersion + " 200 OK\nContent-Type: text/plain; charset=utf-8\n\n"+result.getResultURL());
                    } else {
                        UDPPacket packet1 = new UDPPacket();
                        packet1.setResultURL(result.getResultURL());
                        packet1.setGetTitle(true);
                        SendResult(socket, address, packet1);
                    }

                    CacheAPI.removeCache(cacheTempURL);

                } else {
                    System.out.println("["+sdf.format(new Date())+"] リクエスト : " + RequestURL + " ---> " + result.getResultURL());
                    //SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, result.getResultURL(), false, false);
                    if (socket == null){
                        SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: "+result.getResultURL()+"\nDate: " + new Date() + "\n\n");
                    } else {
                        UDPPacket packet1 = new UDPPacket();
                        packet1.setResultURL(result.getResultURL());
                        packet1.setGetTitle(false);
                        SendResult(socket, address, packet1);
                    }

                    long eTime = 3600000L;
                    if (matcher_NicoVideoURL.matcher(TempURL).find()){
                        eTime = new Date().getTime() + 86400000L;
                        if (matcher_dmcnico.matcher(result.getResultURL()).find()){
                            eTime = -1;
                        }
                    } else if (matcher_Cache1dayURL.matcher(TempURL).find()) {
                        eTime = new Date().getTime() + 86400000L;
                    }

                    CacheAPI.removeCache(cacheTempURL);
                    if (!result.getResultURL().startsWith("https://i2v.nicovrc.net")){
                        CacheAPI.setCache(cacheTempURL, result.getResultURL(), eTime);
                    }
                }
            } else {
                String ResultURL = ConversionAPI.get(httpRequest, RequestURL, TempURL, isTitleGet);
                if (ResultURL == null){
                    throw new Exception("Not Found");
                }

                if (!isTitleGet){
                    System.out.println("["+sdf.format(new Date())+"] リクエスト : " + RequestURL + " ---> " + ResultURL);
                    //SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, ResultURL, false, false);
                    if (socket == null){
                        SendResult(out, "HTTP/" + httpVersion + " 302 Found\nLocation: "+ ResultURL +"\nDate: " + new Date() + "\n\n");
                    } else {
                        UDPPacket packet1 = new UDPPacket();
                        packet1.setResultURL(ResultURL);
                        packet1.setGetTitle(false);
                        SendResult(socket, address, packet1);
                    }
                } else {
                    System.out.println("["+sdf.format(new Date())+"] リクエスト (タイトル取得) : " + RequestURL + " ---> " + ResultURL);
                    //SendWebhook(isWebhook, WebhookURL, WebhookList, RequestURL, ResultURL, false, true);
                    if (socket == null){
                        SendResult(out, "HTTP/" + httpVersion + " 200 OK\nContent-Type: text/plain; charset=utf-8\n\n"+ResultURL);
                    } else {
                        UDPPacket packet1 = new UDPPacket();
                        packet1.setResultURL(ResultURL);
                        packet1.setGetTitle(true);
                        SendResult(socket, address, packet1);
                    }
                }

                long eTime = 3600000L;
                if (matcher_NicoVideoURL.matcher(TempURL).find()){
                    eTime = new Date().getTime() + 86400000L;
                    if (matcher_dmcnico.matcher(ResultURL).find()){
                        eTime = -1;
                    }
                } else if (matcher_Cache1dayURL.matcher(TempURL).find()) {
                    eTime = new Date().getTime() + 86400000L;
                }

                CacheAPI.removeCache(cacheTempURL);
                if (!result.getResultURL().startsWith("https://i2v.nicovrc.net")){
                    CacheAPI.setCache(cacheTempURL, result.getResultURL(), eTime);
                }
            }
        }

    }


    private static void SendResult(OutputStream out, String Result){
        try {
            out.write(Result.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e){
            // e.printStackTrace();
        }
    }

    private static void SendResult(DatagramSocket socket, InetSocketAddress address, UDPPacket packet){
        try {
            socket.send(new DatagramPacket(new Gson().toJson(packet).getBytes(StandardCharsets.UTF_8), new Gson().toJson(packet).getBytes(StandardCharsets.UTF_8).length, address));
        } catch (Exception e){
            // e.printStackTrace();
        }
    }

    private static void SendWebhook(boolean isWebhook, String WebhookURL, ArrayList<String> WebhookList, String RequestURL, String ResultURL, String RequestHeader, boolean isCache, boolean isTitle) {

        if (!isWebhook) {
            return;
        }

        if (WebhookURL.isEmpty()) {
            return;
        }

        final String jsonText;

        Matcher referer = matcher_Referer.matcher(RequestHeader);
        Matcher origin = matcher_Origin.matcher(RequestHeader);
        Matcher access = matcher_AccessIdentifier.matcher(RequestHeader);
        if (referer.find()) {
            jsonText = "" +
                    "{" +
                    "  \"username\": \"nico-proxy_forVRC (Ver " + Constant.getVersion() + ")\"," +
                    "  \"avatar_url\": \"https://r2.7mi.site/vrc/nico/nc296562.png\"," +
                    "  \"content\": \"利用ログ\"," +
                    "  \"embeds\": [" +
                    "    {" +
                    "      \"title\": \"" + (isCache ? "キャッシュ" : "新規") + "\"," +
                    "      \"description\": \"" + sdf.format(new Date()) + "\"," +
                    "      \"fields\": [" +
                    "        {" +
                    "          \"name\": \"リクエストURL\"," +
                    "          \"value\": \"" + RequestURL + "\"" +
                    "        }," +
                    "        {" +
                    "          \"name\": \"処理結果" + (isTitle ? "タイトル" : "URL") + "\"," +
                    "          \"value\": \"" + ResultURL + "\"" +
                    "        }," +
                    "        {" +
                    "          \"name\": \"リファラ\"," +
                    "          \"value\": \"" + referer.group(2) + "\"" +
                    "        }" +
                    "      ]" +
                    "    }" +
                    "  ]" +
                    "}";
        } else if (origin.find()) {
            jsonText = "" +
                    "{" +
                    "  \"username\": \"nico-proxy_forVRC (Ver " + Constant.getVersion() + ")\"," +
                    "  \"avatar_url\": \"https://r2.7mi.site/vrc/nico/nc296562.png\"," +
                    "  \"content\": \"利用ログ\"," +
                    "  \"embeds\": [" +
                    "    {" +
                    "      \"title\": \"" + (isCache ? "キャッシュ" : "新規") + "\"," +
                    "      \"description\": \"" + sdf.format(new Date()) + "\"," +
                    "      \"fields\": [" +
                    "        {" +
                    "          \"name\": \"リクエストURL\"," +
                    "          \"value\": \"" + RequestURL + "\"" +
                    "        }," +
                    "        {" +
                    "          \"name\": \"処理結果" + (isTitle ? "タイトル" : "URL") + "\"," +
                    "          \"value\": \"" + ResultURL + "\"" +
                    "        }," +
                    "        {" +
                    "          \"name\": \"オリジン\"," +
                    "          \"value\": \"" + origin.group(2) + "\"" +
                    "        }" +
                    "      ]" +
                    "    }" +
                    "  ]" +
                    "}";
        } else if (access.find()){
            jsonText = "" +
                    "{" +
                    "  \"username\": \"nico-proxy_forVRC (Ver " + Constant.getVersion() + ")\"," +
                    "  \"avatar_url\": \"https://r2.7mi.site/vrc/nico/nc296562.png\"," +
                    "  \"content\": \"利用ログ\"," +
                    "  \"embeds\": [" +
                    "    {" +
                    "      \"title\": \"" + (isCache ? "キャッシュ" : "新規") + "\"," +
                    "      \"description\": \"" + sdf.format(new Date()) + "\"," +
                    "      \"fields\": [" +
                    "        {" +
                    "          \"name\": \"リクエストURL\"," +
                    "          \"value\": \"" + RequestURL + "\"" +
                    "        }," +
                    "        {" +
                    "          \"name\": \"処理結果" + (isTitle ? "タイトル" : "URL") + "\"," +
                    "          \"value\": \"" + ResultURL + "\"" +
                    "        }," +
                    "        {" +
                    "          \"name\": \"アクセス識別子\"," +
                    "          \"value\": \"" + access.group(1) + "\"" +
                    "        }" +
                    "      ]" +
                    "    }" +
                    "  ]" +
                    "}";
        } else {
            jsonText = "" +
                    "{"+
                    "  \"username\": \"nico-proxy_forVRC (Ver " + Constant.getVersion() + ")\","+
                    "  \"avatar_url\": \"https://r2.7mi.site/vrc/nico/nc296562.png\","+
                    "  \"content\": \"利用ログ\","+
                    "  \"embeds\": ["+
                    "    {"+
                    "      \"title\": \""+(isCache ? "キャッシュ" : "新規")+"\","+
                    "      \"description\": \""+sdf.format(new Date())+"\","+
                    "      \"fields\": ["+
                    "        {"+
                    "          \"name\": \"リクエストURL\","+
                    "          \"value\": \""+RequestURL+"\""+
                    "        },"+
                    "        {"+
                    "          \"name\": \"処理結果"+(isTitle ? "タイトル" : "URL")+"\","+
                    "          \"value\": \""+ResultURL+"\""+
                    "        }"+
                    "      ]"+
                    "    }"+
                    "  ]" +
                    "}";
        }

        WebhookList.add(jsonText);
    }

}
