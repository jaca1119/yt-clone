package com.example.ytclone.infrastructure.web;

import com.example.ytclone.application.VideoService;
import com.example.ytclone.domain.Video;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

}
