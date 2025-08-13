package net.nicovrc.dev;

import com.google.gson.Gson;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.data.LogData;
import net.nicovrc.dev.data.WebhookData;
import net.nicovrc.dev.http.NicoVRCHTTP;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Function {
    public static final String Version = "3.0.0-rc.9";
    public static final Gson gson = new Gson();
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:141.0) Gecko/20100101 Firefox/141.0 nicovrc-net/" + Version;
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final byte[] zeroByte = new byte[0];

    public static final List<String> ProxyList = new ArrayList<>();
    public static final List<String> JP_ProxyList = new ArrayList<>();

    private static final Pattern HTTPVersion = Pattern.compile("HTTP/(\\d+\\.\\d+)");
    private static final Pattern HTTP = Pattern.compile("(CONNECT|DELETE|GET|HEAD|OPTIONS|PATCH|POST|PUT|TRACE) (.+) HTTP/(\\d\\.\\d)");

    public static final ConcurrentHashMap<String, String> APIAccessLog = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, LogData> GetURLAccessLog = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<String, CacheData> CacheList = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, WebhookData> WebhookData = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<String, Long> tempCacheList = new ConcurrentHashMap<>();
    public static final Timer tempCacheCheckTimer = new Timer();

    public static String getHTTPRequest(Socket sock) throws Exception{
        //System.out.println("debug 1");
        InputStream in = sock.getInputStream();
        StringBuilder sb = new StringBuilder();
        int readMaxsize = 2048;
        byte[] data = new byte[readMaxsize];
        int readSize = in.read(data);

        if (readSize <= 0) {
            data = null;
            sb = null;
            in = null;
            return null;
        }
        //System.out.println("debug 2");
        data = Arrays.copyOf(data, readSize);
        String temp = new String(data, StandardCharsets.UTF_8);
        sb.append(temp);
        temp = null;

        if (readSize == readMaxsize){
            data = new byte[readMaxsize];
            readSize = in.read(data);
            boolean isLoop = true;
            while (readSize >= 0){
                //System.out.println(readSize);
                data = Arrays.copyOf(data, readSize);
                temp = new String(data, StandardCharsets.UTF_8);
                sb.append(temp);

                data = null;
                temp = null;

                if (readSize < readMaxsize){
                    isLoop = false;
                }

                if (!isLoop){
                    break;
                }

                data = new byte[readMaxsize];
                readSize = in.read(data);
                if (readSize < readMaxsize){
                    isLoop = false;
                }
            }
        }

        data = null;
        String httpRequest = sb.toString();
        sb.setLength(0);
        sb = null;
        in = null;
        //System.out.println("debug 3");
        //System.gc();
        return httpRequest;
    }

    public static void sendHTTPRequest(Socket sock, String httpVersion, int code, String contentType, byte[] body, boolean isHEAD) throws Exception{
        sendHTTPRequest(sock, httpVersion, code, contentType, null, body, isHEAD);
    }

    public static void sendHTTPRequest(Socket sock, String httpVersion, int code, String contentType, String AccessControlAllowOrigin, byte[] body, boolean isHEAD) throws Exception {
        OutputStream out = sock.getOutputStream();
        StringBuilder sb_header = new StringBuilder();

        sb_header.append("HTTP/").append(httpVersion == null ? "1.1" : httpVersion);
        sb_header.append(" ").append(code).append(" ");
        switch (code) {
            case 200 -> sb_header.append("OK");
            case 302 -> sb_header.append("Found");
            case 400 -> sb_header.append("Bad Request");
            case 403 -> sb_header.append("Forbidden");
            case 404 -> sb_header.append("Not Found");
            case 405 -> sb_header.append("Method Not Allowed");
        }
        sb_header.append("\r\n");
        if (AccessControlAllowOrigin != null){
            sb_header.append("Access-Control-Allow-Origin: ").append(AccessControlAllowOrigin).append("\r\n");
        }
        sb_header.append("Content-Length: ").append(body.length).append("\r\n");
        sb_header.append("Content-Type: ").append(contentType).append("\r\n");

        sb_header.append("Date: ").append(new Date()).append("\r\n");

        sb_header.append("\r\n");

        //System.out.println(sb_header);
        out.write(sb_header.toString().getBytes(StandardCharsets.UTF_8));
        if (!isHEAD){
            out.write(body);
        }
        out.flush();

        out = null;
        sb_header.setLength(0);
        sb_header = null;

    }

    public static String getHTTPVersion(String HTTPRequest){
        Matcher matcher = HTTPVersion.matcher(HTTPRequest);

        if (matcher.find()){
            String group = matcher.group(1);
            matcher = null;
            return group;
        }
        matcher = null;
        return null;

    }

    public static String getMethod(String HTTPRequest){
        Matcher matcher = HTTP.matcher(HTTPRequest);
        if (matcher.find()){
            return matcher.group(1);
        }

        return null;
    }

    public static String getURI(String HTTPRequest){
        String uri = null;
        Matcher matcher = HTTP.matcher(HTTPRequest);

        if (!matcher.find()){
            matcher = null;
        } else {
            uri = matcher.group(2);
            matcher = null;
        }

        return uri;
    }

    public static String getFFmpegPath(){

        final String ffmpegPass;
        if (new File("./ffmpeg").exists()){
            ffmpegPass = "./ffmpeg";
        } else if (new File("./ffmpeg.exe").exists()){
            ffmpegPass = "./ffmpeg.exe";
        } else if (new File("/bin/ffmpeg").exists()){
            ffmpegPass = "/bin/ffmpeg";
        } else if (new File("/usr/bin/ffmpeg").exists()){
            ffmpegPass = "/usr/bin/ffmpeg";
        } else if (new File("/usr/local/bin/ffmpeg").exists()){
            ffmpegPass = "/usr/local/bin/ffmpeg";
        } else if (new File("C:\\Windows\\System32\\ffmpeg.exe").exists()){
            ffmpegPass = "C:\\Windows\\System32\\ffmpeg.exe";
        } else {
            ffmpegPass = "";
        }

        return ffmpegPass;

    }

    public static byte[] getErrorMessageVideo(HttpClient client, String message){

        byte[] content = null;

        try {
            MessageDigest sha3_256 = MessageDigest.getInstance("SHA3-256");
            byte[] sha3_256_result = sha3_256.digest(message.getBytes(StandardCharsets.UTF_8));
            String str = new String(Base64.getEncoder().encode(sha3_256_result), StandardCharsets.UTF_8);
            String videoId = str.replaceAll("\\\\", "").replaceAll("\\+", "").replaceAll("/", "").substring(0, 20);

            File file = new File("./error-video/" + videoId + ".mp4");
            if (file.exists()) {
                FileInputStream stream = new FileInputStream(file);
                content = stream.readAllBytes();
                stream.close();
                stream = null;
            } else {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://nicovrc.net/v3-video/error.php?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8)))
                        .headers("User-Agent", Function.UserAgent)
                        .GET()
                        .build();
                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                FileOutputStream stream = new FileOutputStream("./error-video/" + videoId + ".png");
                stream.write(send.body());
                stream.close();
                stream = null;
                send = null;


                Runtime runtime = Runtime.getRuntime();
                String ffmpegPass = Function.getFFmpegPath();
                if (!new File("./error-video/out.mp3").exists()) {
                    final Process exec0 = runtime.exec(new String[]{ffmpegPass, "-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono", "-t", "5", "-aq", "1", "-c:a", "libmp3lame", "./error-video/out.mp3"});
                    Thread.ofVirtual().start(() -> {
                        try {
                            Thread.sleep(5000L);
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }

                        if (exec0.isAlive()) {
                            exec0.destroy();
                        }
                    });
                    exec0.waitFor();
                }
                final Process exec1 = runtime.exec(new String[]{ffmpegPass, "-loop", "1", "-i", "./error-video/" + videoId + ".png", "-i", "./error-video/out.mp3", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "copy", "-map", "0:v:0", "-map", "1:a:0", "-t", "20", "-r", "60", "./error-video/" + videoId + ".mp4"});
                Thread.ofVirtual().start(() -> {
                    try {
                        Thread.sleep(5000L);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    if (exec1.isAlive()) {
                        exec1.destroy();
                    }
                });
                exec1.waitFor();
                //System.out.println(ffmpegPass);
                file = new File("./error-video/" + videoId + ".jpg");
                file.delete();

                file = new File("./error-video/" + videoId + ".mp4");
                if (file.exists()) {
                    FileInputStream stream1 = new FileInputStream(file);
                    content = stream1.readAllBytes();
                    stream1.close();
                    stream1 = null;
                }
            }

            return content;
        } catch (Exception e) {
            try {
                // e.printStackTrace();
                File file = new File("./error-video/error_000.mp4");
                FileInputStream stream = new FileInputStream(file);
                content = stream.readAllBytes();
                stream.close();
                stream = null;
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }

        return content;
    }
}
