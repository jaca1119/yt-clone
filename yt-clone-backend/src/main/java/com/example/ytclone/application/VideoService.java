package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.media.VideoProcessor;
import com.example.ytclone.infrastructure.persistence.*;
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

/*
It shouldn't return objects from the infrastructure. It should throw domain exceptions that are mapped by web layer instead of ResponseStatusException
 */
@Slf4j
@Service
public class VideoService {
    private final Path videosDirectory = Path.of("videos").toAbsolutePath();
    private final Path thumbnailsDirectory = Path.of("videos/thumbnails").toAbsolutePath();
    private final Path previewDirectory = Path.of("videos/preview_thumbnails").toAbsolutePath();
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final VideoProcessor videoProcessor;
    private final UserVideoInteractionRepository userVideoInteractionRepository;
    private final UserCommentInteractionRepository userCommentInteractionRepository;

    public VideoService(VideoRepository videoRepository, CommentRepository commentRepository, VideoProcessor videoProcessor, UserVideoInteractionRepository userVideoInteractionRepository, UserCommentInteractionRepository userCommentInteractionRepository) {
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.videoProcessor = videoProcessor;
        this.userVideoInteractionRepository = userVideoInteractionRepository;
        this.userCommentInteractionRepository = userCommentInteractionRepository;
    }

    public Optional<Video> getVideo(UUID id) {
        return videoRepository.findById(id).map(this::toVideo);
    }

    public List<Video> getVideos() {
        return videoRepository.findAllByFilenameIsNotNullOrderByUploadDate().stream().map(this::toVideo).toList();
    }

    public List<Video> getVideos(String user) {
        return videoRepository.findAllByCreatedBy(user).stream().map(this::toVideo).toList();
    }

