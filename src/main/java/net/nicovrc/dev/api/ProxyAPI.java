package net.nicovrc.dev.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import net.nicovrc.dev.data.ProxyData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProxyAPI {
    private final List<ProxyData> MainProxyList;
    private final List<ProxyData> JPProxyList;

    public final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    public ProxyAPI(List<ProxyData> MainProxyList, List<ProxyData> JPProxyList){
        this.MainProxyList = MainProxyList;
        this.JPProxyList = JPProxyList;
    }

    /**
     * @param ProxyIP プロキシIP
     * @param ProxyPort プロキシのポート
     * @return 接続できた場合はtrue
     */
    public boolean isCheck(String ProxyIP, int ProxyPort){
        boolean[] temp = {false};
        MainProxyList.forEach(proxyData -> {
            if (temp[0]){
                return;
            }

            if (proxyData.getIP().equals(ProxyIP) && proxyData.getPort() == ProxyPort){
                temp[0] = isCheck(ProxyIP, ProxyPort, false);
            }
        });

        if (!temp[0]){
            JPProxyList.forEach(proxyData -> {
                if (temp[0]){
                    return;
                }

                if (proxyData.getIP().equals(ProxyIP) && proxyData.getPort() == ProxyPort){
                    temp[0] = isCheck(ProxyIP, ProxyPort, true);
                }
            });
        }

        return temp[0];
    }
    /**
     * @param ProxyIP プロキシIP
     * @param ProxyPort プロキシのポート
     * @param isJP 日本国内のプロキシの場合はtrue
     * @return 接続できた場合はtrue
     */
    public boolean isCheck(String ProxyIP, int ProxyPort, boolean isJP){
        try {
            final OkHttpClient build = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ProxyIP, ProxyPort))).build();
            Request request_html;
            if (isJP){
                request_html = new Request.Builder()
                        .url("https://www.nicovideo.jp/watch/so38016254")
                        .build();
            } else {
                request_html = new Request.Builder()
                        .url("https://www.google.co.jp/")
                        .build();
            }
            Response response = build.newCall(request_html).execute();
            if (isJP){
                if (response.body() != null && Pattern.compile("この動画は投稿\\( アップロード \\)された地域と同じ地域からのみ視聴できます。").matcher(response.body().string()).find()){
                    response.close();
                    return false;
                }
            }
            response.close();
        } catch (Exception e){
            return false;
        }
        return true;
    }

    /**
     * 保持しているプロキシリストを設定ファイルから読み込み直して生成し直す
     */
    public void ListRefresh(){
        if (!new File("./config-proxy.yml").exists()){
            return;
        }

        final List<ProxyData> TempMainProxyList = new ArrayList<>();
        final List<ProxyData> TempJPProxyList = new ArrayList<>();
        try {
            final YamlMapping yamlMapping = Yaml.createYamlInput(new File("./config-proxy.yml")).readYamlMapping();
            YamlSequence list = yamlMapping.yamlSequence("VideoProxy");
            YamlSequence list2 = yamlMapping.yamlSequence("OfficialProxy");

            if (list != null){
                for (int i = 0; i < list.size(); i++){
                    String[] s = list.string(i).split(":");
                    if (isCheck(s[0], Integer.parseInt(s[1]), false)){
                        TempMainProxyList.add(new ProxyData(s[0], Integer.parseInt(s[1])));
                    }
                }
            }
            if (list2 != null){
                for (int i = 0; i < list2.size(); i++){
                    String[] s = list2.string(i).split(":");
                    if (isCheck(s[0], Integer.parseInt(s[1]), true)){
                        TempJPProxyList.add(new ProxyData(s[0], Integer.parseInt(s[1])));
                    }
                }
            }
        } catch (Exception e){
            //e.printStackTrace();
        }

        MainProxyList.clear();
        MainProxyList.addAll(TempMainProxyList);
        JPProxyList.clear();
        JPProxyList.addAll(TempJPProxyList);
    }

    /**
     * @return プロキシリスト
     */
    public List<ProxyData> getMainProxyList() {
        return MainProxyList;
    }

    /**
     * @return プロキシリスト(日本国内のみ)
     */
    public List<ProxyData> getJPProxyList() {
        return JPProxyList;
    }

    /**
     * @return 「MainProxy : (保持プロキシ数) JPProxy : (保持プロキシ数)」
     */
    public String getListCount(){
        return "MainProxy : " + MainProxyList.size() + "\nJPProxy : " + JPProxyList.size();
    }
}
