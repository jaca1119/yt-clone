package com.example.ytclone.infrastructure.web;

import com.example.ytclone.application.VideoService;
import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.persistence.CommentDTO;
import com.example.ytclone.infrastructure.persistence.UserCommentInteractionEntity;
import com.example.ytclone.infrastructure.persistence.UserVideoInteractionEntity;
import com.example.ytclone.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//TODO rest controller should return DTO instead of domain object
@Slf4j
@RestController
@RequestMapping("/videos")
public class VideoRestController {

    private final VideoService videoService;

    public VideoRestController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public List<Video> getVideos() {
        return videoService.getVideos();
    }

    @GetMapping("/by-user")
    public List<Video> getVideosByUser(@AuthenticationPrincipal Jwt jwt) {
        log.info("Finding videos by user {}", jwt.getSubject());
        return videoService.getVideos(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> streamVideo(@PathVariable UUID id) {
        Optional<Resource> fileSystemResource = videoService.getVideoHlsPlaylistPath(id).map(FileSystemResource::new);

        return fileSystemResource.map(resource -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/hls/index.m3u8")
    public ResponseEntity<Resource> getVideoHlsPlaylist(@PathVariable UUID id) {
        Optional<Resource> fileSystemResource = videoService.getVideoHlsPlaylistPath(id).map(FileSystemResource::new);
        return fileSystemResource.map(resource -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/hls/{segmentFilename:.+}")
    public ResponseEntity<Resource> getVideoHlsSegment(@PathVariable UUID id, @PathVariable String segmentFilename) {
        Optional<Resource> fileSystemResource = videoService.getVideoHlsAssetPath(id, segmentFilename).map(FileSystemResource::new);
        return fileSystemResource.map(resource -> ResponseEntity.ok()
                .contentType(segmentFilename.endsWith(".ts")
                        ? MediaType.parseMediaType("video/mp2t")
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<Video> getVideo(@PathVariable UUID id) {
        return ResponseEntity.of(videoService.getVideo(id));
    }


    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getVideoThumbnail(@PathVariable UUID id) {
        Optional<Resource> thumbnail = videoService.getVideoThumbnailFilePath(id).map(FileSystemResource::new);
        return ResponseEntity.of(thumbnail);
    }

    @GetMapping(value = "/{id}/preview_thumbnails", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> getVideoPreviewThumbnails(@PathVariable UUID id) {
        Optional<Resource> thumbnail = videoService.getVideoPreviewThumbnailsFilePath(id).map(FileSystemResource::new);
        return ResponseEntity.of(thumbnail);
    }

    @GetMapping(value = "/{id}/preview_thumbnails_vtt", produces = "text/vtt")
    public ResponseEntity<Resource> getVideoPreviewThumbnailsVTT(@PathVariable UUID id) {
        Optional<Resource> thumbnail = videoService.getVideoPreviewThumbnailsVTTFilePath(id).map(FileSystemResource::new);
        return ResponseEntity.of(thumbnail);
    }

    @PostMapping
    public ResponseEntity<VideoUploadResponse> startVideoUpload(@RequestBody @Valid VideoUploadRequest videoUploadRequest, @AuthenticationPrincipal Jwt jwt) {
        UUID id = videoService.startVideoUpload(videoUploadRequest.title(), jwt.getSubject(), LocalDateTime.now());
        return ResponseEntity.created(URI.create("/videos/%s/metadata".formatted(id))).body(new VideoUploadResponse(id));
    }

    @PostMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<UUID> uploadVideo(@PathVariable UUID id, @RequestPart(name = "file") MultipartFile multipartFile, @AuthenticationPrincipal Jwt principal) {

        if (multipartFile.getOriginalFilename() != null && !multipartFile.getOriginalFilename().endsWith(".mp4")) {
            return ResponseEntity.badRequest().build();
        }
        //TODO after file transfer and validation file processing could be done async
        File file = Path.of("videos/%s.mp4".formatted(id)).toAbsolutePath().toFile();
        log.info("uploading video {}, file: {}, by user: {}", id, file.getAbsolutePath(), principal.getSubject());
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        videoService.saveVideoFile(id, file, principal.getSubject());
        log.info("video {} saved successfully", id);

        return ResponseEntity.ok().body(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateVideo(@PathVariable UUID id, @RequestBody VideoUpdateDTO updateDTO, @AuthenticationPrincipal Jwt principal) {

        videoService.updateVideo(id, updateDTO, principal.getSubject());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id, @AuthenticationPrincipal Jwt principal) {
        videoService.deleteVideo(id, principal.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/{videoId}/comments", "/{videoId}/comments/{parentId}"})
    public ResponseEntity<CommentResponse> comment(@PathVariable UUID videoId, @PathVariable Optional<UUID> parentId, @Valid @RequestBody CommentRequest commentRequest, @AuthenticationPrincipal Jwt jwt) {
        UUID commentId = videoService.comment(videoId, commentRequest.comment(), jwt.getSubject(), parentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CommentResponse(commentId));
    }

    @GetMapping("/{videoId}/comments/newest")
    public ResponseEntity<CommentsPageOffset> getNewestComments(@PathVariable UUID videoId, @RequestParam Optional<Long> offset) {
        Instant start = Instant.now();
        List<CommentDTO> comments = videoService.getNewestCommentsForVideo(videoId, offset.orElse(0L));
        log.info("Get newest comments for video: {}, offset: {}, duration: {}", videoId, offset, Duration.between(start, Instant.now()));
        return ResponseEntity.ok(new CommentsPageOffset(comments, comments.size() == 10));
    }

    @GetMapping("/{videoId}/comments/{parentId}/newest")
    public ResponseEntity<CommentsPageOffset> getNewestCommentReplies(@PathVariable UUID videoId, @PathVariable UUID parentId, @RequestParam Optional<Long> offset) {
        Instant start = Instant.now();
        List<CommentDTO> comments = videoService.getNewestRepliesForComment(videoId, parentId, offset.orElse(0L));
        log.info("Get newest replies for video: {}, parent: {}, offset: {}, duration: {}", videoId, parentId, offset, Duration.between(start, Instant.now()));
        return ResponseEntity.ok(new CommentsPageOffset(comments, comments.size() == 10));
    }

    @PostMapping("/{videoId}/views")
    public ResponseEntity<Void> trackView(@PathVariable UUID videoId) {
        videoService.trackView(videoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{videoId}/toggle-like")
    public ResponseEntity<Void> toggleLike(@PathVariable UUID videoId, @AuthenticationPrincipal Jwt jwt) {
        videoService.toggleLike(videoId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{videoId}/toggle-dislike")
    public ResponseEntity<Void> toggleDisLike(@PathVariable UUID videoId, @AuthenticationPrincipal Jwt jwt) {
        videoService.toggleDislike(videoId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{videoId}/user-interactions")
    public ResponseEntity<UserVideoInteractionDTO> getUserInteractionsForVideo(@PathVariable UUID videoId, @AuthenticationPrincipal Jwt jwt) {
        UserVideoInteractionEntity userInteractionForVideo = videoService.getUserInteractionForVideo(videoId, jwt.getSubject());
        return ResponseEntity.ok(new UserVideoInteractionDTO(Optional.ofNullable(userInteractionForVideo.getUserVideoInteraction().getRate())));
    }

    @PostMapping("/{videoId}/comments/user-interactions")
    public ResponseEntity<List<UserCommentInteractionDTO>> getUserInteractionsForComments(@PathVariable UUID videoId, @AuthenticationPrincipal Jwt jwt, @RequestBody List<String> commentsIds) {
        List<UserCommentInteractionEntity> userInteractionForComments = videoService.getUserInteractionForComments(jwt.getSubject(), commentsIds);
        return ResponseEntity.ok(userInteractionForComments.stream().map(c -> new UserCommentInteractionDTO(c.getComment().getId(), c.getUserCommentInteraction().getRate() != null ? c.getUserCommentInteraction().getRate().name() : null)).toList());
    }

    @PostMapping("/{videoId}/comments/{commentId}/toggle-like")
    public ResponseEntity<Void> toggleLike(@PathVariable UUID videoId, @PathVariable UUID commentId, @AuthenticationPrincipal Jwt jwt) {
        videoService.toggleLikeForComment(commentId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{videoId}/comments/{commentId}/toggle-dislike")
    public ResponseEntity<Void> toggleDislike(@PathVariable UUID videoId, @PathVariable UUID commentId, @AuthenticationPrincipal Jwt jwt) {
        videoService.toggleDislikeForComment(commentId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<VideoSearchResponse> searchVideos(@RequestParam("q") String searchQuery) {
        List<Video> videos = videoService.searchVideos(searchQuery);
        return ResponseEntity.ok(new VideoSearchResponse(videos));
    }
}
