package model;

public class VodRecord {

    //gson automatically take all this
    private String id;
    private String title;
    private String url;
    private String duration;
    private String createdAt;
    private String channelUrl;
    private boolean isDownloaded = false;
    private boolean isUploaded = false;

    public VodRecord(String id, String title, String url, String duration,
                     String createdAt, String channelUrl) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.duration = duration;
        this.createdAt = createdAt;
        this.channelUrl = channelUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getChannelUrl() { return channelUrl; }
    public void setChannelUrl(String channelUrl) { this.channelUrl = channelUrl; }

    public boolean isDownloaded() { return isDownloaded; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    public boolean isUploaded() { return isUploaded; }
    public void setUploaded(boolean uploaded) { isUploaded = uploaded; }
}