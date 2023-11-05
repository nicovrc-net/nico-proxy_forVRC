package xyz.n7mn;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import xyz.n7mn.data.UDPJsonRequest;
import xyz.n7mn.data.VideoRequest;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestServer extends Thread{
    @Override
    public void run() {

        while (true){
            try {
                DatagramSocket sock = new DatagramSocket(8888);

                byte[] data = new byte[100000];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                sock.receive(packet);

                if (packet.getLength() == 0){
                    sock.close();
                    continue;
                }

                String s = new String(Arrays.copyOf(packet.getData(), packet.getLength()));

                System.out.println("受信 : \n"+s);
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

                        Matcher matcher = Pattern.compile("(|)").matcher(udpJsonRequest.getHTTPRequest());

                        RequestFunction.getURL(request, true);

                    }

                    sock.send(new DatagramPacket(bytes, bytes.length, address));
                    sock.close();
                } catch (Exception e){
                    byte[] bytes = "{\"Error\": \"Bad Request\"}".getBytes(StandardCharsets.UTF_8);

                    sock.send(new DatagramPacket(bytes, bytes.length, address));
                    sock.close();
                }

                data = null;
                System.gc();
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        }

    }
}
