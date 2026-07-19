package net.nicovrc.dev;

import com.google.gson.Gson;
import net.nicovrc.dev.api.NicoVRCAPI;
import net.nicovrc.dev.data.*;
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
    public static final String Version = "3.5.0";
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
    private static final ConcurrentHashMap<String, String> VideoIDList = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> CacheIDDataList = new ConcurrentHashMap<>();

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

    public final static Pattern matcher_abema = Pattern.compile("abema");

    private final static Pattern matcher_file_m3u8 = Pattern.compile("m3u8");
    private final static Pattern matcher_file_cmfa = Pattern.compile("cmfa");
    private final static Pattern matcher_file_cmfv = Pattern.compile("cmfv");
    private final static Pattern matcher_file_key = Pattern.compile("key");

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
    public static final Pattern matcher_AVProMobile = Pattern.compile("AVProMobileVideo");
    public static final Pattern matcher_FFMpeg = Pattern.compile("[U|u]ser-[A|a]gent: Lavf/");

    private static final Pattern matcher_hlsURI = Pattern.compile("(,|:)URI=\"(.+)\"");
    private static final Pattern matcher_hls_twitcasting = Pattern.compile("twitcasting\\.tv");
    private static final Pattern matcher_hls_abema = Pattern.compile("(.+)-abematv\\.akamaized\\.net");
    private static final Pattern matcher_hls_vimeo = Pattern.compile("vimeocdn\\.com");
    public static final Pattern matcher_hls_fc2Live = Pattern.compile("(.+)\\.live\\.fc2\\.com");

    private static final Pattern matcher_nico_hls_video = Pattern.compile("#EXT-X-STREAM-INF:BANDWIDTH=(\\d+),AVERAGE-BANDWIDTH=(\\d+),CODECS=\"(.+)\",RESOLUTION=(.+),FRAME-RATE=(.+),AUDIO=\"(.+)\"");
    private static final Pattern matcher_nico_hls_audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"(.+)\"");
    private static final Pattern matcher_nico_hls_audio_bitrate = Pattern.compile("audio-aac-(\\d+)kbps");
    private static final Pattern matcher_nico_hls_live_video = Pattern.compile("#EXT-X-STREAM-INF:BANDWIDTH=(\\d+),AVERAGE-BANDWIDTH=(\\d+),CODECS=\"(.+)\",RESOLUTION=(.+),FRAME-RATE=(.+),AUDIO=\"(.+)\"");
    private static final Pattern matcher_nico_hls_live_audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"(.+)\"");

    private static final Pattern matcher_abemahlsHost = Pattern.compile("//(.*)abematv\\.akamaized\\.net");

    private static final Pattern matcher_codecs = Pattern.compile(",CODECS=\"(.+)\",RESOLUTION=");

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
        return new String(buffer.array(), StandardCharsets.UTF_8).split("\\u0000\\u0000")[0];
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
            final Matcher matcher_key = matcher_file_key.matcher(line);

            final boolean ism3u8 = matcher_m3u8.find();
            final boolean iscmfv = matcher_cmfv.find();
            final boolean iscmfa = matcher_cmfa.find();
            final boolean iskey = matcher_key.find();

            String[] split = UUID.randomUUID().toString().split("-");
            String videoId = null;

            if (matcher.find()){
                String oldUrl = matcher.group(2);
                videoId = Function.getVideoID(oldUrl);

                String newUrl = http+httpHostname+"/video/"+URLEncoder.encode(cacheId, StandardCharsets.UTF_8)+"/"+getFileName(line, videoId);
                addVideoIDList(videoId, oldUrl);
                sb.append(line.replace(oldUrl, newUrl)).append("\n");
                continue;
            }

            if (line.startsWith("http")){
                videoId = getVideoID(line);
                addVideoIDList(videoId, line);
                sb.append(http).append(httpHostname).append("/video/").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("/").append(getFileName(line, videoId)).append("\n");
                continue;
            }

            if (line.startsWith("/")){
                videoId = getVideoID(http+hostname+line);
                addVideoIDList(videoId, http+hostname+line);

                if (hls_twitcas.find() && line.startsWith("/tc\\.vod\\.v2")){
                    sb.append(http).append(httpHostname).append("/video/").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("/").append(getFileName(line, videoId)).append("\n");
                    continue;
                }

                if (hls_abema.find()){
                    if (line.startsWith("/tsad")){
                        sb.append(http).append(httpHostname).append("/video/").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("/").append(getFileName(line, videoId)).append("\n");
                        continue;
                    }
                    if (line.startsWith("/preview")) {
                        sb.append(http).append(httpHostname).append("/video/").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("/").append(getFileName(line, videoId)).append("\n");
                        continue;
                    }
                }

            }

            if (hls_vimeo.find()){
                StringBuffer tempHost = new StringBuffer();
                String[] split2 = url.split("/");
                for (int i = 0; i < split2.length - 6; i++) {
                    tempHost.append(split2[i]).append("/");
                }
                line = line.replaceAll("\\.\\./\\.\\./\\.\\./\\.\\./\\.\\./", tempHost.toString());
                videoId = getVideoID(line);
                addVideoIDList(videoId, line);
                sb.append(http).append(httpHostname).append("/video/").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("/").append(getFileName(line, videoId)).append("\n");
                continue;
            }

            sb.append(line).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }

    public static String recreateHLS(String hlsText, boolean isAVProMobile){
        String[] split = hlsText.split("\n");

        String video = "";
        String audio = "";
        String audioText = "";

        long tempBandwith = -1;
        long maxVideoBandwith = -1;
        long maxAudioBandwith = -1;
        long maxLiveVideoBandwith = -1;

        int i = 0;

        for (String s : split){
            Matcher matcher_video_video = matcher_nico_hls_video.matcher(s);
            Matcher matcher_video_audio = matcher_nico_hls_audio.matcher(s);
            Matcher matcher_video_audio_bitrate = matcher_nico_hls_audio_bitrate.matcher(s);
            Matcher matcher_live_video = matcher_nico_hls_live_video.matcher(s);

            if (matcher_video_video.find()){
                tempBandwith = Long.parseLong(matcher_video_video.group(1));
                if (maxVideoBandwith <= tempBandwith){
                    maxVideoBandwith = tempBandwith;
                    video = s.replace(matcher_video_video.group(6), "audio") + "\n" + split[i + 1];
                }
                i++;
                continue;
            }

            if (matcher_video_audio.find()){
                if (matcher_video_audio_bitrate.find()){
                    tempBandwith = Long.parseLong(matcher_video_audio_bitrate.group(1));
                    if (maxAudioBandwith <= tempBandwith){
                        maxAudioBandwith = tempBandwith;
                        audio = s;
                        audioText = matcher_video_audio_bitrate.group(0);
                    }
                }
                i++;
                continue;
            }

            if (matcher_live_video.find()){
                tempBandwith = Long.parseLong(matcher_video_video.group(1));
                if (maxLiveVideoBandwith <= tempBandwith){
                    maxLiveVideoBandwith = tempBandwith;
                    video = s.replace(matcher_live_video.group(6), "audio") + "\n" + split[i + 1];
                }
                i++;
                continue;
            }
            i++;
        }

        if (video.isEmpty() || audio.isEmpty()){
            return hlsText;
        }

        hlsText = "#EXTM3U\n#EXT-X-VERSION:6\n#EXT-X-INDEPENDENT-SEGMENTS\n#audio#\n#video#";
        hlsText = hlsText.replace("#video#", video.replace("audio", audioText));
        hlsText = hlsText.replace("#audio#", audio);

        //System.out.print("----");
        //System.out.println(hlsText);
        //System.out.print("----\n\n");

        Matcher matcher = matcher_codecs.matcher(hlsText);
        if (isAVProMobile && matcher.find()){
            hlsText = hlsText.replaceAll(matcher.group(0), ",RESOLUTION=");
        }

        return hlsText;
    }


    public static String fixAbemaHLS(String hlsText, String originURL, String http, String httpHostname, String cacheId){
        String[] split = hlsText.split("\n");
        StringBuffer sb = new StringBuffer();

        for (String s : split) {
            final Matcher matcher_m3u8 = matcher_file_m3u8.matcher(s);
            final Matcher matcher_cmfv = matcher_file_cmfv.matcher(s);
            final Matcher matcher_cmfa = matcher_file_cmfa.matcher(s);
            final Matcher matcher_key = matcher_file_key.matcher(s);

            final boolean ism3u8 = matcher_m3u8.find();
            final boolean iscmfv = matcher_cmfv.find();
            final boolean iscmfa = matcher_cmfa.find();
            final boolean iskey = matcher_key.find();

            final Matcher matcher = matcher_abemahlsHost.matcher(originURL);

            String[] uuid = UUID.randomUUID().toString().split("-");
            String videoId = null;

            if (s.startsWith("#EXT-X-KEY:METHOD=AES-128")){
                sb.append(s.replaceFirst("\\.ts", ".key")).append('\n');
                continue;
            }

            if (s.startsWith("http://") || s.startsWith("https://")){
                sb.append(s).append("\n");
                continue;
            }

            if (!s.startsWith("180/") && !s.startsWith("240/") && !s.startsWith("/preview") && !s.startsWith("/tsad") ){
                sb.append(s).append("\n");
                continue;
            }

            if (s.startsWith("/preview") && matcher.find()){
                String url = "https://"+matcher.group(1)+"abematv.akamaized.net"+s;
                videoId = getVideoID(url);
                addVideoIDList(videoId, url);
                sb.append(http).append(httpHostname).append("/video/").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("/").append(getFileName(url, videoId)).append("\n");
                continue;
            }

            if (matcher.find()) {
                String url = originURL.replaceFirst("playlist\\.m3u8", "")+s;
                videoId = getVideoID(url);
                addVideoIDList(videoId, url);
                sb.append(http).append(httpHostname).append("/video/").append(URLEncoder.encode(cacheId, StandardCharsets.UTF_8)).append("/").append(getFileName(url, videoId)).append("\n");
                continue;
            }

            sb.append(s).append("\n");
        }
        //System.out.println(sb.toString());

        return sb.toString();
    }

    public static String getFileName(String name, String videoId){

        final Matcher matcher_m3u8 = matcher_file_m3u8.matcher(name);
        final Matcher matcher_cmfv = matcher_file_cmfv.matcher(name);
        final Matcher matcher_cmfa = matcher_file_cmfa.matcher(name);
        final Matcher matcher_key = matcher_file_key.matcher(name);

        final boolean ism3u8 = matcher_m3u8.find();
        final boolean iscmfv = matcher_cmfv.find();
        final boolean iscmfa = matcher_cmfa.find();
        final boolean iskey = matcher_key.find();

        String type = videoId+".ts";
        if (ism3u8){
            type = videoId+".m3u8";
        } else if (iscmfv){
            type = videoId+".cmfv";
        } else if (iscmfa){
            type = videoId+".cmfa";
        } else if (iskey){
            type = videoId+".key";
        }

        return type;
    }

    public static void addVideoIDList(String videoId, String url) {
        if (config_CacheToRedis && redisClient != null) {
            String str = Base64.getEncoder().encodeToString(videoId.getBytes(StandardCharsets.UTF_8));
            redisClient.set("nicovrc:cachelist2:" + str, url, new SetParams().ex(86400));
            return;
        }
        VideoIDList.put(videoId, url);
    }

    public static String getVideoIDListData(String videoId) {
        if (config_CacheToRedis && redisClient != null) {
            String str = Base64.getEncoder().encodeToString(videoId.getBytes(StandardCharsets.UTF_8));
            return redisClient.get("nicovrc:cachelist2:" + str);
        }
        return VideoIDList.get(videoId);
    }
    public static String getVideoID(String url) {
        if (config_CacheToRedis && redisClient != null) {
            ScanParams params = new ScanParams();
            params.count(1000);
            params.match("nicovrc:cachelist2:*");
            String cur = ScanParams.SCAN_POINTER_START;

            boolean isEnd = false;
            long count = 0;

            String resultID = null;
            while (!isEnd) {
                ScanResult<String> scanResult = redisClient.scan(cur, params);
                List<String> result = scanResult.getResult();

                for (String key : result) {
                    String s = redisClient.get(key);
                    if (s.equals(url)) {
                        isEnd = true;
                        String[] split = key.split(":");
                        resultID = new String(Base64.getDecoder().decode(split[split.length - 1].getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
                        break;
                    }
                }

                if (isEnd) {
                    break;
                }

                cur = scanResult.getCursor();
                if (cur.equals("0")) {
                    isEnd = true;
                }
            }

            if (resultID == null) {
                String[] split = UUID.randomUUID().toString().split("-");
                resultID = split[0]+split[1];
            }

            return resultID;
        }

        final String[] d = {null};
        VideoIDList.forEach((id, cacheUrl)->{
            if (d[0] != null) {
                return;
            }

            if (cacheUrl.equals(url)) {
                d[0] = id;
            }
        });

        String[] split = UUID.randomUUID().toString().split("-");
        if (d[0] == null) {
            return split[0]+split[1];
        }
        return d[0];
    }

    public static void addCacheIDDataList(String cacheId, String url) {
        if (config_CacheToRedis && redisClient != null){
            String str = Base64.getEncoder().encodeToString(cacheId.getBytes(StandardCharsets.UTF_8));
            redisClient.set("nicovrc:cachelist3:" + str, url, new SetParams().ex(86400));
            return;
        }
        CacheIDDataList.put(cacheId, url);
    }

    public static String getCacheIDDataListData(String cacheId) {
        if (config_CacheToRedis && redisClient != null){
            String str = Base64.getEncoder().encodeToString(cacheId.getBytes(StandardCharsets.UTF_8));
            return redisClient.get("nicovrc:cachelist3:" + str);
        }
        return CacheIDDataList.get(cacheId);
    }
}
