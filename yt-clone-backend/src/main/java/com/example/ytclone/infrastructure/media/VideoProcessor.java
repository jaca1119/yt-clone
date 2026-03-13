package com.example.ytclone.infrastructure.media;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;

@Service
public class VideoProcessor {

    public Duration getDuration(File file) {
        ProcessBuilder processBuilder = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of" , "default=noprint_wrappers=1:nokey=1", file.getAbsolutePath());
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
}
