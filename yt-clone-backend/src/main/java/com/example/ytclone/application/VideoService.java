package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoService {

    private final VideoRepository videoRepository;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public List<Video> getVideos() {
        return videoRepository.getVideos();
    }
}
