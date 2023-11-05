package xyz.n7mn;

import com.google.gson.Gson;
import xyz.n7mn.data.UDPJsonRequest;
import xyz.n7mn.data.VideoRequest;
import xyz.n7mn.data.VideoResult;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestServer extends Thread{

    private final int Port;

    public RequestServer(int port) {
        this.Port = port;
    }

    @Override
    public void run() {

        System.out.println("[Info] UDP Port "+Port+"で 処理受付用UDPサーバー待機開始");
        while (true){
            DatagramSocket sock = null;
            try {
                sock = new DatagramSocket(Port);

                byte[] data = new byte[100000];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                sock.receive(packet);

                if (packet.getLength() == 0){
                    sock.close();
                    continue;
                }

                String s = new String(Arrays.copyOf(packet.getData(), packet.getLength()));

                //System.out.println("受信 : \n"+s);
                InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());

                try {
                    UDPJsonRequest udpJsonRequest = new Gson().fromJson(s, UDPJsonRequest.class);
                    byte[] bytes = new byte[0];

                    if (udpJsonRequest.getRequestURL().isEmpty() || udpJsonRequest.getTempRequestURL().isEmpty()){
                        bytes = "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8);
                    } else {
                        VideoRequest request = new VideoRequest();
                        request.setRequestCode(udpJsonRequest.getRequestCode());
                        request.setHTTPRequest(udpJsonRequest.getHTTPRequest());
                        request.setServerIP(udpJsonRequest.getRequestServerIP());
                        request.setRequestURL(udpJsonRequest.getRequestURL());
                        request.setTempRequestURL(udpJsonRequest.getTempRequestURL());

                        Matcher matcher = Pattern.compile("(x-nicovrc-titleget: yes|user-agent: unityplayer/)").matcher(udpJsonRequest.getHTTPRequest().toLowerCase(Locale.ROOT));

                        final VideoResult result;
                        if (matcher.find()){
                            result = RequestFunction.getTitle(request, true);
                        } else {
                            result = RequestFunction.getURL(request, true);
                        }

                        bytes = new Gson().toJson(result).getBytes(StandardCharsets.UTF_8);
                    }

                    sock.send(new DatagramPacket(bytes, bytes.length, address));
                    sock.close();

                    data = null;
                    System.gc();
                    continue;
                } catch (Exception e){
                    byte[] bytes = "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8);

                    sock.send(new DatagramPacket(bytes, bytes.length, address));
                    sock.close();

                    data = null;
                    System.gc();
                }
            } catch (Exception e){
                if (sock != null){
                    sock.close();
                }
                e.printStackTrace();
                return;
            }
        }

    }
}
