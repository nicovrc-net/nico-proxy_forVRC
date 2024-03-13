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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerExecute {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void run(CacheAPI CacheAPI, ConversionAPI ConversionAPI, ServerAPI ServerAPI, JinnnaiSystemURL_API JinnnaiAPI, OkHttpClient HttpClient, InputStream in, OutputStream out, Socket sock, DatagramSocket socket, InetSocketAddress address, String httpRequest, String httpVersion, String RequestURL, boolean isWebhook, String WebhookURL, ArrayList<String> WebhookList) throws Exception{

        // 加工用
        //System.out.println(RequestURL);
        String TempURL = JinnnaiAPI.replace(RequestURL);
        //System.out.println(TempURL);

        // 先頭がsm/nm/so/lv/数字のみの場合は先頭に「https://nico.ms/」を追加する
        if (TempURL.startsWith("sm") || TempURL.startsWith("nm") || TempURL.startsWith("so") || TempURL.startsWith("lv") || Pattern.compile("^\\d+").matcher(TempURL).find()){
            TempURL = "https://nico.ms/"+TempURL;
        }
        //System.out.println(TempURL);

        // リダイレクト先のURLを渡す
        final Matcher redirectUrl = Pattern.compile("(api\\.nicoad\\.nicovideo\\.jp|b23\\.tv|nico\\.ms)").matcher(TempURL);
        if (redirectUrl.find()){
            try {
                Request request = new Request.Builder()
                        .url(TempURL)
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
        //System.out.println(TempURL);

        // 置き換え用
        final Matcher replaceUrl = Pattern.compile("(ext|commons)\\.nicovideo\\.jp").matcher(TempURL);
        if (replaceUrl.find()){
            TempURL = TempURL.replaceAll("ext", "www").replaceAll("commons", "www").replaceAll("thumb", "watch").replaceAll("works", "watch");
        }

        //System.out.println(TempURL);

        // 処理鯖に投げるための事前準備
        final Matcher CacheCheck = Pattern.compile("(nicovideo\\.jp|bilibili|tver\\.jp|xvideos\\.com|abema\\.tv|tiktok\\.com)").matcher(TempURL.split("\\?")[0]);
        final boolean isCache = CacheCheck.find();

        Matcher getTitle = Pattern.compile("(x-nicovrc-titleget: yes|user-agent: unityplayer/)").matcher(httpRequest.toLowerCase(Locale.ROOT));
        boolean isTitleGet = getTitle.find();
        try {
            UDPPacket packet = new Gson().fromJson(httpRequest, UDPPacket.class);
            isTitleGet = packet.isGetTitle();
        } catch (Exception e){
            //e.printStackTrace();
        }

        if (isCache && !isTitleGet){
            // キャッシュ対象の場合はキャッシュチェック
            String cacheUrl = CacheAPI.getCache(TempURL.split("\\?")[0]);
            //System.out.println("a");
            if (cacheUrl != null && cacheUrl.equals("pre")){
                //System.out.println("a-2");
                // キャッシュにはあるが処理中の場合 一旦待機してから内容を返す
                while (cacheUrl == null || cacheUrl.equals("pre")){
                    cacheUrl = CacheAPI.getCache(TempURL.split("\\?")[0]);
                    try {
                        Thread.sleep(500L);
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
            CacheAPI.setCache(TempURL.split("\\?")[0], "pre", new Date().getTime() + 5000L);
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
                if (isCache){
                    long eTime = -1;
                    if (Pattern.compile("([nb])\\.nicovrc\\.net").matcher(ResultURL).find()){
                        eTime = new Date().getTime() + 86400000;
                    } else if (Pattern.compile("dmc\\.nico").matcher(ResultURL).find()){
                        //System.out.println("test");
                        Request request = new Request.Builder()
                                .url(TempURL)
                                .build();
                        Response response = HttpClient.newCall(request).execute();
                        if (response.body() != null) {
                            String responseHtml = response.body().string();
                            Matcher matcher = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">").matcher(responseHtml);
                            if (matcher.find()){
                                eTime = Long.parseLong(matcher.group(1)) * 1000;
                                //System.out.println(Long.parseLong(matcher.group(1)) * 1000);
                            } else {
                                eTime = 86400000;
                            }
                        }
                        response.close();
                        eTime = new Date().getTime() + eTime;
                    }

                    CacheAPI.removeCache(TempURL.split("\\?")[0]);

                    if (!isTitleGet){
                        CacheAPI.setCache(TempURL.split("\\?")[0], ResultURL, eTime);
                    }
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

                if (isCache){
                    CacheAPI.removeCache(TempURL.split("\\?")[0]);
                }
                System.out.println("["+sdf.format(new Date())+"] リクエスト (エラー) : " + RequestURL + " ---> " + e.getMessage());
            }
        } else {
            // 処理鯖が設定されている場合は処理鯖へ投げてその結果を返す
            UDPPacket packet = new UDPPacket(RequestURL, TempURL);
            packet.setGetTitle(isTitleGet);
            packet.setHTTPRequest(httpRequest);

            UDPPacket result = ServerAPI.SendServer(packet);
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
                }
                if (isCache){
                    if (!Pattern.compile("i2v\\.nicovrc\\.net").matcher(result.getResultURL()).find()){
                        CacheAPI.removeCache(TempURL.split("\\?")[0]);
                        CacheAPI.setCache(TempURL.split("\\?")[0], result.getResultURL(), Pattern.compile("([nb])\\.nicovrc.net").matcher(result.getResultURL()).find() ? new Date().getTime() + 86400000 : -1);
                    } else {
                        CacheAPI.removeCache(TempURL.split("\\?")[0]);
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
                if (isCache){
                    long eTime = -1;
                    if (Pattern.compile("([nb])\\.nicovrc\\.net").matcher(ResultURL).find()){
                        eTime = new Date().getTime() + 86400000;
                    } else if (Pattern.compile("dmc\\.nico").matcher(ResultURL).find()){
                        //System.out.println("test");
                        Request request = new Request.Builder()
                                .url(TempURL)
                                .build();
                        Response response = HttpClient.newCall(request).execute();
                        if (response.body() != null) {
                            String responseHtml = response.body().string();
                            Matcher matcher = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">").matcher(responseHtml);
                            if (matcher.find()){
                                eTime = Long.parseLong(matcher.group(1)) * 1000;
                                //System.out.println(Long.parseLong(matcher.group(1)) * 1000);
                            }
                        }
                        response.close();
                        if (eTime != -1){
                            eTime = new Date().getTime() + eTime;
                        }
                    }

                    CacheAPI.removeCache(TempURL.split("\\?")[0]);
                    if (!isTitleGet){
                        CacheAPI.setCache(TempURL.split("\\?")[0], ResultURL, eTime);
                    }
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

        Matcher referer = Pattern.compile("([Rr])eferer: (.+)").matcher(RequestHeader);
        Matcher origin = Pattern.compile("([Oo])rigin: (.+)").matcher(RequestHeader);
        Matcher access = Pattern.compile("\\?access=(.+)").matcher(RequestHeader);
        if (referer.find()) {
            jsonText = "" +
                    "{" +
                    "  \"username\": \"nico-proxy_forVRC (Ver " + ConversionAPI.getVer() + ")\"," +
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
                    "  \"username\": \"nico-proxy_forVRC (Ver " + ConversionAPI.getVer() + ")\"," +
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
                    "  \"username\": \"nico-proxy_forVRC (Ver " + ConversionAPI.getVer() + ")\"," +
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
                    "  \"username\": \"nico-proxy_forVRC (Ver "+ConversionAPI.getVer()+")\","+
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
