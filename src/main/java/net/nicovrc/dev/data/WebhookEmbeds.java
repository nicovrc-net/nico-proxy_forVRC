package net.nicovrc.dev.data;

public class WebhookEmbeds {

    private String title;
    private String description;

    private WebhookFields[] fields;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WebhookFields[] getFields() {
        return fields;
    }

    public void setFields(WebhookFields[] fields) {
        this.fields = fields;
    }
}
