package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class VideoServiceTest {

    VideoRepository videoRepository = Mockito.mock();
    VideoService videoService = new VideoService(videoRepository);

    //TOOD should firstly upload video and then test
    @Test
    void shouldReturnPathToVideoFile() {
        //given
        UUID id = UUID.randomUUID();
        when(videoRepository.getVideos()).thenReturn(List.of(new Video(id, "test.mp4", "test", 1, LocalDateTime.now())));

        //when
        Optional<Path> videoFilePath = videoService.getVideoFilePath(id);

        //then
        assertThat(videoFilePath).isPresent();
        assertThat(videoFilePath.get().toString()).contains("/videos/").endsWith(".mp4");
    }

    @Test
    void shouldReturnPathToVideoThumbnail() {
        //given
        UUID id = UUID.randomUUID();
        when(videoRepository.getVideos()).thenReturn(List.of(new Video(id, "test.mp4", "test", 1, LocalDateTime.now())));

        //when
        Optional<Path> videoThumbnailFilePath = videoService.getVideoThumbnailFilePath(id);

        //then
        assertThat(videoThumbnailFilePath).isPresent();
        assertThat(videoThumbnailFilePath.get().toString()).contains("/videos/thumbnails").endsWith(".jpg");
    }
}
