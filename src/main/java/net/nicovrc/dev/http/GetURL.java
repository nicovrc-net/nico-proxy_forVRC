package net.nicovrc.dev.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.NicoNicoVideo;
import net.nicovrc.dev.Service.ServiceAPI;
import net.nicovrc.dev.Service.ServiceList;
import net.nicovrc.dev.api.NicoVRCAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class GetURL implements Runnable, NicoVRCHTTP {

    private Socket sock = null;
    private String URL = null;
    private String httpRequest = null;

    private final Gson gson = Function.gson;
    private final List<ServiceAPI> list = ServiceList.getServiceList();

    @Override
    public void run() {

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

            String json = null;
            String ServiceName = null;
            if (api != null){
                api.Set("{\"URL\":\""+URL+"\"}");
                json = api.Get();
                ServiceName = api.getServiceName();
            }

            //System.out.println(json);
            byte[] content = new byte[0];
            if (json != null){
                JsonElement element = gson.fromJson(json, JsonElement.class);
                if (element.getAsJsonObject().has("ErrorMessage")) {
                    String errorMessage = element.getAsJsonObject().get("ErrorMessage").getAsString();

                    try {
                        MessageDigest sha3_256 = MessageDigest.getInstance("SHA3-256");
                        byte[] sha3_256_result = sha3_256.digest(errorMessage.getBytes(StandardCharsets.UTF_8));
                        String str = new String(Base64.getEncoder().encode(sha3_256_result), StandardCharsets.UTF_8);
                        String videoId = str.replaceAll("\\\\", "").replaceAll("\\+", "").replaceAll("/", "").substring(0, 20);

                        File file = new File("./error-video/" + videoId + ".mp4");
                        if (file.exists()){
                            FileInputStream stream = new FileInputStream(file);
                            content = stream.readAllBytes();
                            stream.close();
                            stream = null;
                        } else {
                            //
                            try (HttpClient client = HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NORMAL)
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .build()) {

                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(new URI("https://nicovrc.net/v3-video/error.php?msg=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)))
                                        .headers("User-Agent", Function.UserAgent)
                                        .GET()
                                        .build();
                                HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                                FileOutputStream stream = new FileOutputStream("./error-video/" + videoId + ".png");
                                stream.write(send.body());
                                stream.close();
                                stream = null;
                                send = null;

                                final String ffmpegPass;
                                if (new File("/bin/ffmpeg").exists()){
                                    ffmpegPass = "/bin/ffmpeg";
                                } else if (new File("/usr/bin/ffmpeg").exists()){
                                    ffmpegPass = "/usr/bin/ffmpeg";
                                } else if (new File("/usr/local/bin/ffmpeg").exists()){
                                    ffmpegPass = "/usr/local/bin/ffmpeg";
                                } else if (new File("./ffmpeg").exists()){
                                    ffmpegPass = "./ffmpeg";
                                } else if (new File("./ffmpeg.exe").exists()){
                                    ffmpegPass = "./ffmpeg.exe";
                                } else if (new File("C:\\Windows\\System32\\ffmpeg.exe").exists()){
                                    ffmpegPass = "C:\\Windows\\System32\\ffmpeg.exe";
                                } else {
                                    ffmpegPass = "";
                                }
                                Runtime runtime = Runtime.getRuntime();
                                if (!new File("./error-video/out.mp3").exists()){
                                    final Process exec0 = runtime.exec(new String[]{ffmpegPass, "-f","lavfi","-i","anullsrc=r=44100:cl=mono","-t","5","-aq","1","-c:a","libmp3lame","./error-video/out.mp3"});
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
                                final Process exec1 = runtime.exec(new String[]{ffmpegPass, "-loop","1","-i","./error-video/"+videoId+".png","-i","./error-video/out.mp3","-c:v","libx264","-pix_fmt","yuv420p","-c:a","copy","-map","0:v:0","-map","1:a:0","-t","20","-r","60","./error-video/" + videoId + ".mp4"});
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
                                /*byte[] read = null;
                                try {
                                    read = exec1.getInputStream().readAllBytes();
                                } catch (Exception e){
                                    read = new byte[0];
                                }
                                if (read.length == 0) {
                                    try {
                                        read = exec1.getErrorStream().readAllBytes();
                                    } catch (Exception e){
                                        read = new byte[0];
                                    }
                                }
                                String infoMessage = new String(read, StandardCharsets.UTF_8);
                                System.out.println(infoMessage);*/
                                //System.out.println(ffmpegPass);
                                file = new File("./error-video/" + videoId + ".jpg");
                                file.delete();

                                file = new File("./error-video/" + videoId + ".mp4");
                                if (file.exists()){
                                    FileInputStream stream1 = new FileInputStream(file);
                                    content = stream1.readAllBytes();
                                    stream1.close();
                                    stream1 = null;
                                }

                            } catch (Exception e){
                                e.printStackTrace();
                                file = new File("./error-video/error_000.mp4");
                                if (file.exists()){
                                    FileInputStream stream = new FileInputStream(file);
                                    content = stream.readAllBytes();
                                    stream.close();
                                    stream = null;
                                }
                            }
                        }
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));

                        file = null;
                        sha3_256_result = null;
                        sha3_256 = null;
                        str = null;
                        errorMessage = null;
                        element = null;
                        content = null;
                        return;
                    } catch (Exception e) {
                        File file = new File("./error-video/error_000.mp4");
                        if (file.exists()){
                            FileInputStream stream = new FileInputStream(file);
                            content = stream.readAllBytes();
                            stream.close();
                            stream = null;
                        }

                        errorMessage = null;
                        element = null;
                        Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
                        content = null;
                        return;
                    }
                } else if (ServiceName.equals("ニコニコ")) {
                    NicoNicoVideo result = gson.fromJson(json, NicoNicoVideo.class);
                    if (result != null){

                    }
                }

                //Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "text/plain; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8), method != null && method.equals("HEAD"));
            } else {
                File file = new File("./error-video/error_404.mp4");
                if (file.exists()){
                    FileInputStream stream = new FileInputStream(file);
                    content = stream.readAllBytes();
                    stream.close();
                    stream = null;
                }
                //System.out.println(content.length);
                Function.sendHTTPRequest(sock, Function.getHTTPVersion(httpRequest), 200, "video/mp4", content, method != null && method.equals("HEAD"));
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
