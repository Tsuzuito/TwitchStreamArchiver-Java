
package model;

public class Settings {

    private ApiCredentials twitch;
    private ApiCredentials youtube;

    private YouTubeTokens youtubeTokens = new YouTubeTokens();

    //for Gson
    public Settings() {
    }

    //for storage.Initialization
    public Settings(ApiCredentials twitch, ApiCredentials youtube) {
        this.twitch = twitch;
        this.youtube = youtube;
    }

    public ApiCredentials getTwitch() {
        return twitch;
    }

    public ApiCredentials getYoutube() {
        return youtube;
    }

    public YouTubeTokens getYoutubeTokens() {
        return youtubeTokens;
    }

    public void setYoutubeTokens(YouTubeTokens youtubeTokens) {
        this.youtubeTokens = youtubeTokens;
    }
}
