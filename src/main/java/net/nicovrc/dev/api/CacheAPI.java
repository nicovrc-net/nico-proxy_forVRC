package net.nicovrc.dev.api;


import net.nicovrc.dev.data.CacheData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class CacheAPI {

    private final ConcurrentHashMap<String, CacheData> CacheList;
    private final OkHttpClient client;

    private final ConcurrentHashMap<String, String> TempList = new ConcurrentHashMap<>();

    /**
     * @param CacheList キャッシュリスト
     * @param client OKHttpのClient
     */
    public CacheAPI(ConcurrentHashMap<String, CacheData> CacheList, OkHttpClient client){
        this.CacheList = CacheList;
        this.client = client;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 無駄にアクセスして見に行かないように
                TempList.clear();
            }
        }, 0L, 5000L);
    }

    /**
     * @param URL キャッシュがあるかどうかチェックしたい処理前URL
     * @return キャッシュしているURL(処理後)
     */
    public String getCache(String URL){
        CacheData data = CacheList.get(URL);

        if (data == null){
            return null;
        }

        if (data.getExpiryDate() == -1){
            return data.getCacheUrl();
        }

        if (data.getCacheUrl().startsWith("http://") || data.getCacheUrl().startsWith("https://")){
            if (CheckCache(URL)){
                return data.getCacheUrl();
            } else {
                return null;
            }
        } else {
            return data.getCacheUrl();
        }
    }

    /**
     * @param URL チェックしたいURL
     * @return trueの場合はキャッシュリストに存在する
     */
    public boolean CheckCache(String URL){
        CacheData data = CacheList.get(URL);

        if (data == null){
            return false;
        }

        final HashMap<String, String> temp = new HashMap(TempList);

        // 連続でアクセスきたとき用
        if (temp.get(URL) != null){
            return true;
        }

        if (data.getCacheUrl().startsWith("http://") || data.getCacheUrl().startsWith("https://")){
            Request request = new Request.Builder()
                    .url(data.getCacheUrl())
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0")
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.code() >= 200 && response.code() <= 399) {
                    cache = true;
                }
                response.close();
            } catch (Exception e){
                // e.printStackTrace();
            }
            TempList.put(URL, data.getCacheUrl());
        } else {
            return true;
        }

        return cache;
    }

    /**
     * @param RequestURL 処理前URL
     * @param CacheURL 処理後URL
     * @param ExpiryDate 有効期限(-1で無限)
     */
    public void setCache(String RequestURL, String CacheURL, long ExpiryDate){
        CacheList.put(RequestURL, new CacheData(ExpiryDate, CacheURL));
    }

    public void removeCache(String RequestURL){
        CacheList.remove(RequestURL);
    }

    public HashMap<String, CacheData> getList(){
        return new HashMap<>(CacheList);
    }

    public void ListRefresh(){
        final HashMap<String, CacheData> temp = new HashMap<>(CacheList);

        //System.out.println(temp.size());
        //System.out.println(new Date().getTime());
        Date date = new Date();
        temp.forEach((RequestURL, Result)->{
            if (Result.getExpiryDate() == -1){
                // キャッシュ期間が設定されていないものはHTTP通信して確認する
                if (!CheckCache(RequestURL)){
                    removeCache(RequestURL);
                }
            } else if (Result.getExpiryDate() != -1 && date.getTime() - Result.getExpiryDate() >= 0){
                // キャッシュ期間1日過ぎてたらアクセスせずに削除
                removeCache(RequestURL);
            }
        });
        //System.out.println(new Date().getTime());
    }

    /**
     * @return 「CacheCount : キャッシュ数」
     */
    public String getListCount(){
        return "CacheCount : " + CacheList.size();
    }
}
