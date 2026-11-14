import model.VodRecord;
import storage.JsonDatabaseManager;
import storage.UploadLimitManager;
import twitch.DownloaderService;
import youtube.YouTubeUploadService;
import com.google.api.services.youtube.YouTube;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ArchiverManager {

    private final DownloaderService downloader;
    private final YouTubeUploadService uploader;
    private final YouTube youtube;
    private final UploadLimitManager uploadLimitManager;

    public ArchiverManager(DownloaderService downloader, YouTubeUploadService uploader, YouTube youtube) throws Exception{
        this.downloader = downloader;
        this.uploader = uploader;
        this.youtube = youtube;
        this.uploadLimitManager = new UploadLimitManager();
    }

    public void start() {

        List<VodRecord> vods = JsonDatabaseManager.loadVods();

        System.out.println("\nStarting archiving process...");
        System.out.println("Total VODs: " + vods.size());

        System.out.println("Uploads remaining today: " + getRemainingUploads());

        //from the oldest stream to the newest
        for(int i = vods.size() - 1; i >= 0; i--){
            VodRecord vod = vods.get(i);
            try {
                // Already uploaded
                if (vod.isUploaded()) {
                    continue;
                }
                if (!uploadLimitManager.canUpload()) {
                    System.out.println("Daily upload limit reached (6 videos).");
                    break;
                }

                String id = vod.getId();

                Path videoPath = JsonDatabaseManager.getUserFolderPath().resolve(id + ".mp4");

                // Download if necessary
                if (!vod.isDownloaded() || !Files.exists(videoPath)){

                    System.out.println("\nDownloading: " + id);

                    boolean downloadResult =
                            downloader.startTwitchDownloaderCLI(id);

                    if (!downloadResult) {
                        System.err.println("Download failed: " + id);
                        continue;
                    }
                }

                // Check downloaded file
                if (!Files.exists(videoPath)) {
                    System.err.println("Video file not found: " + id);
                    continue;
                }

                // Upload to YouTube
                System.out.println("\nUploading: " + id);

                String fullTitle = vod.getTitle();
                String streamDate = getStreamDate(vod.getCreatedAt());
                String youtubeTitle = createYouTubeTitle(fullTitle, streamDate);

                String description = createDescription(fullTitle, vod, streamDate);

                String youtubeId = uploader.uploadVideo(youtube, videoPath.toString(), youtubeTitle, description);

                if(youtubeId != null && !youtubeId.isBlank()){

                    JsonDatabaseManager.updateUploadedStatus(id, true);
                    uploadLimitManager.registerSuccessfulUpload();

                    System.out.println("Upload completed: " + youtubeId);
                    System.out.println("Uploads remaining today: " + getRemainingUploads());

                } else {
                    System.err.println("Upload failed: " + id);
                }
            } catch (Exception e) {

                System.err.println("Error processing VOD " + vod.getId()
                );

                e.printStackTrace();
            }
        }

        System.out.println("\nArchiving process finished!");
    }

    private String getStreamDate(String createdAt){
        if (createdAt == null || createdAt.isBlank()) {
            return "Unknown date";
        }

        try {
            return OffsetDateTime.parse(createdAt)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    private String createYouTubeTitle(String fullTitle, String streamDate){

        String shortDate;

        if ("Unknown date".equals(streamDate)) {shortDate = "Unknown date";
        } else {
            shortDate = streamDate.substring(0, 6) + streamDate.substring(8);
        }

        String prefix = "(" + shortDate + ") ";
        int maxTitleLength = 100 - prefix.length();

        if (fullTitle.length() > maxTitleLength) {
            fullTitle = fullTitle.substring(0, maxTitleLength);
        }

        return prefix + cleanYouTubeText(fullTitle);
    }

    private String createDescription(String fullTitle, VodRecord vod, String streamDate){
        return cleanYouTubeText(fullTitle)
                + "\n\nDate: " + streamDate
                + "\nVOD Link: " + shortenUrl(vod.getUrl())
                + "\nChannel link: " + shortenUrl(vod.getChannelUrl())
                + "\n\n";
    }

    private String cleanYouTubeText(String text) {
        if(text == null){
            return "";
        }
        return text
                .replace("<", "(")
                .replace(">", ")");
//                .replace("|", "-");
    }

    private String shortenUrl(String url){
        if(url == null || url.isBlank()){
            return "";
        }
        return url
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "");
    }

    private int getRemainingUploads() {
        try {
            return uploadLimitManager.getRemainingUploads();
        } catch (Exception e) {
            System.err.println("Cannot read daily upload limit: " + e.getMessage());
            return 0;
        }
    }
}