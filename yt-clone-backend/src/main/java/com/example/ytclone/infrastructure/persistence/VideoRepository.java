package com.example.ytclone.infrastructure.persistence;

import com.example.ytclone.domain.Video;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class VideoRepository {

    public List<Video> getVideos() {
        return List.of(
                new Video(UUID.randomUUID(), Path.of("/tmp"), Path.of("/tmp"), "Video title 1", 96),
                new Video(UUID.randomUUID(), Path.of("/tmp"), Path.of("/tmp"), "Video title 2", 67)
                );
    }
}