    public Optional<Path> getVideoFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> {
                    Path resource = Path.of("videos/%s".formatted(video.getFilename())).toAbsolutePath();
                    if (Files.exists(resource)) {
                        return resource;
                    } else {
                        return null;
                    }
                });
    }

    public Optional<Path> getVideoThumbnailFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> {
                    Path resource = Path.of("videos/thumbnails/%s.jpg".formatted(video.getFilename().split(".mp4")[0])).toAbsolutePath();
                    if (Files.exists(resource)) {
                        return resource;
                    } else {
                        return null;
                    }
                });
    }

    public Optional<Path> getVideoPreviewThumbnailsFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> {
                    Path resource = Path.of("videos/preview_thumbnails/%s.jpg".formatted(video.getFilename().split(".mp4")[0])).toAbsolutePath();
                    if (Files.exists(resource)) {
                        return resource;
                    } else {
                        return null;
                    }
                });
    }

    public Optional<Path> getVideoPreviewThumbnailsVTTFilePath(UUID id) {
        return videoRepository.findById(id)
                .map(video -> {
                    Path resource = Path.of("videos/preview_thumbnails/%s.vtt".formatted(video.getFilename().split(".mp4")[0])).toAbsolutePath();
                    if (Files.exists(resource)) {
                        return resource;
                    } else {
                        return null;
                    }
                });
    }

    @Transactional
    public UUID startVideoUpload(String title, String user, LocalDateTime uploadTime) {
        UUID id = UUID.randomUUID();
        videoRepository.save(new VideoEntity(id, null, title, user, null, null, uploadTime, 0, 0, 0, null, 0));
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

        videoProcessor.generatePreviewThumbnailsSprite(file);
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
                            String filenameWithoutExtension = entity.getFilename().substring(0, entity.getFilename().lastIndexOf(".mp4"));
                            Path thumbnail = thumbnailsDirectory.resolve(filenameWithoutExtension + ".jpg").normalize();
                            Path preview = previewDirectory.resolve(filenameWithoutExtension + ".jpg").normalize();
                            Path previewVTT = previewDirectory.resolve(filenameWithoutExtension + ".vtt").normalize();

                            if (videoFile.toString().contains("..") || thumbnail.toString().contains("..") || preview.toString().contains("..") || previewVTT.toString().contains("..")) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                            }
                            if (!videoFile.toString().contains("/videos/") || !thumbnail.toString().contains("/videos/thumbnails/") || !preview.toString().contains("/videos/preview_thumbnails/") || !previewVTT.toString().contains("/videos/preview_thumbnails/")) {
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

                            if (Files.isRegularFile(preview)) {
                                try {
                                    Files.deleteIfExists(preview);
                                    log.info("Deleted preview {}", id);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            if (Files.isRegularFile(previewVTT)) {
                                try {
                                    Files.deleteIfExists(previewVTT);
                                    log.info("Deleted previewVTT {}", id);
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
        CommentEntity save = commentRepository.save(new CommentEntity(UUID.randomUUID(), comment, videoEntity, optionalComment.orElse(null), 0, 0, LocalDateTime.now(), user, null, 0));
        return save.getId();
    }

    public List<CommentDTO> getNewestCommentsForVideo(UUID videoId, long offset) {
        return videoRepository.findById(videoId)
                .map(v -> commentRepository.findTop10ByVideoOffsetWithReplyCount(v, offset))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<CommentDTO> getNewestRepliesForComment(UUID videoId, UUID parentId, long offset) {
        //TODO could be rewritten to single select for comments with ids instead of multiple selects for entities
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return commentRepository.findByVideoAndParentOrderByCreatedAtDesc(videoEntity, parentId, offset);
    }

    @Transactional
    public void trackView(UUID videoId) {
        //naive implementation, async would be better
        videoRepository.saveView(videoId);
    }

    @Transactional
    public void toggleLike(UUID videoId, String username) {
        VideoEntity video = videoRepository.findById(videoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        toggleRate(video, username, VideoRate.LIKE);
    }

    @Transactional
    public void toggleDislike(UUID videoId, String username) {
        VideoEntity video = videoRepository.findById(videoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        toggleRate(video, username, VideoRate.DISLIKE);
    }

    public UserVideoInteractionEntity getUserInteractionForVideo(UUID videoId, String username) {
        return userVideoInteractionRepository.findByUsernameAndVideoId(username, videoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<UserCommentInteractionEntity> getUserInteractionForComments(String username, List<String> commentsIds) {
        return userCommentInteractionRepository.findAllByUsernameAndCommentIdIn(username, commentsIds.stream().map(UUID::fromString).toList());
    }

    @Transactional
    public void toggleLikeForComment(UUID commentId, String username) {
        CommentEntity commentEntity = commentRepository.findById(commentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        toggleRateForComment(commentEntity, username, CommentRate.LIKE);
    }

    @Transactional
    public void toggleDislikeForComment(UUID commentId, String username) {
        CommentEntity commentEntity = commentRepository.findById(commentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        toggleRateForComment(commentEntity, username, CommentRate.DISLIKE);
    }

    private void toggleRateForComment(CommentEntity comment, String username, CommentRate rate) {
        UserCommentInteractionEntity userCommentInteractionEntity = userCommentInteractionRepository.findByUsernameAndCommentId(username, comment.getId()).orElseGet(() -> new UserCommentInteractionEntity(UUID.randomUUID(), username, comment, new UserCommentInteraction(null)));

        if (userCommentInteractionEntity.getUserCommentInteraction().getRate() == null) {
            if (rate == CommentRate.LIKE) {
                userCommentInteractionEntity.getUserCommentInteraction().setRate(CommentRate.LIKE);
                comment.setLikes(comment.getLikes() + 1);
            } else if (rate == CommentRate.DISLIKE) {
                userCommentInteractionEntity.getUserCommentInteraction().setRate(CommentRate.DISLIKE);
                comment.setDislikes(comment.getDislikes() + 1);
            }
        } else if (userCommentInteractionEntity.getUserCommentInteraction().getRate() == rate) {
            if (rate == CommentRate.LIKE) {
                userCommentInteractionEntity.getUserCommentInteraction().setRate(null);
                comment.setLikes(comment.getLikes() - 1);
            } else if (rate == CommentRate.DISLIKE) {
                userCommentInteractionEntity.getUserCommentInteraction().setRate(null);
                comment.setDislikes(comment.getDislikes() - 1);
            }
        } else {
            if (rate == CommentRate.LIKE) {
                userCommentInteractionEntity.getUserCommentInteraction().setRate(CommentRate.LIKE);
                comment.setLikes(comment.getLikes() + 1);
                comment.setDislikes(comment.getDislikes() - 1);
            } else if (rate == CommentRate.DISLIKE) {
                userCommentInteractionEntity.getUserCommentInteraction().setRate(CommentRate.DISLIKE);
                comment.setLikes(comment.getLikes() - 1);
                comment.setDislikes(comment.getDislikes() + 1);
            }
        }

        commentRepository.save(comment);
        userCommentInteractionRepository.save(userCommentInteractionEntity);
    }

    private void toggleRate(VideoEntity video, String username, VideoRate rate) {
        UserVideoInteractionEntity userVideoInteractionEntity = userVideoInteractionRepository.findByUsernameAndVideoId(username, video.getId()).orElseGet(() -> new UserVideoInteractionEntity(UUID.randomUUID(), username, video, new UserVideoInteraction()));
        if (userVideoInteractionEntity.getUserVideoInteraction().getRate() == null) {
            if (rate == VideoRate.LIKE) {
                userVideoInteractionEntity.getUserVideoInteraction().setRate(VideoRate.LIKE);
                video.setLikes(video.getLikes() + 1);
            } else if (rate == VideoRate.DISLIKE) {
                userVideoInteractionEntity.getUserVideoInteraction().setRate(VideoRate.DISLIKE);
                video.setDislikes(video.getDislikes() + 1);
            }
        } else if (userVideoInteractionEntity.getUserVideoInteraction().getRate() == rate) {
            if (rate == VideoRate.LIKE) {
                userVideoInteractionEntity.getUserVideoInteraction().setRate(null);
                video.setLikes(video.getLikes() - 1);
            } else if (rate == VideoRate.DISLIKE) {
                userVideoInteractionEntity.getUserVideoInteraction().setRate(null);
                video.setDislikes(video.getDislikes() - 1);
            }
        } else {
            if (rate == VideoRate.LIKE) {
                userVideoInteractionEntity.getUserVideoInteraction().setRate(VideoRate.LIKE);
                video.setLikes(video.getLikes() + 1);
                video.setDislikes(video.getDislikes() - 1);
            } else if (rate == VideoRate.DISLIKE) {
                userVideoInteractionEntity.getUserVideoInteraction().setRate(VideoRate.DISLIKE);
                video.setLikes(video.getLikes() - 1);
                video.setDislikes(video.getDislikes() + 1);
            }
        }

        userVideoInteractionRepository.save(userVideoInteractionEntity);
    }

    private Video toVideo(VideoEntity videoEntity) {
        return new Video(videoEntity.getId(), videoEntity.getFilename(), videoEntity.getTitle(), videoEntity.getCreatedBy(), videoEntity.getLength(), videoEntity.getUploadDate(), videoEntity.getViewsCount(), videoEntity.getLikes(), videoEntity.getDislikes());
    }
}
