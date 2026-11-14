package youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.api.client.http.InputStreamContent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

public class YouTubeUploadService {

    public String uploadVideo(
            YouTube youtube,
            String videoPath,
            String title,
            String description
    ) throws IOException {

        File videoFile = new File(videoPath);

        if (!videoFile.exists()) {
            throw new IOException("Video file not found: " + videoFile.getAbsolutePath());
        }

        Video video = new Video();

        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);
        //gaming
        snippet.setCategoryId("22");

        video.setSnippet(snippet);

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("private");

        video.setStatus(status);

        try (FileInputStream inputStream = new FileInputStream(videoFile)){

            InputStreamContent mediaContent = new InputStreamContent("video/mp4", inputStream);

            mediaContent.setLength(videoFile.length());

            System.out.println("Uploading video...");

            YouTube.Videos.Insert request = youtube
                    .videos()
                    .insert(Collections
                            .singletonList("snippet,status"), video, mediaContent);

            request.setNotifySubscribers(false);

            Video uploadedVideo = request.execute();

            System.out.println("Video uploaded successfully!");
            System.out.println("Video ID: " + uploadedVideo.getId());

            String youtubeUrl = "https://www.youtube.com/watch?v=" + uploadedVideo.getId();

            System.out.println("Video URL: " + youtubeUrl);

            return uploadedVideo.getId();
        }
    }
}
