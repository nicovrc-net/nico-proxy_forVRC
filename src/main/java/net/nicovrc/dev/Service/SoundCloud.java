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
                    .uri(new URI("https://a-v2.sndcdn.com/assets/55-9d8411a8.js"))
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
                    .POST(HttpRequest.BodyPublishers.ofString("jspl=dXIgyPEvO7d0xFY1N0P6BwgQ6Ioi4HkVWXLMqnLdg3zMXVdC3QnKKuJ-hI0ZOXkTbyYkAyvfKdHUvtOzTFhssUnK-iZC9PLQ0L641oiSlvcskSf5W_3AaYIgagDVPrvhqeoNPNLz_wE3DQ8t_qeZ4KPeGoi1qHbeOE8fBQgHQGOu_lfjdAPUHkHNUwBFjviX18vkFmK0Yp98UNvuBbFoCqScX5lIADlM65fCWkzPvnzKaDI1pSU7XVVJrr-CgpV4o4tS6gTuq7B0y_HZlfA5C2XanIYGqNB5xjpNG8Bq3wAebBjh-gCQfJ326pwvt_s1dVN616ZW_sGid__TU2ZaDjxq7z4bn_haPGYK0iCCMRHUOSlEZceX_QJh5LW7G7WttXnK6wURnO06wORlPsYauLYsA6icn-77MkdyXHRD9llsWdI_S-C4glQbEjZUwC40meupZoewa5rueafhGfu_ZchVcx-Hwc8twbBEpESnrcsq2jrP7SktzPYUdlmo3CJIBq5RkFJcCTdkB-PxhfVOVMhH3tUeEaPO6Fb8mcPlUFbHhEMppV7-2VmE4lsOwKxbUe_yr9eOfoXn59sRfxmX3Opvm6jDTDw7jEgrUvbQ_OJLuP066JLXPN9e6ppHdyqkzVou4xFkVQmCFQpIBkvqPPYVnO06vRlqKsN5ovQYtLq9G-e6yUJof1Z_TSfXzEAvyNQrJIJKwMAX7hWQkt8Uu0JgrbqV7Wr1SQRHtjCJ1TCdnEK528UP_W3oVa00l4Cn6lwcP84BEzXnt3RGn1f7GxsfyVBsyOapxcQFbYTx9EPYx5RNIco4KbyviG5yzLVX1IiitJmSxf7vHcMVqQEU6f_49xncLvEjX1tY6jx_2dEXr2jNcgEWoekro_naIFNMKZYgCvW4kYUbfN8jefDy_Dp6QflXg_wOqRisY8wGFXwoGjk9A_Ib_Kfg2rxc9WH04ZU4bvBdmBoq29dgTyYWVUpvrpg3Syj35Q1RUUkZNyxjlJNUi1OML2YILZflS66I2Rb4Xae5l7TnXDmjyfb0wCLamsgqfv5uj7O-laSderP5kBB6D01jlIZnXoSJ7B_L-vr24Bdw_ig62tQNe860_53U3hCwZDW7OMU_W9GXbayc-5YDqrHMZPjxzOyWtgZEGBXH1lUpES2ul_MjU-B6bXU2DP9oAxwZGPdb3Rc3WRAB43hyOgbahznrnV9JUNvzJnGBAtlFlyzCEsWcoJAHCLP0qUl2_fdKQQgatbyHdsWACdpocfHowFb3GoZz-dgd57-xIL488hBd51fXJIaRjlGYsSJYsqqNf1Rb1T2MklPOLSgE7tsGN__DZJoQlrWXw_QYUbSgC3HfEoNyzPqxrBuOZfWPVyG1hNljuOvBrPT4OPuHc_IycyLCySGPx3IvAgCMkBtRHupVGPBCl0Sht_NGVKDJLmytuBa35tKQ9Nd-P8OHIS8pNcHZdTvuG-JSgRJ2Nkjr3G4cTgVjIz_W77ryuO7jCEaC0TkUsPGYiqls-uA_8kk9DGv-eRz_XRsCeWQMB6nj0PlnhDRZJvQXQFX3Ilm90SXbWwjC5SfeHrGqtAmCqj3M26fge2HD9-rnUTrIW0E73j4iurZWCk8_PP39wjr0G4csoMRtsqp_AugKhq2D_v9pAmoYi-TuPb1mcqT6LW_xfrnG8GQTKAB0nVxr_wz45-ayw5tOpTh2fvZdImO0NepNdn8mYlnLuDj4JFd3SqfdcYw0mGrYTh_7FBf8E60drK6Wixq7wDWGlSpW2XdtSh2RC4cX4D9uQ3jv9Lmz1skKil3EEHYPRWhaoqz_518itTUgeAUPUNoLdbrnxnKhZjgEUVOD69aGFoHNi9ishuj5dn7Vlw77XiQvbEPYdna46bFJSvG4Ckr2DYnRxum0d-7E9mjUYFRkADL_SyyUBbb5X5zCpxK5gmBxCjDhnFAhwt02LfKUS-Dy9rwT2L1VPcqUBHiuk4415hZq0SVjV4Rev-m5GJBpjZGV-hJokbGWYZe6v_jx4-F8ymocTYkkjqNuwLNtXHGhc7uT76KHo77J4XV3rwchxqGjG-CYJyQQZDgDbfwfdU0BvY-XfXh0mNTrPFbvo9k4d6cF00Q8TyeIPiK84omc4v5K0cF20yV9hjYPs63tzwHHcp50mwi_NA-AuO4Bb2Xj-Ms63heYEydMANgycOYQs33Svm4bklq_fRaBTIPlZGoYMxpHzml_T9a-4mBuYKbDmMqCbZ10YX-TpkwGvDxIGZS8msurfxczGCXsnh2lg8oUemyUirizNJXKnsiAJKzVxalYab8eSrdG6bezYqqPyv-W99v9GMfPxHNgUgH98sA2Orv2scLcRT9VKcfxxZGU5j41d4haO0WKxTV71ZzlETlJ8E2YB183rn1soMBDvXV-RopggnQ36UKds1brW6FzlQ__CWEWGLOpvdvRjp4CIkicOydPibfAgt4tC_FVR0dpiszewpbDBqiQP-35JvoUlHMwTwPSbbEpRPo0OoQWlW0b-LsWsG6VEkZKSnjn26OXcv-0XyGAsGrEQjw9BPpMkHKEDk0hJLzGvuwSCPW3sAcKmog94jD1jGWkajZWFdWqJBq9itTytBFgMXBcbfFw07JXHHEoYCWRF3UtFZPttl-mvaNwVlwG_OyALLDQGyiIb7wSS6qaPDlpr0slAcl8K4CIbye5_q5FsAkSSrGaGMtKpMW83Tc8G7ancDK07LwYsofciH_vI7dLZo2Zgs3SkvTXfKSTUsHhCJBPDDvQfnnkzGXBbQjsZKeMjiX8mWEnZCqGom7SK5G5ypeYUO-QHTgoTlSi6O0VPlJwzQ2xGlsszisILAnhlpO4LZYQ5C2XQzNw-nTBZi3yQ2J0M4JfzsIlszAiGh_nOcWOapws4fXpNnJE0GjF3Sfh_ueVyNd2pijNCZuffTGOavuScxuCjDGzLGGp5HN-5LGp8JuwNC1aImxS-fHHitGNgNIypEad_mw4OhUIilfgRASuriRT9BsvUaCS_dwXZSaP2ua3GxsmgjYaS_D4GoItJHI9TJL_PBQbHop9G-N0cyN04de5IKW4K8ph_o36pOmPasAcezSHeuALW4TdwJynsQ2UfosaBefs1OXnKoCSCuQN5lzqFt4Ov42Z4KrdxUuLyXzUvW15VUtW84QgHXU7_T_HwSrXADobD8V4vRbXp7zDU0rsWR_f9tO94Fzrg-6t4MUyAGniO6HdW3KaeoXmjYTLASlwpQo4AJt6SslQs_fvxAzn7stGS7yjQB5qi2vJCqTsD-QGodoJ57xrY3LC5144Zo7MSCxiRKAUokcDhdqU5wwsgLjvCoyvI4AMR8U75tm-mqGeUYZTml2I8hM4PyWaeW_6cwEb5wLbQOPvPaLLKdJ_KFOA8i5jscj8jgJiSiHFH7rB0J1Qr8-nVID-a0YxXK85qxeC942ASquk6fZVOVrV2mmIS0QLdbHwgpTVmyZdtbNsrSU7f4F4i_YOhhV4j23PR4p6VjVxWnCT5wP85ufP4OSUYvHnJ2UQJMPWlrq_ml-VVE625UxwWkAVugT3CdvTHQxAA_aEjVpIqE0_N83-6NjBIYJaOedwffnVkRhRuzQjWDLhhGJFviDGCAor43IxPXvcs0UhUSmxgXZEAkHgxy1OedsiuDqTVMaIYW3HM69hxo4pLuU6GHvsXMzvzqu-fYVFZxmSiVHTlii5coLSN6aoiRjQ16mSqRhS0byfKF8FF4F_PNHdmLIyeNywcXfFsAwp5yED8lHSTbt2-RTN_cLPPnrPgrhJe54WfB_areWrx2YaYdQloJNO4XaqQKE9DdZWCFhBcRA68g2JUBxKXouKmEnBWV8-Tu86QbR1ucDsOnbF8QQrbG1KUxp7Q1ezpyLtiHha5fqHbw0xkwXEpE6K2hn0_DLPM7jSKByOFrKIXMZTFcERNTlB9hVBFaNtXC0YnTWDDEyNqgxiy3uwSNwquRCEff7okGPFA42hdguP-C1yxb-2RrOKK7vVrBRXFKOgC93Sc467AECNeB-VJAiX-1FG5Gj1sEx7j5Hhv4HH44kbTsl0fmfsMs9ymZyPUtOJCSCxJ80bnWcnRngBsAdMd3Zwe6egP8zBJEk_ltewytIPGMDuZJg3-nnJWU7OnHvdwO3uwmdt-JUjbyck368yaG9UfBTCe3Av5rJuy1c1g33E26n-7L5vQRDmnS-eDLCzcI4cQ-nYFOYPmwxVtUcFepPWiAv0aXLPrVPITUn8PsBnJjOwW7fqzKvG2H_2f34PJpvMzO2rqBC_nnn9V4BTNmcFkaxSZlyNU1ij18A7KblrswVw4yQxlR7Z5n4REFRrjDs7TVbd9TFeDPErMPLnRmSDyeR8BGS8HQygxTDUYAu-rzhXJxDly1NNUjDRwhhyFlO6JvxdcU94QOlyvmVtXUh3kxlQnmVHLx05hxHfbTQ_11txg6XhtqNCsDKu_Fbf49OvwHJoC2NCMClYPEcrFCTop2d_ZCUUSXnIY7IAqOwBdEUIY6C7VzgrNcU2zJFPjt8iI_yHPBJQraTjtJAa17cMwQ2CmDWgWh4y8Nnf1IOmyc3uFizPGmDgP-rVjrYodpNVmPz52yeQtM7-R8THOQPuREKhYHEn2ywDMNeu41A9qzqNMWpVliFsYnAWoyvFgTs1iyPL0-LId4PBvbDiIRuv7KV5o5o184Ntax-HmzWdBoLyR_cNvdaRbLsyR_XZUMs1bCMtezLKoFtfV0ZB5SAJjU3Q7R-QHf2PFeGDvuvaArmtWi3zVQ5HxezU4t8-2ilJ1suSUshmD9aWfVCKyedmy4EegokmDNVNGw-l60NpJ0XYwmavGy_Hd0QFuE9IOHxKrHHRF00wG-AHnZwbsB6B3P6FsN0z3oKH1-wRvM4KK0T5Vfvpj_ge5mDSd4_1hL6oXfHqj5J2-avOA6r1OWQRoZM0VAKS9rkUEfqIUQJ4Ew-mfXzUpY_oXh2Vqxnd1BX9b_gD-Esuqj2d8I_cZ7yBY-sP78100l1Mq5TkvDyJV4onZNya-d7iPP5CM-snd1kJQPI2vhqvbLe8vjbJWj0IT-B1a_D9G1491Mou4Ts16JAQuVefVRMv2-ondJLuqohxzCx3xAw7_J8iuWPtJKcZzn1XF3KUtLZBZZMKqfzJoMKARuoG8gIYXHrSuA9xOjnUd0P7uLhLH8l15sCQ28DoXVaNxBK9C7gG_RfCoJLTfOJH9Lkz8VPb3MRC75IQZc0zOEpwrHtN2JyrFhszs_z3CNaX8CsN7t5jDR9MCusZmrYfGm3jZ6uordlESJG3eqUYnnedxTKYOo7D3ikSAojlLv09BazBxKkfN-J0HaRJaZaGSKw755cPc7DRg7QgxJe3uMikNSka837fk2WukmTqX2CrWr8gslIqBzLjrXAEtAbe02DHzmnFoWrfRyuT62zOPs1MPJxRSOEK_BhPaVWjUIfjKc9doak022pwYdBA3T7gRjN8Qi7vNFH7OBCjiGYlhECA9U8gq3Je-6qF3IQFokkBThSohKaXRdjAWI9kELjmdgEdzZeHIp9dotPU5cMihn78LreQDmNSVDqcNSat-OP225iYA6FCnEbCdfYPBvVPcsPOzPAWVgnLE8fVkumPVoXwXriUcSsfhotO37pcLLYEBFcdkMQxqiGjjp52pthbD9-cqplmxIx7ctkyghQQorxV&eventCounters=%7B%22mousemove%22%3A15%2C%22pointermove%22%3A15%2C%22click%22%3A0%2C%22scroll%22%3A0%2C%22touchstart%22%3A0%2C%22touchend%22%3A0%2C%22touchmove%22%3A0%2C%22keydown%22%3A0%2C%22keyup%22%3A0%7D&jsType=le&cid=_331girGIFd0oY4T1sNDmQr52LJ~PGGLaxXhXnao~pfyym0nKf6PkvoZsc~dR3OOBeN_JCXnHfGfYzDzGC9OjZuHyYM94CeCdgrp1ksdw3Bfq__sgtbzNkzkC7ovjTrC&ddk=7FC6D561817844F25B65CDD97F28A1&Referer="+URLEncoder.encode(url, StandardCharsets.UTF_8)+"&request="+URLEncoder.encode(url.replaceAll("https://soundcloud.com", ""), StandardCharsets.UTF_8)+"&responsePage=origin&ddv=5.7.0"))
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
