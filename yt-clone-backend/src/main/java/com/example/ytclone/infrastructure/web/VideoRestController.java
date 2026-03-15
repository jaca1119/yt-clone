package com.example.ytclone.infrastructure.web;

import com.example.ytclone.application.VideoService;
import com.example.ytclone.domain.Video;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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

    //Spring handles range automatically
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

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<UUID> uploadVideo(@RequestPart(name = "file") MultipartFile multipartFile, @AuthenticationPrincipal Jwt principal) {

        if (multipartFile.getOriginalFilename() != null && !multipartFile.getOriginalFilename().endsWith(".mp4")) {
            return ResponseEntity.badRequest().build();
        }
        //TODO after file transfer and validation file processing could be done async
        UUID id = UUID.randomUUID();
        File file = Path.of("videos/%s.mp4".formatted(id)).toAbsolutePath().toFile();
        log.info("file: {}", file.getAbsolutePath());
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        videoService.saveVideo(id, file, LocalDateTime.now(), principal.getSubject());

        return ResponseEntity.ok().body(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity updateVideo(@PathVariable UUID id, @RequestBody VideoUpdateDTO updateDTO, @AuthenticationPrincipal Jwt principal) {

        videoService.updateVideo(id, updateDTO, principal.getSubject());
        return ResponseEntity.noContent().build();
    }
}
