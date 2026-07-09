package net.nicovrc.dev;

import com.google.gson.Gson;
import net.nicovrc.dev.api.NicoVRCAPI;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.data.LogData;
import net.nicovrc.dev.data.WebhookData;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.ScanResult;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Function {
    public static final String Version = "3.5.0-beta.2";
    public static final Gson gson = new Gson();
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:152.0) Gecko/20100101 Firefox/152.0 nicovrc-net/" + Version;
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final byte[] zeroByte = new byte[0];

    public static final List<String> ProxyList = new ArrayList<>();
    public static final List<String> JP_ProxyList = new ArrayList<>();

    public static final List<NicoVRCAPI> APIList = new ArrayList<>();

    private static final Pattern HTTPVersion = Pattern.compile("HTTP/(\\d+\\.\\d+)");
    private static final Pattern HTTP = Pattern.compile("(.+) (.+) HTTP/(\\d\\.\\d)");
    private static final Pattern HTTPURI = Pattern.compile("(.+) HTTP/(\\d\\.\\d)");

    public static final ConcurrentHashMap<String, String> APIAccessLog = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, LogData> GetURLAccessLog = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, CacheData> CacheList = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, WebhookData> WebhookData = new ConcurrentHashMap<>();

    public static final String contentType_textPlain = "text/plain; charset=utf-8";
    public static final String contentType_json = "application/json; charset=utf-8";
    public static final String contentType_video_mp4 = "video/mp4";
    public static final String contentType_hls = "application/vnd.apple.mpegurl";

    public static final byte[] content_errorAPINotFound = "API Not Found".getBytes(StandardCharsets.UTF_8);
    public static final byte[] content_BadGateway = "Bad Gateway".getBytes(StandardCharsets.UTF_8);
    public static final byte[] content_NotFound = "Not Found".getBytes(StandardCharsets.UTF_8);
    public static final byte[] content_MethodNotAllowed = "Method Not Allowed".getBytes(StandardCharsets.UTF_8);
    public static final byte[] content_VideoNotFound = "Video Not Found".getBytes(StandardCharsets.UTF_8);
    public static byte[] content_errorVideo_others = null;
    public static byte[] content_errorVideo_site = null;
    public static byte[] content_errorVideo_endLive = null;

    public static final Timer mainTimer = new Timer();
    public static final Timer checkTimer = new Timer();

    public static int config_httpPort = 25252;
    public static String config_FolderPass = "";
    public static String config_user_session = null;
    public static String config_nicosid = null;
    public static String config_twitcast_ClientId = null;
    public static String config_twitcast_ClientSecret = null;
    public static boolean config_CacheToRedis = false;
    public static String DiscordWebhookURL = null;

    public static RedisClient redisClient = null;


    public static final Pattern NicoID1 = Pattern.compile("(http|https)://(live|www)\\.nicovideo\\.jp/watch/(.+)");
    public static final Pattern NicoID_short = Pattern.compile("(http|https)://www\\.nicovideo\\.jp/shorts/(.+)");
    public static final Pattern NicoID2 = Pattern.compile("(http|https)://nico\\.ms/(.+)");
    public static final Pattern NicoID3 = Pattern.compile("(http|https)://cas\\.nicovideo\\.jp/user/(.+)");
    public static final Pattern NicoID4 = Pattern.compile("^(sm\\d+|nm\\d+|am\\d+|fz\\d+|ut\\d+|dm\\d+|so\\d+|ax\\d+|ca\\d+|cd\\d+|cw\\d+|fx\\d+|ig\\d+|na\\d+|om\\d+|sd\\d+|sk\\d+|yk\\d+|yo\\d+|za\\d+|zb\\d+|zc\\d+|zd\\d+|ze\\d+|nl\\d+|ch\\d+|\\d+|lv\\d+|ss\\d+)");


    public static String getHTTPRequest(ByteBuffer buffer) {
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }

    public static String createHTTPHeader(String httpVersion, int code, String contentType, String contentEncoding, String AccessControlAllowOrigin, byte[] body, String redirectUrl,boolean isRange, long rangeStart, long rangeEnd, long rangeSize){
        StringBuilder sb_header = new StringBuilder();

        sb_header.append("HTTP/").append(httpVersion == null ? "1.1" : httpVersion);
        sb_header.append(" ").append(code).append(" ");
        switch (code) {
            case 200 -> sb_header.append("OK");
            case 206 -> sb_header.append("Partial Content");
            case 302 -> sb_header.append("Found");
            case 400 -> sb_header.append("Bad Request");
            case 403 -> sb_header.append("Forbidden");
            case 404 -> sb_header.append("Not Found");
            case 405 -> sb_header.append("Method Not Allowed");
            case 503 -> sb_header.append("Service Unavailable");
        }
        sb_header.append("\r\n");

        if (code != 302){
            if (AccessControlAllowOrigin != null){
                sb_header.append("Access-Control-Allow-Origin: ").append(AccessControlAllowOrigin).append("\r\n");
            }
            if (isRange){
                sb_header.append("Accept-Ranges: bytes\r\n");
            }
            sb_header.append("Content-Length: ").append(body.length).append("\r\n");
            if (contentEncoding != null && !contentEncoding.isEmpty()) {
                sb_header.append("Content-Encoding: ").append(contentEncoding).append("\r\n");
            }
            sb_header.append("Content-Type: ").append(contentType).append("\r\n");

            if (isRange){
                sb_header.append("Content-Ranges: ").append(rangeStart).append("-").append(rangeEnd).append("/").append(rangeSize).append("\r\n");
            }
        }

        sb_header.append("Date: ").append(new Date()).append("\r\n");

        if (code == 302 && redirectUrl != null){
            sb_header.append("Location: ").append(redirectUrl).append("\r\n");
        }

        sb_header.append("\r\n");
        String httpRequest = sb_header.toString();
        sb_header.setLength(0);
        sb_header = null;

        //System.out.println(httpRequest);
        return httpRequest;

    }

    public static byte[] createSendHTTPData(String header, byte[] body){
        if (body == null){
            return header.getBytes(StandardCharsets.UTF_8);
        }
        return concatByteArrays(header.getBytes(StandardCharsets.UTF_8), body);
    }

    public static void sendHTTPData(AsynchronousSocketChannel ch, byte[] data){
        ByteBuffer write = ByteBuffer.allocate(data.length);
        write.put(data);
        write.flip();

        ch.write(write, write, new CompletionHandler<>() {
            public void completed(Integer m, ByteBuffer bb) {
                bb.clear();
                try {
                    ch.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            }

            public void failed(Throwable e, ByteBuffer bb) {
                try {
                    ch.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            }
        });
    }

    @Deprecated
    public static void sendHTTPRequest(Socket sock, String httpVersion, int code, String contentType, String contentEncoding, String AccessControlAllowOrigin, byte[] body, boolean isHEAD, String redirectUrl) throws Exception {
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();
        String httpHeader = createHTTPHeader(httpVersion, code, contentType, contentEncoding, AccessControlAllowOrigin, body, redirectUrl, false, -1, -1, -1);

        //System.out.println(sb_header);
        //System.out.println(1);
        if (!sock.isClosed()){
            out.write(httpHeader.getBytes(StandardCharsets.UTF_8));
            if (code != 302){
                if (!isHEAD){
                    out.write(body);
                }
            }
            out.flush();
        }
        //System.out.println(2);
        out.close();
        in.close();
        sock.close();
        //System.out.println(3);

        out = null;
        in = null;

    }

    @Deprecated
    public static void sendHTTPRequest(Socket sock, String httpVersion, int code, String contentType, String contentEncoding, String AccessControlAllowOrigin, byte[] body, boolean isHEAD, long rangeStart, long rangeEnd, long rangeSize) throws Exception {
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();
        //System.out.println("!");
        String httpHeader = createHTTPHeader(httpVersion, code, contentType, contentEncoding, AccessControlAllowOrigin, body, null, true, rangeStart, rangeEnd, rangeSize);

        //System.out.println(httpHeader);
        //System.out.println(1);
        if (sock.isConnected() && !sock.isClosed()){
            out.write(httpHeader.getBytes(StandardCharsets.UTF_8));
            if (code != 302){
                if (!isHEAD){
                    out.write(body);
                }
            }
            out.flush();
        }
        //System.out.println(2);

        out.close();
        in.close();
        sock.close();
        //System.out.println(3);
        out = null;
        in = null;
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
        Matcher matcher1 = HTTP.matcher(HTTPRequest);
        Matcher matcher2 = HTTPURI.matcher(HTTPRequest);

        if (matcher1.find()) {
            uri = matcher1.group(2);
        } else if (matcher2.find()) {
            uri = matcher2.group(1);
        }
        matcher1 = null;
        matcher2 = null;

        //System.out.println("URI : "+uri);

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

    public static String getBrotliPath(){

        if (new File("/usr/share/brotli").exists()){
            return "/usr/share/brotli";
        }

        if (new File("/usr/bin/brotli").exists()){
            return "/usr/bin/brotli";
        }

        if (new File("./brotli.exe").exists()){
            return "./brotli.exe";
        }

        if (new File("./brotli").exists()){
            return "./brotli";
        }

        return "";

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
                //System.out.println(Arrays.toString(new String[]{ffmpegPass, "-loop", "1", "-i", "./error-video/" + videoId + ".png", "-i", "./error-video/out.mp3", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "copy", "-map", "0:v:0", "-map", "1:a:0", "-t", "20", "-r", "60", "./error-video/" + videoId + ".mp4"}));
                String[] command = new String[]{ffmpegPass, "-v","quiet","-loop", "1", "-i", "./error-video/" + videoId + ".png", "-i", "./error-video/out.mp3", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "copy", "-map", "0:v:0", "-map", "1:a:0", "-t", "20", "-r", "60", "./error-video/" + videoId + ".mp4"};
                final Process exec1 = runtime.exec(command);
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

    public static void addCache(String url, CacheData data){
        if (config_CacheToRedis && redisClient != null){
            String str = Base64.getEncoder().encodeToString(url.getBytes(StandardCharsets.UTF_8));
            redisClient.set("nicovrc:cachelist:" + str, gson.toJson(data), new SetParams().ex(86400));
        } else {
            CacheList.put(url, data);
        }
    }

    public static CacheData getCache(String url){
        if (config_CacheToRedis && redisClient != null){
            String str = Base64.getEncoder().encodeToString(url.getBytes(StandardCharsets.UTF_8));
            String s = redisClient.get("nicovrc:cachelist:" + str);
            if (s == null || s.isEmpty()){
                return null;
            }

            return gson.fromJson(s, CacheData.class);
        } else {
            return CacheList.get(url);
        }
    }

    public static HashMap<String, CacheData> getCacheList(){
        if (config_CacheToRedis && redisClient != null){
            final HashMap<String, CacheData> temp = new HashMap<>();
            final ScanParams params = new ScanParams();
            params.count(1000);
            params.match("nicovrc:cachelist:*");
            String cur = ScanParams.SCAN_POINTER_START;
            ScanResult<String> scanResult = null;
            List<String> result = null;
            String jsonText = null;
            String[] split = null;
            String str = null;

            boolean isEnd = false;
            while (!isEnd) {
                scanResult = redisClient.scan(cur, params);
                result = scanResult.getResult();

                //System.out.println(result.size());
                for (String key : result) {
                    jsonText = redisClient.get(key);
                    split = key.split(":");
                    str = new String(Base64.getDecoder().decode(split[split.length - 1]), StandardCharsets.UTF_8);
                    temp.put(str, Function.gson.fromJson(jsonText, CacheData.class));
                    jsonText = null;
                    split = null;
                    str = null;
                }

                cur = scanResult.getCursor();
                if (cur.equals("0")) {
                    isEnd = true;
                }
                scanResult = null;
                result.clear();
            }

            return temp;

        } else {
            return new HashMap<>(CacheList);
        }
    }

    public static void deleteCache(String url){
        if (config_CacheToRedis && redisClient != null){
            redisClient.del("nicovrc:cachelist:" + url);
        } else {
            CacheList.remove(url);
        }
    }

    public static byte[] decompressByte(byte[] content, String compressType) throws Exception {
        byte[] body = content;

        if (compressType == null || compressType.isEmpty()){
            return body;
        }

        if (compressType.toLowerCase(Locale.ROOT).equals("gzip")){

            ByteArrayInputStream stream = new ByteArrayInputStream(content);
            GZIPInputStream gis = new GZIPInputStream(stream);
            body = gis.readAllBytes();
            gis.close();
            stream.close();

        } else if (compressType.toLowerCase(Locale.ROOT).equals("br")){

            String brotliPath = Function.getBrotliPath();
            String d_file = "./text_d_"+ UUID.randomUUID().toString()+"_"+new Date().getTime()+".txt";
            String o_file = "./text_d_"+ UUID.randomUUID().toString()+"_"+new Date().getTime()+".txt.br";

            Runtime runtime = Runtime.getRuntime();
            if (!brotliPath.isEmpty()){

                FileOutputStream outputStream = new FileOutputStream(o_file);
                outputStream.write(content);
                outputStream.close();

                //final Process exec0 = runtime.exec(new String[]{brotliPath, "-9", "-o", "text.br2", "text.txt"});
                final Process exec0 = runtime.exec(new String[]{brotliPath, "-o" , d_file, "-d" , o_file});
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

                FileInputStream inputStream = new FileInputStream(d_file);
                body = inputStream.readAllBytes();
                inputStream.close();

                new File(d_file).delete();
                new File(o_file).delete();

                //System.out.println(body.length);

            }

        }
        return body;
    }

    public static byte[] compressByte(byte[] content, String compressType) throws Exception {
        compressType = compressType.toLowerCase(Locale.ROOT);

        if (compressType.equals("br") || compressType.equals("brotli")){
            String brotliPath = Function.getBrotliPath();
            String d_file = "./text_"+ UUID.randomUUID().toString()+"_"+new Date().getTime()+".txt.br";
            String o_file = "./text_"+ UUID.randomUUID().toString()+"_"+new Date().getTime()+".txt";

            Runtime runtime = Runtime.getRuntime();
            if (!brotliPath.isEmpty()) {

                FileOutputStream outputStream = new FileOutputStream(o_file);
                outputStream.write(content);
                outputStream.close();

                final Process exec0 = runtime.exec(new String[]{brotliPath, "-9", "-o", d_file, o_file});
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

                FileInputStream inputStream = new FileInputStream(d_file);
                byte[] body = inputStream.readAllBytes();
                inputStream.close();

                new File(d_file).delete();
                new File(o_file).delete();

                return body;
            }
        } else if (compressType.equals("gzip")){
            ByteArrayOutputStream compressBaos = new ByteArrayOutputStream();
            try (OutputStream gzip = new GZIPOutputStream(compressBaos)) {
                gzip.write(content);
            }

            return compressBaos.toByteArray();
        } else if (compressType.isEmpty()) {
            return content;
        }

        return null;
    }

    public static byte[] concatByteArrays(byte[]... arrays) {
        return Arrays.stream(arrays)
                .collect(ByteArrayOutputStream::new,
                        ByteArrayOutputStream::writeBytes,
                        (left, right) -> left.writeBytes(right.toByteArray()))
                .toByteArray();
    }
}
