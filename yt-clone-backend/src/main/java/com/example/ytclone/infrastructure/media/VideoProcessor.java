package com.example.ytclone.infrastructure.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class VideoProcessor {
    private final static int PREVIEW_THUMBNAIL_INTERVAL_SECONDS = 5;
    private final static int SPRITE_COLUMNS = 10;

    public Duration getDuration(File file) {
        ProcessBuilder processBuilder = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", file.getAbsolutePath());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String duration = bufferedReader.readLine();
                return Duration.ofMillis(Math.round(Double.parseDouble(duration) * 1000));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void generateThumbnail(File file, String filename) {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-ss", "00:00:01.000", "-i", file.getAbsolutePath(), "-vframes", "1", "videos/thumbnails/%s".formatted(filename));
        pb.redirectErrorStream(true);
        try {
            //wait for? timeout?
            pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //right now it Spring Async but to make it more reliable Kafka, or RabbitMq could be used to schedule message
    @Async
    public CompletableFuture<Boolean> generatePreviewThumbnailsSprite(File file) {
        Duration duration = getDuration(file);
        long thumbnailsToGenerate = duration.toSeconds() / PREVIEW_THUMBNAIL_INTERVAL_SECONDS;
        long spriteRows = Math.ceilDiv(thumbnailsToGenerate, SPRITE_COLUMNS);
        String filename = file.getName().substring(0, file.getName().lastIndexOf(".mp4"));
        String outputFilePath = "videos/preview_thumbnails/%s.jpg".formatted(filename);
        log.info("Generating preview thumbnails for file: {} to {}", file.getAbsolutePath(), outputFilePath);
        //-fps_mode (vsync) could be added
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", //ffmpeg command
                "-discard", //discards frames
                "nokey", //discards no keyframes (leave only keyframes)
                "-i", //input
                file.getAbsolutePath(), //input file
                "-vf", //video filter
                "fps=1/5,scale=160:90,tile=%sx%s".formatted(SPRITE_COLUMNS, spriteRows), //one frame each 5 sec, scale to 160:90, set tile (table) for desired columns x rows
                "-q:v", //quality of generated frames
                "5", //higher worse quality but faster
                "-frames:v", //generate number of frames
                "1", // generate just one frame (one big sprite file)
                "-y", //override file if exist
                "-loglevel", //set loglevel
                "error", //log only errors
                outputFilePath //output file
        );
        try {
            Process process = pb.start();
            process.waitFor(); //should use reasonable timeout but for longer videos it could take long
            List<String> errorOutput = process.errorReader().readAllLines();
            if (errorOutput.isEmpty()) {
                log.error("Generation of preview thumbnails error: {}", errorOutput);
                return CompletableFuture.completedFuture(true);
            } else {
                return CompletableFuture.failedFuture(new RuntimeException(String.join(", ", errorOutput)));
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
