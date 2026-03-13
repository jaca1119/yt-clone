package com.example.ytclone.application;

import com.example.ytclone.infrastructure.media.VideoProcessor;
import com.example.ytclone.infrastructure.persistence.VideoEntity;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class VideoServiceTest {

    VideoRepository videoRepository = new InMemoryVideoRepository();
    VideoProcessor videoProcessor = Mockito.mock();
    VideoService videoService = new VideoService(videoRepository, videoProcessor);

    @Test
    void shouldReturnPathToVideoFile() {
        //given
        UUID id = UUID.randomUUID();
        videoService.saveVideo(id, new File("test.mp4"), LocalDateTime.now());

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
        videoService.saveVideo(id, new File("test.mp4"), LocalDateTime.now());

        //when
        Optional<Path> videoThumbnailFilePath = videoService.getVideoThumbnailFilePath(id);

        //then
        assertThat(videoThumbnailFilePath).isPresent();
        assertThat(videoThumbnailFilePath.get().toString()).contains("/videos/thumbnails").endsWith(".jpg");
    }

    @Test
    void shouldSaveVideoFileAndGenerateThumbnail() {
        //given
        UUID id = UUID.randomUUID();
        File file = new File("test.mp4");
        LocalDateTime uploadDatetime = LocalDateTime.now();
        when(videoProcessor.getDuration(any())).thenReturn(Duration.ofSeconds(123));

        //when
        videoService.saveVideo(id, file, uploadDatetime);

        //then
        Optional<VideoEntity> videoEntity = videoRepository.findById(id);
        assertThat(videoEntity).isPresent();
        assertThat(videoEntity.get()).isEqualTo(new VideoEntity(id, file.getName(), file.getName(), 123, uploadDatetime));
    }
}
