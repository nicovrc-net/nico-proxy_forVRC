package net.nicovrc.dev;

import com.google.gson.Gson;
import net.nicovrc.dev.data.Config;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Function {

    public static String Version = "4.0.0-alpha.1";
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:146.0) Gecko/20100101 Firefox/146.0 nicovrc-net/"+Version;
    public static final Gson gson = new Gson();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final byte[] zeroByte = new byte[0];
    public static final byte[] err400__2_0 = ("HTTP/2.0 400 Bad Request\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: "+("Bad Request".getBytes(StandardCharsets.UTF_8).length)+"\r\nContent-Type: text/plain\r\nDate: "+new Date()+"\r\n\r\nBad Request").getBytes(StandardCharsets.UTF_8);
    public static final byte[] err400__1_1 = ("HTTP/1.1 400 Bad Request\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: "+("Bad Request".getBytes(StandardCharsets.UTF_8).length)+"\r\nContent-Type: text/plain\r\nDate: "+new Date()+"\r\n\r\nBad Request").getBytes(StandardCharsets.UTF_8);
    public static final byte[] err400__1_0 = ("HTTP/1.0 400 Bad Request\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: "+("Bad Request".getBytes(StandardCharsets.UTF_8).length)+"\r\nContent-Type: text/plain\r\nDate: "+new Date()+"\r\n\r\nBad Request").getBytes(StandardCharsets.UTF_8);
    public static final byte[] err400__2_0_head = ("HTTP/2.0 400 Bad Request\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: "+("Bad Request".getBytes(StandardCharsets.UTF_8).length)+"\r\nContent-Type: text/plain\r\nDate: "+new Date()+"\r\n\r\n").getBytes(StandardCharsets.UTF_8);
    public static final byte[] err400__1_1_head = ("HTTP/1.1 400 Bad Request\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: "+("Bad Request".getBytes(StandardCharsets.UTF_8).length)+"\r\nContent-Type: text/plain\r\nDate: "+new Date()+"\r\n\r\n").getBytes(StandardCharsets.UTF_8);
    public static final byte[] err400__1_0_head = ("HTTP/1.0 400 Bad Request\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: "+("Bad Request".getBytes(StandardCharsets.UTF_8).length)+"\r\nContent-Type: text/plain\r\nDate: "+new Date()+"\r\n\r\n").getBytes(StandardCharsets.UTF_8);



    public static final Config config = new Config();

    public static final List<String> ProxyList = new ArrayList<>();
    public static final List<String> JP_ProxyList = new ArrayList<>();

    private static final Pattern HTTPVersion = Pattern.compile("HTTP/(\\d+\\.\\d+)");
    private static final Pattern HTTP = Pattern.compile("(CONNECT|DELETE|GET|HEAD|OPTIONS|PATCH|POST|PUT|TRACE) (.+) HTTP/(\\d\\.\\d)");


    private static HttpClient client = null;


    public static HttpClient getClient() {
        return client;
    }

    public static void setClient(HttpClient client) {
        Function.client = client;
    }

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

    public static String getMethod(String HTTPRequest){
        Matcher matcher = HTTP.matcher(HTTPRequest);
        if (matcher.find()){
            return matcher.group(1);
        }

        return null;
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

    public static void sendHTTPRequest(Socket sock, String httpVersion, int code, String contentType, String contentEncoding, String AccessControlAllowOrigin, byte[] body, boolean isHEAD) throws Exception {
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
        if (contentEncoding != null && !contentEncoding.isEmpty()) {
            sb_header.append("Content-Encoding: ").append(contentEncoding).append("\r\n");
        }
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
}
