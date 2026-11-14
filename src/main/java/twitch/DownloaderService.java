package twitch;

import storage.JsonDatabaseManager;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloaderService {

    private Path twitchDownloaderCLIFilePath;
    private Path ffmpegFilePath;
    private Path userFolderPath;

    private Path tempFolderPath;

    public void initializeTwitchDownloaderCLI() {
        try {
            // create folder for TLCLI
            File jarDir = new File(DownloaderService.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();

            userFolderPath = jarDir.toPath().resolve("services");
            Files.createDirectories(userFolderPath);

            tempFolderPath = jarDir.toPath().resolve("temp");
            Files.createDirectories(tempFolderPath);

            this.twitchDownloaderCLIFilePath =
                    userFolderPath.resolve("TwitchDownloaderCLI.exe");

            this.ffmpegFilePath =
                    userFolderPath.resolve("ffmpeg.exe");

            if (isTwitchDownloaderInstalled() && isFFMpegInstalled()) {
                System.out.println("TwitchDownloaderCLI initialized!");
            } else {
                System.out.println("FFmpeg or TwitchDownloaderCLI is not installed!");
                System.out.println("TwitchDownloader: https://github.com/lay295/TwitchDownloader/releases");
                System.out.println("FFmpeg: https://github.com/absolllute/ffmpeg-build/releases");

                if (Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(userFolderPath.toFile());
                }
            }

        } catch (Exception e) {
            System.err.println("Error handling TwitchDownloaderCLI.exe: "
                    + e.getMessage());
        }
    }

    public boolean isTwitchDownloaderInstalled() {
        return twitchDownloaderCLIFilePath != null
                && Files.isRegularFile(twitchDownloaderCLIFilePath);
    }

    public boolean isFFMpegInstalled() {
        return ffmpegFilePath != null
                && Files.isRegularFile(ffmpegFilePath);
    }

    public boolean startTwitchDownloaderCLI(String ID) {

        if (!isTwitchDownloaderInstalled() || !isFFMpegInstalled()) {
            System.err.println("Downloader or FFmpeg not found!");
            return false;
        }

        try {
            // final path to file
            String outputFilePath = JsonDatabaseManager.getUserFolderPath()
                    .resolve(ID + ".mp4")
                    .toString();

            // all parameters for start
            ProcessBuilder processBuilder = new ProcessBuilder(
//                    "cmd.exe", "/c", "start", "/wait", // windows console
                    twitchDownloaderCLIFilePath.toString(),
                    "videodownload",
                    "--id",
                    ID,
                    "-o",
                    outputFilePath,
                    "--ffmpeg-path",
                    ffmpegFilePath.toString(),
                    "--temp-path",
                    tempFolderPath.toString()
            );

            processBuilder.environment().put("TEMP", tempFolderPath.toString());
            processBuilder.environment().put("TMP", tempFolderPath.toString());

            // Show process in console
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            System.out.println("Exit code: " + exitCode);

            if (exitCode == 0 && Files.exists(Path.of(outputFilePath))) {

                System.out.println("Stream Downloaded successfully!");

                JsonDatabaseManager.updateDownloadedStatus(ID, true);

                return true;
            }

            System.err.println("Stream download failed: " + ID);
            return false;

        } catch (Exception e) {
            System.err.println("Error handling TwitchDownloaderCLI.exe: "
                    + e.getMessage());

            return false;
        }
    }
}
