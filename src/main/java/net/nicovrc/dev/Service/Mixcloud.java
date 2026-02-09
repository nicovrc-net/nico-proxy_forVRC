package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.MixcloudResult;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mixcloud implements ServiceAPI {

    private String url = null;
    private String proxy = null;
    private final Gson gson = Function.gson;
    private HttpClient client = null;

    private final Pattern matcher_URL = Pattern.compile("https://www\\.mixcloud\\.com/(.+)/(.+)/");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.mixcloud.com"};
    }

    @Override
    public void Set(String json, HttpClient client) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.url = jsonElement.getAsJsonObject().get("URL").getAsString();
        }

        this.client = client;
    }

    @Override
    public String Get() {

        if (url  == null || url.isEmpty()){
            return gson.toJson(new ErrorMessage("URLがありません"));
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            proxy = Function.ProxyList.get(i);
        }

        Matcher matcher = matcher_URL.matcher(url);
        if (!matcher.find()){
            return gson.toJson(new ErrorMessage("対応してないURLです。"));
        }

        try {
            final String post = "{\"id\":\"PlayerHeroQuery\",\"query\":\"query PlayerHeroQuery(\\n  $lookup: CloudcastLookup!\\n) {\\n  cloudcast: cloudcastLookup(lookup: $lookup) {\\n    id\\n    name\\n    picture {\\n      isLight\\n      primaryColor\\n      darkPrimaryColor: primaryColor(darken: 60)\\n    }\\n    restrictedReason\\n    seekRestriction\\n    ...HeaderActions_cloudcast\\n    ...PlayButton_cloudcast\\n    ...CloudcastBaseAutoPlayComponent_cloudcast\\n    ...HeroWaveform_cloudcast\\n    ...RepeatPlayUpsellBar_cloudcast\\n    ...HeroAudioMeta_cloudcast\\n    ...HeroChips_cloudcast\\n    ...ImageCloudcast_cloudcast\\n  }\\n  viewer {\\n    restrictedPlayer: featureIsActive(switch: \\\"restricted_player\\\")\\n    hasRepeatPlayFeature: featureIsActive(switch: \\\"repeat_play\\\")\\n    ...HeroWaveform_viewer\\n    ...HeroAudioMeta_viewer\\n    ...HeaderActions_viewer\\n    id\\n  }\\n}\\n\\nfragment AddToButton_cloudcast on Cloudcast {\\n  id\\n  isUnlisted\\n  isPublic\\n}\\n\\nfragment CloudcastBaseAutoPlayComponent_cloudcast on Cloudcast {\\n  id\\n  streamInfo {\\n    uuid\\n    url\\n    hlsUrl\\n    dashUrl\\n  }\\n  audioLength\\n  seekRestriction\\n  currentPosition\\n}\\n\\nfragment FavoriteButton_cloudcast on Cloudcast {\\n  id\\n  isFavorited\\n  isPublic\\n  slug\\n  owner {\\n    id\\n    isFollowing\\n    username\\n    isSelect\\n    displayName\\n    isViewer\\n  }\\n}\\n\\nfragment FavoriteButton_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment HeaderActions_cloudcast on Cloudcast {\\n  ...FavoriteButton_cloudcast\\n  ...AddToButton_cloudcast\\n  ...RepostButton_cloudcast\\n  ...MoreMenu_cloudcast\\n  ...ShareButton_cloudcast\\n}\\n\\nfragment HeaderActions_viewer on Viewer {\\n  ...FavoriteButton_viewer\\n  ...RepostButton_viewer\\n  ...MoreMenu_viewer\\n}\\n\\nfragment HeroAudioMeta_cloudcast on Cloudcast {\\n  slug\\n  plays\\n  publishDate\\n  qualityScore\\n  listenerMinutes\\n  owner {\\n    username\\n    id\\n  }\\n  favorites {\\n    totalCount\\n  }\\n  reposts {\\n    totalCount\\n  }\\n  hiddenStats\\n}\\n\\nfragment HeroAudioMeta_viewer on Viewer {\\n  me {\\n    isStaff\\n    id\\n  }\\n}\\n\\nfragment HeroChips_cloudcast on Cloudcast {\\n  isUnlisted\\n  audioType\\n  isExclusive\\n  audioQuality\\n  owner {\\n    isViewer\\n    id\\n  }\\n  restrictedReason\\n  isAwaitingAudio\\n  isDisabledCopyright\\n}\\n\\nfragment HeroWaveform_cloudcast on Cloudcast {\\n  id\\n  audioType\\n  waveformUrl\\n  previewUrl\\n  audioLength\\n  isPlayable\\n  streamInfo {\\n    hlsUrl\\n    dashUrl\\n    url\\n    uuid\\n  }\\n  restrictedReason\\n  seekRestriction\\n  currentPosition\\n  ...SeekWarning_cloudcast\\n}\\n\\nfragment HeroWaveform_viewer on Viewer {\\n  restrictedPlayer: featureIsActive(switch: \\\"restricted_player\\\")\\n}\\n\\nfragment ImageCloudcast_cloudcast on Cloudcast {\\n  name\\n  picture {\\n    urlRoot\\n    primaryColor\\n  }\\n}\\n\\nfragment MoreMenu_cloudcast on Cloudcast {\\n  id\\n  isSpam\\n  owner {\\n    isViewer\\n    id\\n  }\\n}\\n\\nfragment MoreMenu_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment PlayButton_cloudcast on Cloudcast {\\n  restrictedReason\\n  owner {\\n    isSubscribedTo\\n    isViewer\\n    id\\n  }\\n  id\\n  isAwaitingAudio\\n  isDraft\\n  isPlayable\\n  streamInfo {\\n    hlsUrl\\n    dashUrl\\n    url\\n    uuid\\n  }\\n  audioLength\\n  currentPosition\\n  proportionListened\\n  repeatPlayAmount\\n  hasPlayCompleted\\n  seekRestriction\\n  previewUrl\\n  isExclusive\\n  ...StaticPlayButton_cloudcast\\n  ...useAudioPreview_cloudcast\\n  ...useExclusivePreviewModal_cloudcast\\n  ...useExclusiveCloudcastModal_cloudcast\\n}\\n\\nfragment RepeatPlayUpsellBar_cloudcast on Cloudcast {\\n  audioType\\n  owner {\\n    username\\n    displayName\\n    isSelect\\n    id\\n  }\\n}\\n\\nfragment RepostButton_cloudcast on Cloudcast {\\n  id\\n  isReposted\\n  isExclusive\\n  isPublic\\n  reposts {\\n    totalCount\\n  }\\n  owner {\\n    isViewer\\n    isSubscribedTo\\n    id\\n  }\\n}\\n\\nfragment RepostButton_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment SeekWarning_cloudcast on Cloudcast {\\n  owner {\\n    displayName\\n    isSelect\\n    username\\n    id\\n  }\\n  seekRestriction\\n}\\n\\nfragment ShareButton_cloudcast on Cloudcast {\\n  id\\n  isUnlisted\\n  isPublic\\n  slug\\n  description\\n  audioType\\n  picture {\\n    urlRoot\\n  }\\n  owner {\\n    displayName\\n    isViewer\\n    username\\n    id\\n  }\\n}\\n\\nfragment StaticPlayButton_cloudcast on Cloudcast {\\n  owner {\\n    username\\n    id\\n  }\\n  slug\\n  isAwaitingAudio\\n  restrictedReason\\n}\\n\\nfragment useAudioPreview_cloudcast on Cloudcast {\\n  id\\n  previewUrl\\n}\\n\\nfragment useExclusiveCloudcastModal_cloudcast on Cloudcast {\\n  id\\n  isExclusive\\n  owner {\\n    username\\n    id\\n  }\\n}\\n\\nfragment useExclusivePreviewModal_cloudcast on Cloudcast {\\n  id\\n  isExclusivePreviewOnly\\n  owner {\\n    username\\n    id\\n  }\\n}\\n\",\"variables\":{\"lookup\":{\"username\":\""+matcher.group(1)+"\",\"slug\":\""+matcher.group(2)+"\"}}}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://app.mixcloud.com/graphql"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(post))
                    .build();

            HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentEncoding = send.headers().firstValue("Content-Encoding").isPresent() ? send.headers().firstValue("Content-Encoding").get() : send.headers().firstValue("content-encoding").isPresent() ? send.headers().firstValue("content-encoding").get() : "";
            String jsonText = "{}";
            if (!contentEncoding.isEmpty()){
                byte[] bytes = Function.decompressByte(send.body(), contentEncoding);
                jsonText = new String(bytes, StandardCharsets.UTF_8);
            } else {
                jsonText = new String(send.body(), StandardCharsets.UTF_8);
            }
            JsonElement json = gson.fromJson(jsonText, JsonElement.class);

            if (json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").isJsonNull()){
                return gson.toJson(new ErrorMessage("対応してないURLです。"));
            }

            MixcloudResult result = new MixcloudResult();

            result.setTitle(json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").getAsJsonObject().get("name").getAsString());
            result.setFavCount(json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").getAsJsonObject().get("favorites").getAsJsonObject().get("totalCount").getAsLong());
            result.setPlayCount(json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").getAsJsonObject().get("plays").getAsLong());
            result.setRepostCount(json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").getAsJsonObject().get("reposts").getAsJsonObject().get("totalCount").getAsLong());

            StringBuilder result1 = new StringBuilder();
            final String key = "IFYOUWANTTHEARTISTSTOGETPAIDDONOTDOWNLOADFROMMIXCLOUD";
            byte[] ciphertext = Base64.getDecoder().decode(json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").getAsJsonObject().get("streamInfo").getAsJsonObject().get("hlsUrl").getAsString());
            int keyLength = key.length();

            for (int i = 0; i < ciphertext.length; i++) {
                result1.append((char) (ciphertext[i] ^ key.charAt(i % keyLength)));
            }

            result.setAudioURL(result1.toString());
            result1.setLength(0);
            client.close();

            return gson.toJson(result);

        } catch (Exception e){
            e.printStackTrace();
            return gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }
    }

    @Override
    public String getServiceName() {
        return "Mixcloud";
    }

    @Override
    public String getUseProxy() {
        return proxy;
    }
}
