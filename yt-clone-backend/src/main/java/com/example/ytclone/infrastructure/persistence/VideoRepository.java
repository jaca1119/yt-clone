package com.example.ytclone.infrastructure.persistence;

import com.example.ytclone.domain.Video;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VideoRepository {

    private List<Video> videos;

    public List<Video> getVideos() {
        return videos;
    }

    public void save(List<Video> videos) {
        this.videos = videos;
    }
}
