package com.example.ytclone.application;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.media.VideoProcessor;
import com.example.ytclone.infrastructure.persistence.CommentRepository;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class VideoServiceTest {

    VideoRepository videoRepository = new InMemoryVideoRepository();
    CommentRepository commentRepository = Mockito.mock();
    VideoProcessor videoProcessor = Mockito.mock();
    VideoService videoService = new VideoService(videoRepository, commentRepository, videoProcessor, Mockito.mock(), Mockito.mock());

    @Test
    void shouldSaveVideoFileFileAndGenerateThumbnail() {
        //given
        File file = new File("test.mp4");
        LocalDateTime uploadDatetime = LocalDateTime.now();
        String initialTitle = "test title";
        UUID id = videoService.startVideoUpload(initialTitle, "test", uploadDatetime);
        videoService.saveVideoFile(id, new File("test.mp4"), "test");
        when(videoProcessor.getDuration(any())).thenReturn(Duration.ofSeconds(123));

        //when
        videoService.saveVideoFile(id, file, "test");

        //then
        Optional<Video> video = videoService.getVideo(id);
        assertThat(video).isPresent();
        assertThat(video.get()).isEqualTo(new Video(id, file.getName(), initialTitle, "test", 123L, uploadDatetime, 0, 0, 0));
    }
}
