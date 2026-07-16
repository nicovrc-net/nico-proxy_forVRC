package net.nicovrc.dev;

import com.google.gson.Gson;
import net.nicovrc.dev.api.NicoVRCAPI;
import net.nicovrc.dev.data.CacheData;
import net.nicovrc.dev.data.HttpHeader;
import net.nicovrc.dev.data.LogData;
import net.nicovrc.dev.data.WebhookData;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.ScanResult;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Function {
    public static final String Version = "3.5.0-beta.3";
    public static final Gson gson = new Gson();
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:152.0) Gecko/20100101 Firefox/152.0 nicovrc-net/" + Version;
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final byte[] zeroByte = new byte[0];

    public static final List<String> ProxyList = new ArrayList<>();
    public static final List<String> JP_ProxyList = new ArrayList<>();

    public static final List<NicoVRCAPI> APIList = new ArrayList<>();

    private static final Pattern HTTPVersion = Pattern.compile("HTTP/(\\d+\\.\\d+)");
    private static final Pattern HTTP = Pattern.compile("(.+) (.+) HTTP/(\\d\\.\\d)");
    private static final Pattern HTTPURI1 = Pattern.compile("(.+) HTTP/(\\d\\.\\d)");
    private static final Pattern HTTPURI2 = Pattern.compile("(.+) HTTP/");
    private static final Pattern matcher_host = Pattern.compile("[H|h]ost: (.+)");

    public static final ConcurrentHashMap<String, String> APIAccessLog = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, LogData> GetURLAccessLog = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, CacheData> CacheList = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, WebhookData> WebhookData = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Date> CacheWaitList = new ConcurrentHashMap<>();

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

    public static final Pattern matcher_niconico = Pattern.compile("nicovideo\\.jp");
    public static final Pattern matcher_VLC = Pattern.compile("(VLC/(.+) LibVLC/(.+)|LibVLC)");
    public static final Pattern matcher_AVPro = Pattern.compile("(NSPlayer|AVPro|AppleCoreMedia)");
    public static final Pattern matcher_FFMpeg = Pattern.compile("[U|u]ser-[A|a]gent: Lavf/");

    private static final Pattern matcher_hlsURI = Pattern.compile("(,|:)URI=\"(.+)\"");
    private static final Pattern matcher_hls_twitcasting = Pattern.compile("twitcasting\\.tv");
    private static final Pattern matcher_hls_abema = Pattern.compile("(.+)-abematv\\.akamaized\\.net");
    private static final Pattern matcher_hls_vimeo = Pattern.compile("vimeocdn\\.com");
    public static final Pattern matcher_hls_fc2Live = Pattern.compile("(.+)\\.live\\.fc2\\.com");

    public static boolean isFoundFile(String filePass) {
        Path path = Paths.get(filePass);
        return Files.exists(path);
    }

    public static boolean isFoundFolder(String folderPass) {
        Path path = Paths.get(folderPass);
        return Files.exists(path);
    }

    public static boolean createFolder(String filePass) {
        if (!isFoundFile(filePass)) {
            try {
                Files.createDirectory(Path.of(filePass));
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public static String getFileByText(String filePass, Charset charset) {

        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }

        byte[] binary = getFileByBinary(filePass);
        if (binary == null) {
            return null;
        }

        return new String(binary, charset);
    }

    public static byte[] getFileByBinary(String filePass){
        final Path path = Path.of(filePass);
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean writeFile(String filePass, String content, Charset charset) {
        return writeFile(filePass, content.getBytes(charset));
    }

    public static boolean writeFile(String filePass, byte[] content) {
        Path path = Paths.get(filePass);

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
            out.write(content);
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteFile(String filePass) {
        Path path = Paths.get(filePass);
        try {
            Files.delete(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getHTTPRequest(ByteBuffer buffer) {
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }

    public static byte[] createSendHttpData(HttpHeader header, byte[] body){
        if (body == null){
            return header.toString().getBytes(StandardCharsets.UTF_8);
        }
        return concatByteArrays(header.toString().getBytes(StandardCharsets.UTF_8), body);
    }

    public static void sendHttpData(AsynchronousSocketChannel ch, byte[] data){
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

    public static void sendHttpData(AsynchronousSocketChannel ch, HttpHeader header){
        sendHttpData(ch, createSendHttpData(header, header.getHttpBody()));
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
        Matcher matcher2 = HTTPURI1.matcher(HTTPRequest);
        Matcher matcher3 = HTTPURI2.matcher(HTTPRequest);

        if (matcher1.find()) {
            uri = matcher1.group(2);
        } else if (matcher2.find()) {
            uri = matcher2.group(1);
        } else if (matcher3.find()) {
            uri = matcher3.group(1);
        }
        matcher1 = null;
        matcher2 = null;
        matcher3 = null;

        //System.out.println("URI : "+uri);

        return uri;
    }

    public static String getHost(String HTTPRequest){
        Matcher matcher = matcher_host.matcher(HTTPRequest);
        if (matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

    public static String getFFmpegPath(){

        final String ffmpegPass;
        if (isFoundFile("./ffmpeg")){
            ffmpegPass = "./ffmpeg";
        } else if (isFoundFile("./ffmpeg.exe")){
            ffmpegPass = "./ffmpeg.exe";
        } else if (isFoundFile("/bin/ffmpeg")){
            ffmpegPass = "/bin/ffmpeg";
        } else if (isFoundFile("/usr/bin/ffmpeg")){
            ffmpegPass = "/usr/bin/ffmpeg";
        } else if (isFoundFile("/usr/local/bin/ffmpeg")){
            ffmpegPass = "/usr/local/bin/ffmpeg";
        } else if (isFoundFile("C:\\Windows\\System32\\ffmpeg.exe")){
            ffmpegPass = "C:\\Windows\\System32\\ffmpeg.exe";
        } else {
            ffmpegPass = "";
        }

        return ffmpegPass;

    }

    public static String getBrotliPath(){

        if (isFoundFile("/usr/share/brotli")){
            return "/usr/share/brotli";
        }

        if (isFoundFile("/usr/bin/brotli")){
            return "/usr/bin/brotli";
        }

        if (isFoundFile("./brotli.exe")){
            return "./brotli.exe";
        }

        if (isFoundFile("./brotli")){
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

            if (isFoundFile("./error-video/" + videoId + ".mp4")) {
                content = getFileByBinary("./error-video/" + videoId + ".mp4");
            } else {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://nicovrc.net/v3-video/error.php?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8)))
                        .headers("User-Agent", Function.UserAgent)
                        .GET()
                        .build();
                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                writeFile("./error-video/" + videoId + ".png", send.body());

                Runtime runtime = Runtime.getRuntime();
                String ffmpegPass = Function.getFFmpegPath();
                if (!isFoundFile("./error-video/out.mp3")) {
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
                deleteFile("./error-video/" + videoId + ".png");

                if (isFoundFile("./error-video/" + videoId + ".mp4")) {
                    content = getFileByBinary("./error-video/" + videoId + ".mp4");
                }
            }

            return content;
        } catch (Exception e) {
            return content_errorVideo_others;
        }
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

                if (isFoundFile(o_file)){
                    deleteFile(o_file);
                }
                writeFile(o_file, body);

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

                if (isFoundFile(d_file)){
                    body = getFileByBinary(d_file);
                }

                deleteFile(o_file);
                deleteFile(d_file);

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

                if (isFoundFile(o_file)){
                    deleteFile(o_file);
                }
                writeFile(o_file, content);

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

                if (isFoundFile(d_file)){
                    deleteFile(d_file);
                    deleteFile(o_file);
                    return getFileByBinary(d_file);
                }

                return null;
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

    final static Pattern matcher_file_m3u8 = Pattern.compile("m3u8");
    final static Pattern matcher_file_cmfa = Pattern.compile("cmfa");
    final static Pattern matcher_file_cmfv = Pattern.compile("cmfv");
    final static Pattern matcher_file_keys = Pattern.compile("keys");
    public static byte[] replaceHLS(byte[] hls_data, String http, String httpHostname, String cacheId, String hostname, String url) {
        final String hlsText = new String(hls_data, StandardCharsets.UTF_8);
        final Matcher hls_twitcas = matcher_hls_twitcasting.matcher(url);
        final Matcher hls_abema = matcher_hls_abema.matcher(url);
        final Matcher hls_vimeo = matcher_hls_vimeo.matcher(url);


        StringBuffer sb = new StringBuffer();
        for (String line : hlsText.split("\n")){
            final Matcher matcher = matcher_hlsURI.matcher(line);
            final Matcher matcher_m3u8 = matcher_file_m3u8.matcher(line);
            final Matcher matcher_cmfv = matcher_file_cmfv.matcher(line);
            final Matcher matcher_cmfa = matcher_file_cmfa.matcher(line);
            final Matcher matcher_keys = matcher_file_keys.matcher(line);

            final boolean ism3u8 = matcher_m3u8.find();
            final boolean iscmfv = matcher_cmfv.find();
            final boolean iscmfa = matcher_cmfa.find();
            final boolean iskeys = matcher_keys.find();

            String type = "dummy.ts";
            if (ism3u8){
                type = "dummy.m3u8";
            } else if (iscmfv){
                type = "dummy.cmfv";
            } else if (iscmfa){
                type = "dummy.cmfa";
            } else if (iskeys){
                type = "dummy.keys";
            }

            if (matcher.find()){
                String oldUrl = matcher.group(2);
                String newUrl = http+httpHostname+"/video/"+type+"?cacheId="+URLEncoder.encode(cacheId, StandardCharsets.UTF_8)+"&url="+URLEncoder.encode(oldUrl, StandardCharsets.UTF_8);
                sb.append(line.replace(oldUrl, newUrl)).append("\n");
                continue;
            }

            if (line.startsWith("http")){
                sb.append(http).append(httpHostname).append("/video/").append(type).append("?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(line, StandardCharsets.UTF_8)).append("\n");
                continue;
            }

            if (line.startsWith("/")){
                String hlsUrl = "https://"+hostname+line;

                if (hls_twitcas.find() && line.startsWith("/tc\\.vod\\.v2")){
                    sb.append(http).append(httpHostname).append("/video/").append(type).append("?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(hlsUrl, StandardCharsets.UTF_8)).append("\n");
                    continue;
                }

                if (hls_abema.find()){
                    if (line.startsWith("/tsad")){
                        sb.append(http).append(httpHostname).append("/video/").append(type).append("?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(hlsUrl, StandardCharsets.UTF_8)).append("\n");
                        continue;
                    }
                    if (line.startsWith("/preview")) {
                        sb.append(http).append(httpHostname).append("/video/").append(type).append("?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(hlsUrl, StandardCharsets.UTF_8)).append("\n");
                        continue;
                    }
                }

            }

            if (hls_vimeo.find()){
                StringBuffer tempHost = new StringBuffer();
                String[] split = url.split("/");
                for (int i = 0; i < split.length - 6; i++) {
                    tempHost.append(split[i]).append("/");
                }
                line = line.replaceAll("\\.\\./\\.\\./\\.\\./\\.\\./\\.\\./", tempHost.toString());
                sb.append(http).append(httpHostname).append("/video/").append(ism3u8 ? "dummy.m3u8" : "").append("?cacheId=").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("&url=").append(URLEncoder.encode(line, StandardCharsets.UTF_8)).append("\n");
                continue;
            }

            sb.append(line).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }
}
