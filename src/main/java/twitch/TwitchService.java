package twitch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import storage.JsonDatabaseManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchService {

    private final String CLIENT_ID;
    private final String CLIENT_SECRET;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String CURRENT_TWITCH_CHANNEL;
    private String TOKEN;
    private String CHANNEL_ID;

    public TwitchService(String clientId, String clientSecret) {
        this.CLIENT_ID = clientId;
        this.CLIENT_SECRET = clientSecret;
    }

    public void fetchVodsForUser(String twitchLink){
        getTwitchChannelName(twitchLink);

        if (CURRENT_TWITCH_CHANNEL == null) {
            System.out.println("Canceled by wrong link.");
            return;
        }

        System.out.println("\nChannel: " + CURRENT_TWITCH_CHANNEL);

        try {
            JsonDatabaseManager.createUserJSON(CURRENT_TWITCH_CHANNEL);

            getAccessToken();
            System.out.println("Getting token is success!");

            getUserID();
            getVods();

        } catch (Exception e ){
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void getTwitchChannelName(String twitchLink) {
        Pattern pattern = Pattern.compile("twitch\\.tv/([^/?]+)");
        Matcher matcher = pattern.matcher(twitchLink);

        if (matcher.find()) {
            this.CURRENT_TWITCH_CHANNEL = matcher.group(1);
        } else {
            System.out.println("Incorect Link");
        }
    }

    private void getAccessToken() throws Exception{
        String url = "https://id.twitch.tv/oauth2/token?client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&grant_type=client_credentials";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //code 200 is success.
        if(response.statusCode() == 200){
            parseAndSetTOKEN(response);
        } else {
            throw new RuntimeException("Error. Response code: " + response.statusCode());
        }
    }

    public void getUserID() throws Exception{
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitch.tv/helix/users?login=" + CURRENT_TWITCH_CHANNEL))
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + TOKEN)
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //code 200 is success.
        if(response.statusCode() == 200){
            parseAndSetCHANNEL_ID(response);
        } else {
            throw new RuntimeException("Response code: " + response.statusCode());
        }
    }

    private void parseAndSetTOKEN(HttpResponse<String> responce){
        JsonObject jsonObject = JsonParser.parseString(responce.body()).getAsJsonObject();

        this.TOKEN = jsonObject.get("access_token").getAsString();
    }

    private void parseAndSetCHANNEL_ID(HttpResponse<String> response){
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();

        JsonObject userData = jsonObject.getAsJsonArray("data").get(0).getAsJsonObject();

        this.CHANNEL_ID = userData.get("id").getAsString();

        System.out.println("Channel ID successfully found: " + this.CHANNEL_ID);
    }

    private void parseVodsinfo(HttpResponse<String> response){
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();

        JsonArray vodsArray = jsonObject.getAsJsonArray("data");

        System.out.println("\nTotal amount of streams: " + vodsArray.size());

        String channelUrl = "https://www.twitch.tv/" + CURRENT_TWITCH_CHANNEL;

        JsonDatabaseManager.saveVodsToJson(vodsArray, channelUrl);

        for(int i = 0; i < vodsArray.size(); i++){
            JsonObject vod = vodsArray.get(i).getAsJsonObject();

            String id = vod.get("id").getAsString();
            String title = vod.get("title").getAsString();
            String url = vod.get("url").getAsString();
            String duration = vod.get("duration").getAsString();
            String createdAt = vod.get("created_at").getAsString();

            System.out.println("ID: " + id + " | " + duration + " | " + title);
            System.out.println("Created at: " + createdAt);
            System.out.println("URL: " + url);
            System.out.println("---");
        }
    }

    public void getVods()throws Exception{
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitch.tv/helix/videos?user_id=" + CHANNEL_ID + "&first=100"))
                .header("Client-Id", CLIENT_ID)
                .header("Authorization", "Bearer " + TOKEN)
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //code 200 is success
        if(response.statusCode() == 200){
            parseVodsinfo(response);
        } else {
            throw new RuntimeException("Response code: " + response.statusCode());
        }
    }
}