package net.nicovrc.dev.http.getContent;

import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.bilibili;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.util.Arrays;

public class bilibili_com implements GetContent{
    @Override
    public ContentObject run(HttpClient client, String httpRequest, String URL, String json) throws Exception {

        bilibili result = Function.gson.fromJson(json, bilibili.class);

        ContentObject object = new ContentObject();
        object.setHLS(false);
        object.setRefererText(result.getURL());
        return object;
    }

    private static byte[] concatByteArrays(byte[]... arrays) {
        return Arrays.stream(arrays)
                .collect(ByteArrayOutputStream::new,
                        ByteArrayOutputStream::writeBytes,
                        (left, right) -> left.writeBytes(right.toByteArray()))
                .toByteArray();
    }
}