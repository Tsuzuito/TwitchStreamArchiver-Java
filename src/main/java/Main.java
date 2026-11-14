import storage.Initialization;
import model.Settings;

import youtube.YouTubeAuthService;
import youtube.YouTubeUploadService;
import twitch.TwitchService;
import twitch.DownloaderService;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;

import java.util.Collections;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            Initialization.startInitialization();

            Settings settings = Initialization.getSettings();

            if (settings == null) {
                System.out.println("Settings not found!");
                return;
            }

            YouTubeAuthService authService = new YouTubeAuthService(settings);
            YouTube youtube = authService.getYouTubeService();

            //check YouTube channel
            ChannelListResponse response = youtube.channels()
                    .list(Collections.singletonList("snippet"))
                    .setMine(true)
                    .execute();

            if (response.getItems() == null || response.getItems().isEmpty()) {

                System.out.println("YouTube channel not found!");
                return;
            }

            Channel channel = response.getItems().get(0);

            System.out.println("YouTube channel connected!");
            System.out.println("Channel: " + channel.getSnippet().getTitle());

            Scanner scanner = new Scanner(System.in);

            System.out.print("Set Twitch link (Example: https://www.twitch.tv/user)\nLink: ");
            String twitchLink = scanner.nextLine();

            //Get VODs from Twitch
            TwitchService twitchService = new TwitchService(
                    settings.getTwitch().getClientId(),
                    settings.getTwitch().getClientSecret()
            );

            twitchService.fetchVodsForUser(twitchLink);
            DownloaderService downloader = new DownloaderService();

            downloader.initializeTwitchDownloaderCLI();

            if (!downloader.isTwitchDownloaderInstalled() || !downloader.isFFMpegInstalled()){

                System.err.println("Cannot start archiving process!");
                return;
            }

            YouTubeUploadService uploader = new YouTubeUploadService();

            ArchiverManager archiverManager = new ArchiverManager(
                    downloader,
                    uploader,
                    youtube
            );

            archiverManager.start();

            System.out.println("\nApplication finished!");

        } catch (Exception e) {
            System.err.println("Application error!");
            e.printStackTrace();
        }
    }
}
