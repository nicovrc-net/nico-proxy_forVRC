package net.nicovrc.dev.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.gson.Gson;
import net.nicovrc.dev.data.OutputJson;
import net.nicovrc.dev.data.ServerData;
import net.nicovrc.dev.data.UDPPacket;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ServerAPI {
    private final ConcurrentHashMap<String, ServerData> ServerList;
    private final Gson gson = new Gson();
    private boolean isRefresh = false;

    public ServerAPI(ConcurrentHashMap<String, ServerData> ServerList){
        this.ServerList = ServerList;
    }

    /**
     * @param IP 処理サーバーのURL
     * @param Port 処理サーバーのポート
     * @return 接続できた場合はtrue
     */
    public boolean isCheck(String IP, int Port){
        UDPPacket check = new UDPPacket();
        DatagramSocket udp_sock = null;
        try {
            udp_sock = new DatagramSocket();

            byte[] textByte = gson.toJson(check).getBytes(StandardCharsets.UTF_8);
            DatagramPacket udp_packet = new DatagramPacket(textByte, textByte.length,new InetSocketAddress(IP, Port));
            udp_sock.send(udp_packet);

            byte[] temp1 = new byte[100000];
            DatagramPacket udp_packet2 = new DatagramPacket(temp1, temp1.length);
            udp_sock.setSoTimeout(100);
            udp_sock.receive(udp_packet2);

            String result = new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength()));
            //System.out.println("受信 : " + result);
            OutputJson json = new Gson().fromJson(result, OutputJson.class);

            udp_sock.close();
        } catch (Exception e){
            try {
                if (udp_sock != null){
                    udp_sock.close();
                }
            } catch (Exception ex){
                //ex.printStackTrace();
            }

            return false;
        }

        return true;
    }

    /**
     * @param ServerName 処理サーバーの名前
     * @return 接続できた場合はtrue
     */
    public boolean isCheck(String ServerName){
        ServerData data = ServerList.get(ServerName);
        return isCheck(data.getIP(), data.getPort());
    }

    /**
     * 保持している処理サーバーのリストを設定ファイルから構築し直す
     */
    public void ListRefresh(){
        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
            final YamlSequence list = yamlMapping.yamlSequence("ServerList");

            isRefresh = true;
            removeAllList();
            if (list != null){
                for (int i = 0; i < list.size(); i++){
                    String[] s = list.string(i).split(":");
                    //System.out.println(isCheck(s[0], Integer.parseInt(s[1])));
                    if (isCheck(s[0], Integer.parseInt(s[1]))){
                        addList("Server"+(i+1), s[0], Integer.parseInt(s[1]));
                    }
                }
            }
            isRefresh = false;
        } catch (Exception e){
            // e.printStackTrace();
        }
    }

    /**
     * @param ServerName サーバー名
     * @param IP サーバーのIPアドレス
     * @param Port サーバーのUDPポート
     */
    public void addList(String ServerName, String IP, int Port){
        ServerList.put(ServerName, new ServerData(IP, Port));
    }

    public void removeList(String IP, int Port){
        HashMap<String, ServerData> temp = new HashMap<>(ServerList);
        temp.forEach((ServerName, ServerData) -> {
            if (ServerData.getIP().equals(IP) && ServerData.getPort() == Port){
                ServerList.remove(ServerName);
            }
        });
    }

    /**
     * @param ServerName サーバー名
     */
    public void removeList(String ServerName){
        ServerList.remove(ServerName);
    }

    /**
     * 保持している処理サーバーのリストの中身を全部削除する
     */
    public void removeAllList(){
        ServerList.clear();
    }

    /**
     * @return 保持している処理サーバーのリスト
     */
    public HashMap<String, ServerData> getList(){
        return new HashMap<>(ServerList);
    }

    /**
     * @return 「ServerCount : (処理サーバーリストの件数)」
     */
    public String getListCount(){
        return "ServerCount : " + ServerList.size();
    }

    /**
     * @param packet 送信するデータ
     * @return 送信結果
     */
    public UDPPacket SendServer(UDPPacket packet){
        if (ServerList.isEmpty() && !isRefresh){
            return null;
        }

        long l = 0;
        while (isRefresh && ServerList.isEmpty()){
            l++;
        }

        final HashMap<String, ServerData> temp = getList();

        int i = new SecureRandom().nextInt(1, temp.size());

        while (!temp.isEmpty()){
            ServerData data = temp.get("Server" + i);
            try {
                DatagramSocket udp_sock = new DatagramSocket();

                String jsonText = gson.toJson(packet);
                DatagramPacket udp_packet = new DatagramPacket(jsonText.getBytes(StandardCharsets.UTF_8), jsonText.getBytes(StandardCharsets.UTF_8).length, new InetSocketAddress(data.getIP(), data.getPort()));
                udp_sock.send(udp_packet);
                udp_sock.setSoTimeout(2000);

                byte[] temp1 = new byte[100000];
                DatagramPacket udp_packet2 = new DatagramPacket(temp1, temp1.length);
                udp_sock.receive(udp_packet2);

                UDPPacket json = gson.fromJson(new String(Arrays.copyOf(udp_packet2.getData(), udp_packet2.getLength())), UDPPacket.class);

                if (json.getResultURL() != null || json.getErrorMessage() != null){
                    packet.setResultURL(json.getResultURL());
                    packet.setErrorMessage(json.getErrorMessage());
                    temp.clear();
                }
            } catch (Exception e){
                temp.remove("Server" + i);
            }
        }

        return packet;
    }
}
