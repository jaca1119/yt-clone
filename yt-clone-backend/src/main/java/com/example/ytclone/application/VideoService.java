package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.media.VideoProcessor;
import com.example.ytclone.infrastructure.persistence.VideoEntity;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import com.example.ytclone.infrastructure.web.VideoUpdateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class VideoService {
    private final Path videosDirectory = Path.of("videos").toAbsolutePath();
    private final Path thumbnailsDirectory = Path.of("videos/thumbnails").toAbsolutePath();
    private final VideoRepository videoRepository;
    private final VideoProcessor videoProcessor;

    public VideoService(VideoRepository videoRepository, VideoProcessor videoProcessor) {
        this.videoRepository = videoRepository;
        this.videoProcessor = videoProcessor;
    }

    public Optional<Video> getVideo(UUID id) {
        return videoRepository.findById(id).map(this::toVideo);
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

    public void saveVideo(UUID id, File file, LocalDateTime uploadDateTime, String creator) {
        try {
            Duration duration = videoProcessor.getDuration(file);
            videoProcessor.generateThumbnail(file, "%s.jpg".formatted(id));
            videoRepository.save(new VideoEntity(id, file.getName(), file.getName(), creator, duration.getSeconds(), uploadDateTime));
        } catch (RuntimeException e) {
            file.delete();
            throw e;
        }
    }

    private Video toVideo(VideoEntity videoEntity) {
        return new Video(videoEntity.getId(), videoEntity.getFilename(), videoEntity.getTitle(), videoEntity.getCreatedBy(), videoEntity.getLength(), videoEntity.getUploadDate());
    }

    @Transactional
    public void updateVideo(UUID id, VideoUpdateDTO updateDTO, String user) {
        videoRepository.findByIdAndCreatedBy(id, user)
                .ifPresentOrElse(videoEntity -> {
                    updateDTO.title().ifPresent(videoEntity::setTitle);

                    videoRepository.save(videoEntity);
                }, () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                });
    }

    @Transactional
    public void deleteVideo(UUID id, String user) {
        log.info("Deleting video: {}, user: {}", id, user);
        videoRepository.findByIdAndCreatedBy(id, user)
                .ifPresentOrElse(
                        entity -> {
                            Path videoFile = videosDirectory.resolve(entity.getFilename()).normalize();
                            Path thumbnail = thumbnailsDirectory.resolve(entity.getFilename().substring(0, entity.getFilename().lastIndexOf(".mp4")) + ".jpg").normalize();

                            if (videoFile.toString().contains("..") || thumbnail.toString().contains("..")) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                            }
                            if (!videoFile.toString().contains("/videos/") || !thumbnail.toString().contains("/videos/thumbnails/")) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                            }

                            log.info("Deleting {}, {}", videoFile, thumbnail);
                            videoRepository.delete(entity);
                            if (Files.isRegularFile(videoFile)) {
                                try {
                                    Files.deleteIfExists(videoFile);
                                    log.info("Deleted video {}", id);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            if (Files.isRegularFile(thumbnail)) {
                                try {
                                    Files.deleteIfExists(thumbnail);
                                    log.info("Deleted thumbnail {}", id);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        },
                        () -> {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                        });
    }
}
