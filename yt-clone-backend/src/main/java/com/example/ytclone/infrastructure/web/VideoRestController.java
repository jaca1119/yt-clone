package com.example.ytclone.infrastructure.web;

import com.example.ytclone.application.VideoService;
import com.example.ytclone.domain.Comment;
import com.example.ytclone.domain.Video;
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

    //Spring handles range automatically
    //TODO Browser (e.g. firefox) sends range for full data and stops consuming if has enough data. Test if spring load full file, or send it in chunks
    @GetMapping("/{id}")
    public ResponseEntity<Resource> streamVideo(@PathVariable UUID id) {
        Optional<Resource> fileSystemResource = videoService.getVideoFilePath(id).map(FileSystemResource::new);

        return fileSystemResource.map(resource -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
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

    @PostMapping
    public ResponseEntity<VideoUploadResponse> startVideoUpload(@RequestBody VideoUploadRequest videoUploadRequest, @AuthenticationPrincipal Jwt jwt) {
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

        return ResponseEntity.ok().body(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity updateVideo(@PathVariable UUID id, @RequestBody VideoUpdateDTO updateDTO, @AuthenticationPrincipal Jwt principal) {

        videoService.updateVideo(id, updateDTO, principal.getSubject());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deleteVideo(@PathVariable UUID id, @AuthenticationPrincipal Jwt principal) {
        videoService.deleteVideo(id, principal.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/{videoId}/comments", "/{videoId}/comments/{parentId}"})
    public ResponseEntity<CommentResponse> comment(@PathVariable UUID videoId, @PathVariable Optional<Long> parentId, @RequestBody CommentRequest commentRequest, @AuthenticationPrincipal Jwt jwt) {
        long commentId = videoService.comment(videoId, commentRequest.comment(), jwt.getSubject(), parentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CommentResponse(commentId));
    }

    @GetMapping("/{videoId}/comments/newest")
    public ResponseEntity<List<Comment>> getNewestComments(@PathVariable UUID videoId, @RequestParam Optional<Long> offset) {
        List<Comment> newestCommentsForVideo = videoService.getNewestCommentsForVideo(videoId, offset.orElse(0L));
        return ResponseEntity.ok(newestCommentsForVideo);
    }
}
