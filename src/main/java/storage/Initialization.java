package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.ApiCredentials;
import model.ClientSecretYouTube;
import model.Settings;
import model.YouTubeTokens;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Initialization {

    private static Path currentFilePath;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static String CLIENT_ID_TWITCH;
    private static String CLIENT_SECRET_TWITCH;

    public static String getClientIdTwitch(){ return CLIENT_ID_TWITCH; }
    public static String getClientSecretTwitch(){ return CLIENT_SECRET_TWITCH; }

    private static String CLIENT_ID_YOUTUBE;
    private static String CLIENT_SECRET_YOUTUBE;

    public static String getClientIdYoutube(){ return CLIENT_ID_YOUTUBE; }
    public static String getClientSecretYoutube(){ return CLIENT_SECRET_YOUTUBE; }

    private static Settings currentSettings;

    public static void startInitialization() {
        try {
            //create folder for settings
            File jarDir = new File(JsonDatabaseManager.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();

            Path userFolderPath = jarDir.toPath().resolve("settings");
            currentFilePath = userFolderPath.resolve("settings.json");


            if (Files.exists(currentFilePath)) {
                System.out.println("Save file exist.");

                settingFileContent(userFolderPath);

            } else {
                System.out.println("Settings file not exist. Attempt to create one.");

                Files.createDirectories(userFolderPath);
                Files.createFile(currentFilePath);



                System.out.println("Settings file create successfully!");
                settingFileContent(userFolderPath);
            }
        } catch (Exception e) {
            System.err.println("Error handling settings JSON: " + e.getMessage());
        }
    }

    public static void settingFileContent(Path userFolderPath){
        try{
            //First start
            if(Files.size(currentFilePath) == 0){
                System.out.println("First start.");

                Scanner scan = new Scanner(System.in);

                System.out.print("----------------------------------------------\nhttps://console.cloud.google.com/\n");
                System.out.println("First put client_secret_***.json to opened folder");
                if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)){
                    Desktop.getDesktop().open(userFolderPath.toFile());
                }

                System.out.println("Press Enter to continue...");

                scan.nextLine();

                System.out.print("----------------------------------------------\nhttps://dev.twitch.tv/\n");
                System.out.print("Enter Twitch Client ID: \nID: ");
                String UserCLIENT_ID_TWITCH = scan.nextLine();
                System.out.print("Enter Twitch Client Secret: \nSecret: ");
                String UserCLIENT_SECRET_TWITCH = scan.nextLine();

                readClientSecretYoutubeTXT(userFolderPath);

                if (CLIENT_ID_YOUTUBE == null || CLIENT_SECRET_YOUTUBE == null) {
                    System.err.println("YouTube client credentials not found!");
                    return;
                }

//                System.out.print("----------------------------------------------\nhttps://console.cloud.google.com/\n");
//                System.out.print("Enter YouTube Client ID: \nID: ");
//                String UserCLIENT_ID_YOUTUBE = scan.nextLine();
//                System.out.print("Enter YouTube Client Secret: \nSecret: ");
//                String UserCLIENT_SECRET_YOUTUBE = scan.nextLine();

                System.out.println("Thanks!");

                ApiCredentials twitchCreds = new ApiCredentials(UserCLIENT_ID_TWITCH, UserCLIENT_SECRET_TWITCH);
                ApiCredentials youtubeCreds = new ApiCredentials(CLIENT_ID_YOUTUBE, CLIENT_SECRET_YOUTUBE);

                currentSettings = new Settings(twitchCreds, youtubeCreds);

                String json = gson.toJson(currentSettings);
                Files.writeString(currentFilePath, json);

                CLIENT_ID_TWITCH = UserCLIENT_ID_TWITCH.trim();
                CLIENT_SECRET_TWITCH = UserCLIENT_SECRET_TWITCH.trim();
//                CLIENT_ID_YOUTUBE = UserCLIENT_ID_YOUTUBE.trim();
//                CLIENT_SECRET_YOUTUBE = UserCLIENT_SECRET_YOUTUBE.trim();

            } else {
                // File exist and have data
                //read
                String fileContent = Files.readString(currentFilePath);

                currentSettings = gson.fromJson(fileContent, Settings.class);

                if (currentSettings.getYoutubeTokens() == null) {
                    currentSettings.setYoutubeTokens(new YouTubeTokens());
                }

                if(currentSettings != null && currentSettings.getTwitch() != null && currentSettings.getYoutube() != null){
                    CLIENT_ID_TWITCH = currentSettings.getTwitch().getClientId();
                    CLIENT_SECRET_TWITCH = currentSettings.getTwitch().getClientSecret();

                    CLIENT_ID_YOUTUBE = currentSettings.getYoutube().getClientId();
                    CLIENT_SECRET_YOUTUBE = currentSettings.getYoutube().getClientSecret();

                    System.out.println("Settings loaded successfully!");
                }
            }
        } catch (Exception e){
            System.err.println("Error : " + e.getMessage());
        }
    }

    private static void readClientSecretYoutubeTXT(Path userFolderPath) {
        String prefix = "client_secret";
        Gson gson = new GsonBuilder().create();

        try (var stream = Files.list(userFolderPath)) {
            Path clientSecretPath = stream
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElse(null);

            if (clientSecretPath != null) {
                System.out.println("File found: " + clientSecretPath);

                String fileContent = Files.readString(clientSecretPath);
                ClientSecretYouTube clientSecretYouTube = gson.fromJson(fileContent, ClientSecretYouTube.class);

                CLIENT_ID_YOUTUBE = clientSecretYouTube.getClientId();
                CLIENT_SECRET_YOUTUBE = clientSecretYouTube.getClientSecret();


            } else {
                System.out.println("File with prefix " + prefix + " not fount.");
            }

        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
        }
    }

    public static Settings getSettings() {
        return currentSettings;
    }

    public static void saveSettings() throws IOException {
        if (currentSettings == null || currentFilePath == null) {
            throw new IllegalStateException("Settings have not been initialized!");
        }

        String json = gson.toJson(currentSettings);
        Files.writeString(currentFilePath, json);
    }

}
