package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import model.VodRecord;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JsonDatabaseManager {

    private static Path currentFilePath;
    private static Path userFolderPath;

    private JsonDatabaseManager() {}

    public static Path getUserFolderPath() { return userFolderPath; }

    public static List<VodRecord> getUndownloadedVods(){
        return loadVods()
                .stream()
                .filter(vod -> !vod.isDownloaded() )
                .toList();
    }

    public static List<VodRecord> getUnuploadedVods(){
        return loadVods()
                .stream()
                .filter(vod -> !vod.isUploaded() )
                .toList();
    }

    public static List<VodRecord> getReadyForUploadVods(){
        return loadVods()
                .stream()
                .filter(vod -> vod.isDownloaded() && !vod.isUploaded() )
                .toList();
    }

    public static void updateDownloadedStatus(String vodId, boolean downloaded){
        List<VodRecord> existingRecords = loadVods();
        boolean isUpdated = false;

        for(int i = 0; i < existingRecords.size(); i++){
            if(existingRecords.get(i).getId().equals(vodId)){
                existingRecords.get(i).setDownloaded(downloaded);
                isUpdated = true;
                break;
            }
        }
        if(isUpdated){
            saveAllVods(existingRecords);
        }
    }

    public static void updateUploadedStatus(String vodId, boolean uploaded){
        List<VodRecord> existingRecords = loadVods();
        boolean isUpdated = false;

        for(int i = 0; i < existingRecords.size(); i++){
            if(existingRecords.get(i).getId().equals(vodId)){
                existingRecords.get(i).setUploaded(uploaded);
                isUpdated = true;
                break;
            }
        }
        if(isUpdated){
            saveAllVods(existingRecords);
        }
    }

    public static void createUserJSON(String username) {
        try {
            //where (jar directory)
            File jarDir = new File(JsonDatabaseManager.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();

            userFolderPath = jarDir.toPath().resolve("streamers").resolve(username);
            Files.createDirectories(userFolderPath);

            currentFilePath = userFolderPath.resolve(username + ".json");

            // create a file with a username
            if (!Files.exists(currentFilePath)) {
                Files.createFile(currentFilePath);
                System.out.println("Created new file: " + currentFilePath.toAbsolutePath());
            } else {
                System.out.println("File already exist: " + currentFilePath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Error creating user JSON: " + e.getMessage());
        }
    }

    public static List<VodRecord> loadVods() {
        try {
            //if file exist
            if (currentFilePath == null) {
                System.err.println("Error: File path is not initialized.");
                return new ArrayList<>();
            }

            //if file doesnt exist or empty
            if (!Files.exists(currentFilePath) || Files.size(currentFilePath) == 0) {
                return new ArrayList<>();
            }

            Gson gson = new GsonBuilder().create();
            String fileContent = Files.readString(currentFilePath);

            //TypeToken to save info about types
            Type listType = new TypeToken<List<VodRecord>>() {}.getType();
            List<VodRecord> readFromFile = gson.fromJson(fileContent, listType);

            // условие ? ifTrue : ifFalse
            return readFromFile != null ? readFromFile : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("Error reading from JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    //add new vods if exist
    public static void saveVodsToJson(JsonArray vodsArray, String channelUrl) {
        try {
            if (currentFilePath == null) {
                System.err.println("Error: File path is not initialized.");
                return;
            }

            List<VodRecord> existingRecords = loadVods();
            List<VodRecord> newRecords = new ArrayList<>();

            for (int i = 0; i < vodsArray.size(); i++) {
                JsonObject vod = vodsArray.get(i).getAsJsonObject();

                String id = vod.get("id").getAsString();
                String title = vod.get("title").getAsString();
                String url = vod.get("url").getAsString();
                String duration = vod.get("duration").getAsString();
                String createdAt = vod.get("created_at").getAsString();

                boolean exists = false;
                for (VodRecord existing : existingRecords) {
                    if (existing.getId().equals(id)) {
                        existing.setTitle(title);
                        existing.setUrl(url);
                        existing.setDuration(duration);
                        existing.setCreatedAt(createdAt);
                        existing.setChannelUrl(channelUrl);
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    VodRecord newRecord = new VodRecord(
                            id, title, url, duration, createdAt, channelUrl
                    );
                    newRecords.add(newRecord);
                }
            }

            //if we have new stream add it to top
            if (!newRecords.isEmpty()) {
                existingRecords.addAll(0, newRecords);
            }

            //nnewest streams stay at the top of the list
            existingRecords.sort(Comparator
                    .comparing(VodRecord::getCreatedAt, Comparator
                            .nullsLast(Comparator
                                    .reverseOrder())));

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(existingRecords);
            Files.writeString(currentFilePath, jsonString);

            System.out.println("Database updates: " + newRecords.size()
                    + " | Total amount of data: "
                    + existingRecords.size()
                    + "\n");

        } catch (Exception e) {
            System.err.println("Error writing to JSON: " + e.getMessage());
        }
    }

    public static void saveAllVods(List<VodRecord> vods){
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(currentFilePath, gson.toJson(vods));

        } catch (Exception e){
            System.err.println("Error writing to JSON: " + e.getMessage());
        }
    }
}