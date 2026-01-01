package net.nicovrc.dev;

import net.nicovrc.dev.http.GetAPI;
import net.nicovrc.dev.http.GetInterface;
import net.nicovrc.dev.http.GetURL;
import net.nicovrc.dev.http.GetVideo;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Timer;

public class HTTPServer extends Thread {

    private final Timer timer = new Timer();
    private final boolean isView;

    public HTTPServer(){
        isView = Function.config.isViewLog();

    }

    @Override
    public void run() {

        boolean[] isRun = {true};
        ServerSocket svSock = null;
        try {
            svSock = new ServerSocket(Function.config.getHttpPort());
        } catch (Exception e){
            e.printStackTrace();
            isRun[0] = false;
            return;
        }

        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        Function.setClient(client);

        while (isRun[0]){
            try {
                final Socket sock = svSock.accept();
                Thread.ofVirtual().start(()->{
                    try {
                        InputStream in = sock.getInputStream();
                        OutputStream out = sock.getOutputStream();

                        final String httpRequest = Function.getHTTPRequest(sock);
                        if (httpRequest == null){
                            in.close();
                            out.close();
                            sock.close();
                            return;
                        }

                        String uri = Function.getURI(httpRequest);

                        GetInterface run = null;
                        if (uri.startsWith("/?url=")){
                            run = new GetURL(httpRequest, uri);
                        } else if (uri.startsWith("/api")){
                            run = new GetAPI(httpRequest, uri);
                        } else if (uri.startsWith("/video")){
                            run = new GetVideo(httpRequest, uri);
                        }

                        if (run != null){
                            byte[] result = run.run();
                            out.write(result);
                        } else {

                            String httpVersion = Function.getHTTPVersion(httpRequest);
                            String method = Function.getMethod(httpRequest);

                            if (httpVersion == null || httpVersion.equals("1.1")){
                                if (method != null && method.equals("HEAD")){
                                    out.write(Function.err400__1_1_head);
                                } else {
                                    out.write(Function.err400__1_1);
                                }
                            } else if (httpVersion.equals("2.0")){
                                if (method != null && method.equals("HEAD")){
                                    out.write(Function.err400__2_0_head);
                                } else {
                                    out.write(Function.err400__2_0);
                                }
                            } else {
                                if (method != null && method.equals("HEAD")){
                                    out.write(Function.err400__1_0_head);
                                } else {
                                    out.write(Function.err400__1_0);
                                }
                            }

                        }

                        in.close();
                        out.close();
                        sock.close();
                    } catch (Exception e){
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
                isRun[0] = false;
            }
        }

        try {
            client.close();
            svSock.close();
        } catch (Exception e){
            // e.printStackTrace();
        }

    }
}
