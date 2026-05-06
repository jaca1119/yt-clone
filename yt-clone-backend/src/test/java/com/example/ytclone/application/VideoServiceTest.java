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
import java.util.List;
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

    @Test
    void shouldPreserveUpdatedTitleWhenSavingUploadedFileMetadata() {
        // given
        File file = new File("test.mp4");
        LocalDateTime uploadDatetime = LocalDateTime.now();
        UUID id = videoService.startVideoUpload("initial title", "test", uploadDatetime);
        when(videoProcessor.getDuration(any())).thenReturn(Duration.ofSeconds(321));

        // simulate concurrent title update that happened while file upload was still in progress
        videoRepository.findById(id).ifPresent(video -> {
            video.setTitle("updated while uploading");
            videoRepository.save(video);
        });

        // when
        videoService.saveVideoFile(id, file, "test");

        // then
        Optional<Video> video = videoService.getVideo(id);
        assertThat(video).isPresent();
        assertThat(video.get().getTitle()).isEqualTo("updated while uploading");
        assertThat(video.get().getFilename()).isEqualTo(file.getName());
        assertThat(video.get().getLength()).isEqualTo(321L);
    }

    @Test
    void shouldRankPopularOlderVideoAboveFreshUnpopularVideo() {
        // given
        when(videoProcessor.getDuration(any())).thenReturn(Duration.ofSeconds(120));
        LocalDateTime now = LocalDateTime.now();
        UUID olderVideoId = videoService.startVideoUpload("older-popular", "test", now.minusDays(10));
        UUID freshVideoId = videoService.startVideoUpload("fresh-unpopular", "test", now.minusHours(2));
        videoService.saveVideoFile(olderVideoId, new File("older.mp4"), "test");
        videoService.saveVideoFile(freshVideoId, new File("fresh.mp4"), "test");

        videoRepository.findById(olderVideoId).ifPresent(video -> {
            video.setViewsCount(50_000);
            video.setLikes(120);
            video.setDislikes(3);
        });
        videoRepository.findById(freshVideoId).ifPresent(video -> {
            video.setViewsCount(5);
            video.setLikes(0);
            video.setDislikes(0);
        });

        // when
        List<Video> feed = videoService.getPopularFreshVideos(Optional.of(10));

        // then
        assertThat(feed).extracting(Video::getId).contains(olderVideoId, freshVideoId);
        assertThat(feed.getFirst().getId()).isEqualTo(olderVideoId);
    }

    @Test
    void shouldClampFeedLimitToAtLeastOne() {
        // given
        when(videoProcessor.getDuration(any())).thenReturn(Duration.ofSeconds(120));
        UUID firstId = videoService.startVideoUpload("first", "test", LocalDateTime.now().minusDays(1));
        UUID secondId = videoService.startVideoUpload("second", "test", LocalDateTime.now().minusHours(1));
        videoService.saveVideoFile(firstId, new File("first.mp4"), "test");
        videoService.saveVideoFile(secondId, new File("second.mp4"), "test");

        // when
        List<Video> feed = videoService.getPopularFreshVideos(Optional.of(0));

        // then
        assertThat(feed).hasSize(1);
    }
}
