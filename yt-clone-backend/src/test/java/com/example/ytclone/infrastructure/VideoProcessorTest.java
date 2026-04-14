package com.example.ytclone.infrastructure;

import com.example.ytclone.VideoTestUtils;
import com.example.ytclone.infrastructure.media.VideoProcessor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class VideoProcessorTest {
    private final VideoProcessor videoProcessor = new VideoProcessor();
    private static final File VIDEO = VideoTestUtils.generateTestSrcVideoFile();

    @Test
    void shouldGeneratePreviewThumbnailSpriteForVideo() {
        //when
        CompletableFuture<Boolean> previewThumbnailsGeneration = videoProcessor.generatePreviewThumbnailsSprite(VIDEO);

        //then
        assertThat(previewThumbnailsGeneration).isCompletedWithValue(true);
        //basic comparison of files if matches exactly byte to byte. Might fail on different machine because generated files might be slightly different I guess
        assertThat(new File("videos/preview_thumbnails/testsrc.jpg")).hasSameBinaryContentAs(VideoTestUtils.THUMBNAIL_PREVIEW);
    }
}
