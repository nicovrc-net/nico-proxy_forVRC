package net.nicovrc.dev.Service;

import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.SoundCloudResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundCloud implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern clientId = Pattern.compile("client_id:\"(.+)\",client_is");
    private final Pattern datadomeValue = Pattern.compile("datadome=(.+); Max-Age=");
    private final Pattern jsonData = Pattern.compile("window\\.__sc_hydration = \\[(.+)\\];");
    private final Pattern CheckQuestion = Pattern.compile("\\?");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"soundcloud.com"};
    }

    @Override
    public void setHttpClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setURL(String URL) {
        this.url = URL;
    }

    @Override
    public void setToken(String[] token) {

    }

    @Override
    public void setProxy(String proxy) {

    }

    @Override
    public String get() {
        if (url == null || url.isEmpty()){
            return Function.gson.toJson(new ErrorMessage("URLが入力されていません。"));
        }

        try {

            // https://soundcloud.com/kysn/5-hyperflip-67?access=ex_chrome
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .GET()
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            String text = "{}";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                text = new String(bytes, StandardCharsets.UTF_8);
            } else {
                text = new String(send.body(), StandardCharsets.UTF_8);
            }
            Matcher matcher1 = jsonData.matcher(text);

            JsonElement json = null;
            if (matcher1.find()){
                try {
                    json = Function.gson.fromJson("["+matcher1.group(1)+"]", JsonElement.class);
                } catch (Exception e){
                    //client.close();
                    return Function.gson.toJson(new ErrorMessage("対応していないURLです。"));
                }
            }

            if (json == null){
                //client.close();
                return Function.gson.toJson(new ErrorMessage("対応していないURLです。"));
            }

            request = HttpRequest.newBuilder()
                    .uri(new URI("https://a-v2.sndcdn.com/assets/55-163991f5.js"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            text = "{}";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                text = new String(bytes, StandardCharsets.UTF_8);
            } else {
                text = new String(send.body(), StandardCharsets.UTF_8);
            }
            final String ClientId;
            Matcher matcher2 = clientId.matcher(text);
            if (matcher2.find()){
                ClientId = matcher2.group(1);
            } else {
                ClientId = null;
            }

            request = HttpRequest.newBuilder()
                    .uri(new URI("https://dwt.soundcloud.com/js/"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("jspl=yaZWbRyTMWnin8IUNG4JXuMoZID6OCunrtevLMUq7ywtQUDemmaKxEZP65O69E0uD41wvw73U3UR-7oEgzIYooyAsYmI0voi7RKQQj3rVLVfGKrQ0sr7pT99wu7Jmj0KvPgKFdIelcS6ZlVATkHE2XHbl_ipTPSnxr846dmHPa-T5MneSisFE6OK4brMONYvc1j2cNc4riUewTyiOgrCyWl7di6pk8l02QrXBIV0XHu--qwXEDoJUFolvstIb6RNPGG78mLS6WLvy2mP9skd6Q-BqeYlLOS0HtZTHnTf3yhvri3JpChLauQiU6lQYcASSygIVmng2XyiGWwARLg1lwQzauYNwQfn6PlVNOgw4JIX50Hd8oiK3sHouSVp7yMHZg1FW6qrTrVDX5977Kqe036Sv0vNj9SYxnNVmRwezveys9Dj5ZR2JUzM6TzKvIdYfIG_kUewHigRk0zjOsLlEja0b4qqCuE4z6Zs240EZIPFt-wmdGbYUNWzMNpY8KP49NTNywoIaFlHD-ESfXHEdRz9h7Jo0iPa6US8h9LkToDZfFva6Xas3DYCl4Fwjv8SGip_gcVeVlDAzvfFTap3hXwMpBcHGNNz0KbtovRpbPPjw3_wEFlbaCmFIYho8dfUSBgLdFMLgEXxoauO8oUO4GtiKbsgEBs4tCJuYWHDTKj8gfFcqh-vKWZjNUI_eJJGOL1ncQTcpBoeQNzCWByN5966sOw-3lq5wjlZRHM1vBcnt-hH4NbrTTbhrYoItbsEKOMijZ7FitlGz1d-9rCjPPVjr7W-NQqCCkr4J8FKofS-U1JpUfc6zSw8TrHGRwJcLAgURcaK4t60DcvearJ7hvHrrY-4KNbX0X_5FlQXlXsxHPAqAb33zMdDPFyBSlVwpwB48efEqxec7fpBZqPhzgr9ZsTmYR_EMUYxBNMWu2xZo-XYs5YPxq38xkzBkMqwgWTwGFZA7q_1ydzVa7xVJf8lLM7yu4Hd98PrBjsddMpeLZexZlnkA-t8peSwj41NH2nFuRMAQsmbDwNVlcKSCCdNADVDKzjqfDOf3wXkb1rSclaP-Gr0XkQCn_zRcM9bs7zcUNIA8iekcaStS73XM3R4MvKCrRR0ee-H1TBM3QsY-T0-P4jBUEfo88VDF6sZa5BKwlVkPcdYHS_6Zj2LwkMbyv9kFaOBrXlUxiVDYUt9p1k1PR9jBbn6moonR5_V-BPo-d2aM83i-VRhfnnq21tbhF3DE6FJQVCnUesv9Qcc6vPmJBmWZ6k9KzsRBCK6AHAA5BEhiqILaXirCSl4pCRL0crnYuqLFC1-WXqziPb2M-2hz3Af7QfZV2ysUyFZ2mLXhM-8ToHO9TFRwiGgIS-c96RuxVqF6ObbGCWCpOTWaTbXqIq__WCh-bePT9PKubIqKX9qE0UgAP0p-w_epRJobDXQvzpflFal8ijEFAVf3FAmRVi807XY1Tc7KdMNGj6gTuP9XvGUCqQaIk1S7MS1TzUIRXVO923mpdMgq8Nnp8Lgn4oriWMmctOY9c7G_Cq1wtQpEPk4oal1DVGOVwqbDvTD1Am7TlQvEPT1rf4KkcAOU7fQVjR4lnI-iup7E2eG45HhhzycBoQIByhSxhYOJlmY83Da81vnuDES9o5loo9W14btcUUwK0IJ5j9jEJ5ebgOn_qY4cf77hyoY8EMmAg9EBgQzqJ6o9zVbMBNWC6L5CWhl8m33PPqtWWJCeKQ8etATaAawA0-0unlY4QbA9bo80DEdozZ24CUysxBuFEWe5jSynbyC8zZDQhSbwoijnnHspxZonT0-qHOPVxhvqPDRkkzPRhrmgjiDZliWTSDfO1azFcpeNDBwJxH-J23_zhxmFzfYCHTd5AvDTBIRMg6BSKehw0VHU4IirN_8_njAgoK_lgzdl6nWFisg1I4R3ehFj3MyInb1jQ1ESxh4_D7e3z-Kh7JdEds-yCCr4ObewImP5TUqJG3uxR1LseI1G-X3R_mnM6tI6VCB1dj6CNdqsiW2v_ZuYf60RNsV6J2pnT1WmL-apYkZMQfhs1OokxQl_LixsUKcbCO7FQ_ofgZb9sz_1TVThsjdxdVxuMmpgASrOHxtP-ijnYzYEvVrGA98quKj39UHtXiydAx_HEaBiqpoYEyFQqQOSPwCosad8qIYECMBfolXNcLAtFATY6hk_zAICn9Bz9rNLHCRpSDwSngrex2JcG4mWxrphYGcloqZlkSkDaPGVy1htoj1crRBPrn9-CmTW6baaX4gKCJPfX31qx1uaLuyWoa_QjjzAiQumoKEylXHqNMBEhibyFffrDax3kwVcbrTPosKnLWPOa5u01y3IbgmPI1qvbOQNu23kaD_wWsNJxLdOrJEfYPpFv4dUehKZR1NgKeeu8GkiUKk8GZapZovWfaj3O0pGVDk2L-vvyFKH1Prk_j0JVkVtdC7jeLCefschIQvfRIAbXRO4oRZ7E5LVjKJhSZ7W-cpAMANJwCDonksoBZ0vxAOqPMI28t1vzbD3QYo1B7dJlvWhJhscRMF4WikO8nWfAbE7tY_qQV0tVXu-f9Wf5laNMO72n2L1S4wdG8WFHj4UjBR0TU_vhpdTaCBqFECv5wwKp6G_yhRHoQqYMaJ0T042jv_kBcPk4CyFpZ2kZTzgWH1K5KJ47BR5sDM3tfJgH50OToHihjwliMVi6s7vkPEQf1yHrkUgY4uUTsjjg37EfL7r0G9oXWd7EyKL5GJpMtu80NhSi9zVINFV-Xyn2N9swdhXkxRWmUd-R8LMT5_Ip3xhiKFsR6J57p4ILMuwtgrda38d7VgNltwbZLWPRJ1yxFRDgMs_GNyfJ5glC0S9AnhHeTgdsO9TdsmmHw6Ws9XFKvq-1wToQaJDLTieLgDvVKSg1aDXxy7iBSK2bpCJHNlTXa0kCesBGbzIk_wpTwFx32GSIv53IWjgpuyYYKumeoH_Pxta0lOf2tHTohXIPPggr6xmUijXrunT3nz9WeeE_6QYCWMElsiqHfR6Od9fNnr5ByKcUTRnDO5r6SSfCQ1M0tk8qBob-owGsavMX9mQNo9dN-YWS7bF3HQ9NrMR9L8M0t0hOFOepw8whuXR8RleBSeAUB9_wOqoNL_W_mIO-2g4b9BDl8DY1-oS1L1Pc4sppL0X_MOw9sCquKqhjN4uFkf0z_LNKJ4PFLypD-WksXsBbH_G_dtyJLaJQOngNTJZeGbt8hiOf_xYKT6MjbmxN9nVCl0IPXubpDaaqxJg_OWh-WruYO-Ztnx0TRsTcvTwObHxl-QV7plA8LGIDaAAz4r1I29Z6Z6YaNPiXNAsuh0Yy0Mz99Kbr9MKsbka7bT3ngviaDcypcdC24Cwu1hY5wBUSHhTz_i0pJq3Ry1w1n7sOwEb-Vp8NG7J8FrQIMx7wnvQ9ommT3mlsJXL2aiZIFxyB-bh3I_QUk4YTDSfiJgnFxIS3V-Lq2ado82yaYXsVMe67IwuhadkGv46U-fWM8A6LaS_nwRY0RMIP3n_xmrmVgI2oBSIwVLtcQKGQT44BHF2gzYzCib6WzAKaJFcmllZRZgJVLkvL6Zob3ZqUcY1xZVjXsbOdPgo2Qgx6zxlnLaDL5C0Oj_VTY4wBx4qNnnlVDNre9Z-GpbwmaZeQA44jWVXVtej4tV72S1lPOjKZA-8I-YQPL2MfyfP0KNW_VvNs31oeYuY-7ITzH-18usNBgv8T3zazWT2q9kjxF3aGFKhNDnwScHuQtCN3Ukn0zS08XUeoaX5JWVDt-wCt_5QQo4gjaeblri6oKl7CQ9gGlA_SHN7_kPIojiUGPUpeMKqFAM2tz0WCVtdOFKQSTxLE4RVhy_TMRseYor6xBibiynUOd8NZFmfsGaDa82OsaT3TCKevL_SAFavGdDy7IsUh7y06ZJS82Ygm9lMTFzPE8AFGhp5-DKfmBCyD_tVuQe8t0YbkIuDD4kTdcSk8wzy5ITR2rr-JaSUN8H6cvGx_glW_kTGlYk4u32DY7PSIkq5-LAluRQliCgN_uWSYRbZ1ciVGkUWu94WwbMSVm0mV2lm78LMwJQY-E5DvnBS_b9JN2Mwvrwz32gjbvUF0x6LSLVu3aLZYTuVF7dB22-8QfDL6kKh7irQ1swHu_GdVI2-8fZgVeBDpGDqTPt5T_S1VszwGDIAm9paylaq8c3pVTPDTy-Br1XMxRVlRNG2rsDp4gNPj8WQzC6farSwWXfB8Gek5FRJf4Us0zVgOfavFNhenGIEJG_qo8QrvuWRLriVY2hY_ZMQb9znANqR6kpdjWTWYXnwWnGv6Y_E5LpgslMO9yBgi3NrQW_O-TErB_sVEeuI91eDCvIl0HwfVtVJySWw8Pa77q_NXuHknZDkc14yL-1Mx8pLcNuBEonMj0kLI9lB2lNoQxGN1bnbZjRMiGVUs9SbD6p2SQY5225AwLM060BWcpDI8phR6VuIOKMVtDwinV17LOecHohc1Nfo0Gi3vDYK33CQOQJufRc9Uree9LwliOp_Eq6u4HntVXrpFbAQ1FPBx8F0CP_cnQjBsPwdeFsmtRiUT3voEeDZo77lKVYau2ayvxd4iB0NIETqNxPUznhi9OYeYQLRug53YFvuvpZwiUtT9G3RDXEVGTJ9yfKfylw0EqXmM_fs0coYeLgFD3W77ktHKRfXbs-e6-ts6QRMWnCTbed9_z5a_vsKngy79HVrSgrYc68wiLl5TFd8KnfylSfbsjokSckpPEDgopzeI7nk7DhcHnBI7rerO1t0s&eventCounters=%5B%5D&jsType=ch&cid=JnMx~uMTGj_eK7~H8bY3el3yczTrPmLekVaM77a04L4Vozwp_CtiGhMyUi9hqbZaeQYYAjnHJjGAAUgQkrGk~GFiY33ir09LeVo26YPNoS_FndBUq0A6ZuGQjKGi1ouq&ddk=7FC6D561817844F25B65CDD97F28A1&Referer="+URLEncoder.encode(url, StandardCharsets.UTF_8)+"&request="+URLEncoder.encode(url.replaceAll("https://soundcloud.com", ""), StandardCharsets.UTF_8)+"&responsePage=origin&ddv=5.10.0"))
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            text = "";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                text = new String(bytes, StandardCharsets.UTF_8);
            } else {
                text = new String(send.body(), StandardCharsets.UTF_8);
            }
            //System.out.println(text);
            Matcher matcher = datadomeValue.matcher(text);
            String datadome = matcher.find() ? matcher.group(1) : "";
            //System.out.println("datadome: " + datadome);

            String TrackAuthorization = null;
            String BaseURL = null;

            String permalink_url = null;
            String title = null;
            Long duration = null;
            String description = null;

            for (int i = 0; i < json.getAsJsonArray().size(); i++) {
                if (json.getAsJsonArray().get(i).getAsJsonObject().get("hydratable").getAsString().equals("sound")){
                    BaseURL = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    TrackAuthorization = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("track_authorization").getAsString();
                    permalink_url = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("permalink_url").getAsString();
                    title = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString();
                    duration = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("full_duration").getAsLong();
                    description = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("description").getAsString();
                }

            }

            SoundCloudResult result = new SoundCloudResult();
            result.setURL(permalink_url);
            result.setTitle(title);
            result.setDescription(description);
            result.setDuration(duration);

            String hlsUrl = BaseURL + "?client_id=" + ClientId + "&track_authorization=" + TrackAuthorization;

            request = HttpRequest.newBuilder()
                    .uri(new URI(hlsUrl))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .headers("x-datadome-clientid", datadome)
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            text = "{}";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                text = new String(bytes, StandardCharsets.UTF_8);
            } else {
                text = new String(send.body(), StandardCharsets.UTF_8);
            }
            json = Function.gson.fromJson(text, JsonElement.class);

            result.setAudioURL(json.getAsJsonObject().get("url").getAsString());
            //client.close();
            return Function.gson.toJson(result);
        } catch (Exception e){
            e.printStackTrace();
            //client.close();
            return Function.gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }

    }

    @Override
    public String getServiceName() {
        return "SoundCloud";
    }
}
