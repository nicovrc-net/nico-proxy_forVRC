package net.nicovrc.dev;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Function {
    public static final String Version = "3.0.0-alpha.1";
    public static final Gson gson = new Gson();
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0 nicovrc-net/" + Version;

    private static final Pattern HTTPVersion = Pattern.compile("HTTP/(\\d+\\.\\d+)");
    private static final Pattern HTTP = Pattern.compile("(CONNECT|DELETE|GET|HEAD|OPTIONS|PATCH|POST|PUT|TRACE) (.+) HTTP/(\\d\\.\\d)");

    public static String getHTTPRequest(Socket sock) throws Exception{
        //System.out.println("debug 1");
        InputStream in = sock.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] data = new byte[1024];
        int readSize = in.read(data);

        if (readSize <= 0) {
            return null;
        }
        //System.out.println("debug 2");
        data = Arrays.copyOf(data, readSize);
        sb.append(new String(data, StandardCharsets.UTF_8));

        if (readSize == 1024){
            data = new byte[1024];
            readSize = in.read(data);
            boolean isLoop = true;
            while (readSize >= 0){
                //System.out.println(readSize);
                data = Arrays.copyOf(data, readSize);
                sb.append(new String(data, StandardCharsets.UTF_8));

                data = null;

                if (readSize < 1024){
                    isLoop = false;
                }

                if (!isLoop){
                    break;
                }

                data = new byte[1024];
                readSize = in.read(data);
                if (readSize < 1024){
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
        sb_header.append("\n");
        if (AccessControlAllowOrigin != null){
            sb_header.append("Access-Control-Allow-Origin: ").append(AccessControlAllowOrigin).append("\n");
        }
        sb_header.append("Content-Length: ").append(body.length).append("\n");
        sb_header.append("Content-Type: ").append(contentType).append("\n\n");

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
        String temp = HTTPRequest.substring(0, 3);
        if (temp.toUpperCase(Locale.ROOT).equals("GET") || temp.toUpperCase(Locale.ROOT).equals("POST") || temp.toUpperCase(Locale.ROOT).equals("HEAD")){
            return temp;
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
}
