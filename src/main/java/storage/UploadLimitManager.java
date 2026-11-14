package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class UploadLimitManager {

    private static final int DAILY_LIMIT = 6;

    private final Path limitFilePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public UploadLimitManager() throws Exception {

        File jarDir = new File(UploadLimitManager.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI())

                .getParentFile();

        Path settingsFolder = jarDir.toPath().resolve("settings");
        Files.createDirectories(settingsFolder);

        limitFilePath = settingsFolder.resolve("upload_limit.json");

        if (!Files.exists(limitFilePath)){
            UploadLimitData data = new UploadLimitData(LocalDate.now().toString(), 0);
            saveData(data);
        }
    }

    public boolean canUpload() throws IOException {
        UploadLimitData data = loadData();
        resetIfNewDay(data);
        return data.uploadedCount < DAILY_LIMIT;
    }

    public int getRemainingUploads() throws IOException {
        UploadLimitData data = loadData();
        resetIfNewDay(data);
        return DAILY_LIMIT - data.uploadedCount;
    }

    public void registerSuccessfulUpload() throws IOException {
        UploadLimitData data = loadData();
        resetIfNewDay(data);

        if (data.uploadedCount < DAILY_LIMIT) {
            data.uploadedCount++;
            saveData(data);
        }
    }

    private UploadLimitData loadData() throws IOException {
        if (!Files.exists(limitFilePath) || Files.size(limitFilePath) == 0) {
            UploadLimitData data = new UploadLimitData(LocalDate.now().toString(), 0);
            saveData(data);
            return data;
        }

        String json = Files.readString(limitFilePath);
        UploadLimitData data = gson.fromJson(json, UploadLimitData.class);

        if (data == null) {
            data = new UploadLimitData(LocalDate.now().toString(), 0);
        }

        if (data.date == null) {
            data.date = LocalDate.now().toString();
        }

        return data;
    }

    private void resetIfNewDay(UploadLimitData data) throws IOException {
        String today = LocalDate.now().toString();

        if (!today.equals(data.date)) {
            data.date = today;
            data.uploadedCount = 0;
            saveData(data);
            System.out.println("Claude limit reset.");
        }
    }

    private void saveData(UploadLimitData data) throws IOException {
        Files.writeString(limitFilePath, gson.toJson(data));
    }

    private static class UploadLimitData {
        private String date;
        private int uploadedCount;

        public UploadLimitData(String date, int uploadedCount) {
            this.date = date;
            this.uploadedCount = uploadedCount;
        }
    }
}