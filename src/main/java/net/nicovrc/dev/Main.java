package net.nicovrc.dev;

import net.nicovrc.dev.http.NicoVRCWebAPI;
import net.nicovrc.dev.http.NicoVRCHTTP;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final List<NicoVRCHTTP> httpServiceList = new ArrayList<>();

    public static void main(String[] args) {

        // HTTP受付
        httpServiceList.add(new NicoVRCWebAPI());

        TCPServer tcpServer = new TCPServer(httpServiceList);
        tcpServer.start();
        try {
            tcpServer.join();
        } catch (Exception e){
            // e.printStackTrace();
        }

    }
}
