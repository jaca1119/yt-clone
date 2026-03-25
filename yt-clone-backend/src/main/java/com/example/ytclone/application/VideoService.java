package com.example.ytclone.application;

import com.example.ytclone.domain.Comment;
import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.media.VideoProcessor;
import com.example.ytclone.infrastructure.persistence.CommentEntity;
import com.example.ytclone.infrastructure.persistence.CommentRepository;
import com.example.ytclone.infrastructure.persistence.VideoEntity;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import com.example.ytclone.infrastructure.web.dto.CommentsPageOffset;
import com.example.ytclone.infrastructure.web.dto.VideoUpdateDTO;
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
    private final CommentRepository commentRepository;
    private final VideoProcessor videoProcessor;

    public VideoService(VideoRepository videoRepository, CommentRepository commentRepository, VideoProcessor videoProcessor) {
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.videoProcessor = videoProcessor;
    }

    public Optional<Video> getVideo(UUID id) {
        return videoRepository.findById(id).map(this::toVideo);
    }

    public List<Video> getVideos() {
        return videoRepository.findAllByFilenameIsNotNull().stream().map(this::toVideo).toList();
    }

    public List<Video> getVideos(String user) {
        return videoRepository.findAllByCreatedBy(user).stream().map(this::toVideo).toList();
    }

    public Optional<Path> getVideoFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> Path.of("videos/%s".formatted(video.getFilename())).toAbsolutePath());
    }

    public Optional<Path> getVideoThumbnailFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> Path.of("videos/thumbnails/%s.jpg".formatted(video.getFilename().split(".mp4")[0])).toAbsolutePath());
    }

    @Transactional
    public UUID startVideoUpload(String title, String user, LocalDateTime uploadTime) {
        UUID id = UUID.randomUUID();
        videoRepository.save(new VideoEntity(id, null, title, user, null, null, uploadTime, 0));
        return id;
    }

    public void saveVideoFile(UUID id, File file, String creator) {
        videoRepository.findByIdAndCreatedBy(id, creator)
                .ifPresentOrElse(videoEntity -> {
                    try {
                        Duration duration = videoProcessor.getDuration(file);
                        videoProcessor.generateThumbnail(file, "%s.jpg".formatted(id));
                        videoEntity.setFilename(file.getName());
                        videoEntity.setLength(duration.getSeconds());
                        videoRepository.save(videoEntity);
                    } catch (RuntimeException e) {
                        file.delete();
                        throw e;
                    }
                }, () -> {
                    file.delete();
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                });
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
                            videoRepository.delete(entity);

                            if (entity.getFilename() == null) {
                                return;
                            }

                            Path videoFile = videosDirectory.resolve(entity.getFilename()).normalize();
                            Path thumbnail = thumbnailsDirectory.resolve(entity.getFilename().substring(0, entity.getFilename().lastIndexOf(".mp4")) + ".jpg").normalize();

                            if (videoFile.toString().contains("..") || thumbnail.toString().contains("..")) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                            }
                            if (!videoFile.toString().contains("/videos/") || !thumbnail.toString().contains("/videos/thumbnails/")) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                            }

                            log.info("Deleting {}, {}", videoFile, thumbnail);
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

    @Transactional
    public UUID comment(UUID videoId, String comment, String user, Optional<UUID> parentId) {
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Optional<CommentEntity> optionalComment = parentId.map(id -> commentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found")));
        CommentEntity save = commentRepository.save(new CommentEntity(UUID.randomUUID(), comment, videoEntity, optionalComment.orElse(null), 0, 0, LocalDateTime.now(), user));
        return save.getId();
    }

    public CommentsPageOffset getNewestCommentsForVideo(UUID videoId, long offset) {
        return videoRepository.findById(videoId)
                .map(v -> {
                    List<Comment> comments = commentRepository.findTop10ByVideoOffset(v, offset).stream().map(this::toComment).toList();
                    return new CommentsPageOffset(comments, comments.size() == 10);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Video toVideo(VideoEntity videoEntity) {
        return new Video(videoEntity.getId(), videoEntity.getFilename(), videoEntity.getTitle(), videoEntity.getCreatedBy(), videoEntity.getLength(), videoEntity.getUploadDate(), videoEntity.getViewsCount());
    }

    private Comment toComment(CommentEntity commentEntity) {
        return new Comment(commentEntity.getId(), commentEntity.getContent(), commentEntity.getLikes(), commentEntity.getDislikes(), commentEntity.getCreatedBy(), commentEntity.getCreatedAt());
    }

    @Transactional
    public void trackView(UUID videoId) {
        //naive implementation
        videoRepository.saveView(videoId);
    }
}
