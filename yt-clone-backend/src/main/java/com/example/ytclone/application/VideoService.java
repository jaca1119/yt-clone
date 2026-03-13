package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.media.VideoProcessor;
import com.example.ytclone.infrastructure.persistence.VideoEntity;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
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
    private final VideoProcessor videoProcessor;

    public VideoService(VideoRepository videoRepository, VideoProcessor videoProcessor) {
        this.videoRepository = videoRepository;
        this.videoProcessor = videoProcessor;
    }

    public List<Video> getVideos() {
        return videoRepository.findAll().stream().map(this::toVideo).toList();
    }

    public Optional<Path> getVideoFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> Path.of("videos/%s".formatted(video.getFilename())).toAbsolutePath());
    }

    public Optional<Path> getVideoThumbnailFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> Path.of("videos/thumbnails/%s.jpg".formatted(video.getFilename().split(".mp4")[0])).toAbsolutePath());
    }

    public void saveVideo(UUID id, File file, LocalDateTime uploadDateTime) {
        Duration duration = videoProcessor.getDuration(file);
        videoProcessor.generateThumbnail(file, "%s.jpg".formatted(id));

        videoRepository.save(new VideoEntity(id, file.getName(), file.getName(), duration.getSeconds(), uploadDateTime));
    }

    private Video toVideo(VideoEntity videoEntity) {
        return new Video(videoEntity.getId(), videoEntity.getFilename(), videoEntity.getTitle(), videoEntity.getLength(), videoEntity.getUploadDate());
    }
}
