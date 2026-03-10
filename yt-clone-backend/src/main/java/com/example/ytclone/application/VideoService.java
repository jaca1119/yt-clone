package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

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

    public Optional<Resource> getVideoResource(UUID id) {
        return videoRepository.getVideos().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .map(video -> new FileSystemResource("videos/%s".formatted(video.getFilename())));
    }

    public Optional<Resource> getVideoThumbnail(UUID id) {
        return videoRepository.getVideos().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .map(video -> new FileSystemResource("videos/thumbnails/%s.jpg".formatted(video.getFilename().split(".mp4")[0])));
    }
}
