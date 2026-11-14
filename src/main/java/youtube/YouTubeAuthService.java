package youtube;

import storage.Initialization;
import model.Settings;
import model.YouTubeTokens;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

import com.google.api.services.youtube.YouTube;

import java.io.IOException;
import java.util.Collections;

//imgonnakms

public class YouTubeAuthService {

    private final String APPLICATION_NAME = "TwitchStreamArchiver";

    private final String YOUTUBE_UPLOAD_SCOPE = "https://www.googleapis.com/auth/youtube.upload";

    private final String YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly";

    private final Settings settings;

    private final NetHttpTransport httpTransport = new NetHttpTransport.Builder().build();

    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    public YouTubeAuthService(Settings settings) {
        this.settings = settings;
    }

    public YouTube getYouTubeService() throws IOException {

        YouTubeTokens tokens = settings.getYoutubeTokens();

        //if token does not exist create throw browser
        if (tokens == null || tokens.getRefreshToken() == null){

            System.out.println("Starting YouTube authorization...");

            GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();

            details.setClientId(settings.getYoutube().getClientId());
            details.setClientSecret(settings.getYoutube().getClientSecret());

            GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
            clientSecrets.setInstalled(details);

            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(
                            httpTransport,
                            jsonFactory,
                            clientSecrets,
                            java.util.Arrays.asList(
                                    YOUTUBE_UPLOAD_SCOPE,
                                    YOUTUBE_READONLY_SCOPE
                            )
                    )
                            .setDataStoreFactory(new MemoryDataStoreFactory())
                            .setAccessType("offline")
                            .build();

            Credential credential =
                    new AuthorizationCodeInstalledApp(
                            flow,
                            new LocalServerReceiver.Builder()
                                    .setPort(8888)
                                    .build()
                    ).authorize("user");

            if (credential.getRefreshToken() == null) {
                throw new IOException("Google did not provide a refresh token. Try authorizing again.");
            }

            saveTokens(credential);

            System.out.println("YouTube authorization successful!");

            return createYouTubeClient(credential);
        }

        //If token exist use it
        Credential credential = new Credential.Builder(
                BearerToken.authorizationHeaderAccessMethod()
        )
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setTokenServerUrl(
                        new GenericUrl("https://oauth2.googleapis.com/token")
                )
                .setClientAuthentication(
                        new ClientParametersAuthentication(
                                settings.getYoutube().getClientId(),
                                settings.getYoutube().getClientSecret()
                        )
                )
                .build();

        credential.setAccessToken(tokens.getAccessToken());
        credential.setRefreshToken(tokens.getRefreshToken());
        credential.setExpirationTimeMilliseconds(tokens.getExpiresAt());

        //if token expire soon create new one
        if (credential.getExpiresInSeconds() == null
                || credential.getExpiresInSeconds() <= 60) {

            System.out.println("Refreshing YouTube token...");

            if (!credential.refreshToken()) {
                throw new IOException("Failed to refresh YouTube token!");
            }

            saveTokens(credential);
        }

        return createYouTubeClient(credential);
    }

    private YouTube createYouTubeClient(Credential credential) {

        return new YouTube.Builder(
                httpTransport,
                jsonFactory,
                credential
        )
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private void saveTokens(Credential credential) throws IOException {

        YouTubeTokens tokens = settings.getYoutubeTokens();

        if (tokens == null) {
            tokens = new YouTubeTokens();
            settings.setYoutubeTokens(tokens);
        }

        tokens.setAccessToken(credential.getAccessToken());
        tokens.setRefreshToken(credential.getRefreshToken());
        tokens.setExpiresAt(credential.getExpirationTimeMilliseconds());

        Initialization.saveSettings();

        System.out.println("YouTube tokens saved!");
    }
}
