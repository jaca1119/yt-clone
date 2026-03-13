package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class VideoService {

    private final VideoRepository videoRepository;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public List<Video> getVideos() {
        return videoRepository.getVideos();
    }

    public Optional<Path> getVideoFilePath(UUID id) {
        return videoRepository.getVideos().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .map(video -> Path.of("videos/%s".formatted(video.getFilename())).toAbsolutePath());
    }

    public Optional<Path> getVideoThumbnailFilePath(UUID id) {
        return videoRepository.getVideos().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .map(video -> Path.of("videos/thumbnails/%s.jpg".formatted(video.getFilename().split(".mp4")[0])).toAbsolutePath());
    }

    public void saveVideo(UUID id, File file) {
        //get duration
        ProcessBuilder processBuilder = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of" , "default=noprint_wrappers=1:nokey=1", file.getAbsolutePath());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String duration = bufferedReader.readLine();
                Duration videoDuration = Duration.ofMillis(Math.round(Double.parseDouble(duration) * 1000));

                //generate thumbnail
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-ss", "00:00:01.000", "-i", file.getAbsolutePath(), "-vframes", "1", "videos/thumbnails/%s.jpg".formatted(id));
                pb.redirectErrorStream(true);
                //wait for?
                pb.start();

                videoRepository.save(new Video(id, file.getName(), file.getName(), videoDuration.getSeconds(), LocalDateTime.now()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
