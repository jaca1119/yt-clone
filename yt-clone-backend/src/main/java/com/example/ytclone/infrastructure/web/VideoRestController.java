package com.example.ytclone.infrastructure.web;

import com.example.ytclone.application.VideoService;
import com.example.ytclone.domain.Video;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        Optional<Resource> fileSystemResource = videoService.getVideoResource(id);

        return fileSystemResource.map(resource -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource)).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getVideoThumbnail(@PathVariable UUID id) {
        Optional<Resource> thumbnail = videoService.getVideoThumbnail(id);
        return ResponseEntity.of(thumbnail);
    }
}
