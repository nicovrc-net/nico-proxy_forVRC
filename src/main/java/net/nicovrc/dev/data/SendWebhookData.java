package net.nicovrc.dev.data;

public class SendWebhookData {

    private String username;
    private String avatar_url;
    private String content;
    private WebhookEmbeds[] embeds;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public WebhookEmbeds[] getEmbeds() {
        return embeds;
    }

    public void setEmbeds(WebhookEmbeds[] embeds) {
        this.embeds = embeds;
    }
}
