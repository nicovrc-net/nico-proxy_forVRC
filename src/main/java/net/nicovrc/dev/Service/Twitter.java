package net.nicovrc.dev.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.nicovrc.dev.Function;
import net.nicovrc.dev.Service.Result.ErrorMessage;
import net.nicovrc.dev.Service.Result.TwitterResult;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;

public class Twitter implements ServiceAPI {

    private String url = null;
    private String Proxy = null;

    private final Gson gson = Function.gson;

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"twitter.com", "x.com"};
    }

    @Override
    public void Set(String json) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("URL")){
            this.url = jsonElement.getAsJsonObject().get("URL").getAsString();
        }
    }

    @Override
    public String Get() {

        if (url == null || url.isEmpty()){
            return gson.toJson(new ErrorMessage("URLがありません"));
        }

        // Proxy
        if (!Function.ProxyList.isEmpty()){
            int i = new SecureRandom().nextInt(0, Function.ProxyList.size());
            Proxy = Function.ProxyList.get(i);
        }

        String[] split = url.split("/");
        int i = 0;
        for (String str : split) {
            if (str.startsWith("status")) {
                break;
            }
            i++;
        }

        final String id = split[i + 1].split("\\?")[0];
        final String contentId;
        if (split.length > i + 3){
            contentId = split[i + 3].split("\\?")[0];
        } else {
            contentId = null;
        }

        try (HttpClient client = Proxy == null ? HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build()
                :
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(5))
                        .proxy(ProxySelector.of(new InetSocketAddress(Proxy.split(":")[0], Integer.parseInt(Proxy.split(":")[1]))))
                        .build()
        ) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.x.com/1.1/guest/activate.json"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .headers("authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA")
                    .POST(HttpRequest.BodyPublishers.noBody())
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

            String token = "";
            JsonElement json = gson.fromJson(text, JsonElement.class);
            if (json.isJsonObject() && json.getAsJsonObject().has("guest_token")) {
                token = json.getAsJsonObject().get("guest_token").getAsString();
            }

            request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.x.com/graphql/_y7SZqeOFfgEivILXIy3tQ/TweetResultByRestId?variables=%7B%22tweetId%22%3A%22" + id + "%22%2C%22withCommunity%22%3Afalse%2C%22includePromotedContent%22%3Afalse%2C%22withVoice%22%3Afalse%7D&features=%7B%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22premium_content_api_read_enabled%22%3Afalse%2C%22communities_web_enable_tweet_community_results_fetch%22%3Atrue%2C%22c9s_tweet_anatomy_moderator_badge_enabled%22%3Atrue%2C%22responsive_web_grok_analyze_button_fetch_trends_enabled%22%3Afalse%2C%22responsive_web_grok_analyze_post_followups_enabled%22%3Afalse%2C%22responsive_web_jetfuel_frame%22%3Afalse%2C%22responsive_web_grok_share_attachment_enabled%22%3Atrue%2C%22articles_preview_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Atrue%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22responsive_web_grok_analysis_button_from_backend%22%3Afalse%2C%22creator_subscriptions_quote_tweet_preview_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22rweb_video_timestamps_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22profile_label_improvements_pcf_label_in_post_enabled%22%3Atrue%2C%22rweb_tipjar_consumption_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22responsive_web_grok_image_annotation_enabled%22%3Afalse%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D&fieldToggles=%7B%22withArticleRichContentState%22%3Atrue%2C%22withArticlePlainText%22%3Afalse%2C%22withGrokAnalyze%22%3Afalse%2C%22withDisallowedReplyControls%22%3Afalse%7D"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .headers("Accept-Encoding", "gzip, br")
                    .headers("authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA")
                    .header("X-Client-Transaction-Id", "ozoi3NmSR6Q+mm10a6SD6Ip273gbYKGGhsUVW72QUk6s1chfi1qeS14PIS0fkt/XDlZCcaNY2e8U09ILFtFf++WNxmR3og")
                    .header("x-guest-token", token)
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

            json = gson.fromJson(text, JsonElement.class);
            TwitterResult result = new TwitterResult();

            //System.out.println(json.getAsJsonObject().get("data").getAsJsonObject().get("tweetResult").getAsJsonObject().has("legacy"));
            if (json.isJsonObject() && json.getAsJsonObject().has("data") && json.getAsJsonObject().get("data").getAsJsonObject().has("tweetResult") && json.getAsJsonObject().get("data").getAsJsonObject().get("tweetResult").getAsJsonObject().has("result") && json.getAsJsonObject().get("data").getAsJsonObject().get("tweetResult").getAsJsonObject().get("result").getAsJsonObject().has("legacy")) {
                JsonElement element = json.getAsJsonObject().get("data").getAsJsonObject().get("tweetResult").getAsJsonObject().get("result").getAsJsonObject().get("legacy");

                int contentNumber = 0;
                if (contentId != null){
                    contentNumber = Integer.parseInt(contentId) - 1;
                }

                result.setURL(element.getAsJsonObject().get("extended_entities").getAsJsonObject().get("media").getAsJsonArray().get(contentNumber).getAsJsonObject().get("expanded_url").getAsString());
                result.setTweetText(element.getAsJsonObject().get("full_text").getAsString());
                result.setThumbnail(element.getAsJsonObject().get("extended_entities").getAsJsonObject().get("media").getAsJsonArray().get(contentNumber).getAsJsonObject().get("media_url_https").getAsString());
                result.setReplyCount(element.getAsJsonObject().get("reply_count").getAsLong());
                result.setRetweetCount(element.getAsJsonObject().get("retweet_count").getAsLong());
                result.setFavoriteCount(element.getAsJsonObject().get("favorite_count").getAsLong());
                result.setBookmarkCount(element.getAsJsonObject().get("bookmark_count").getAsLong());
                result.setQuoteCount(element.getAsJsonObject().get("quote_count").getAsLong());

                JsonElement element1 = element.getAsJsonObject().get("entities").getAsJsonObject().getAsJsonObject().get("media").getAsJsonArray().get(contentNumber);
                result.setDuration(element1.getAsJsonObject().get("video_info").getAsJsonObject().get("duration_millis").getAsLong());
                long MaxBitRate = -1;
                String videoURL = "";
                JsonArray array = element1.getAsJsonObject().get("video_info").getAsJsonObject().get("variants").getAsJsonArray();
                for (JsonElement jsonElement : array) {
                    if (jsonElement.getAsJsonObject().has("bitrate")){
                        if (jsonElement.getAsJsonObject().get("bitrate").getAsLong() >= MaxBitRate){
                            MaxBitRate = jsonElement.getAsJsonObject().get("bitrate").getAsLong();
                            videoURL = jsonElement.getAsJsonObject().get("url").getAsString();
                        }
                    }
                }
                result.setVideoURL(videoURL);

                return gson.toJson(result);

            } else if (json.getAsJsonObject().get("data").getAsJsonObject().get("tweetResult").getAsJsonObject().has("result") && json.getAsJsonObject().get("data").getAsJsonObject().get("tweetResult").getAsJsonObject().get("result").getAsJsonObject().has("reason")){
                String reason = json.getAsJsonObject().get("data").getAsJsonObject().get("tweetResult").getAsJsonObject().get("result").getAsJsonObject().get("reason").getAsString();
                if (reason.equals("NsfwLoggedOut")){
                    return gson.toJson(new ErrorMessage("取得に失敗しました。 (理由 : NSFW)"));
                } else {
                    return gson.toJson(new ErrorMessage("取得に失敗しました。 (理由 : "+reason+")"));
                }
            } else {
                //System.out.println(json);
                return gson.toJson(new ErrorMessage("ツイートがありません"));
            }

        } catch (Exception e){
            e.printStackTrace();
            return gson.toJson(new ErrorMessage("内部エラーです。 ("+e.getMessage()+")"));
        }

        //return "";

    }

    @Override
    public String getServiceName() {
        return "Twitter";
    }

    @Override
    public String getUseProxy() {
        return Proxy;
    }
}
