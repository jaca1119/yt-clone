package com.example.ytclone.infrastructure;

import com.example.ytclone.VideoTestUtils;
import com.example.ytclone.infrastructure.media.VideoProcessor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class VideoProcessorTest {
    private final VideoProcessor videoProcessor = new VideoProcessor();
    private static final File VIDEO = VideoTestUtils.generateTestSrcVideoFile();

    @Test
    void shouldGeneratePreviewThumbnailSpriteAndWebVTTForVideo() {
        //when
        CompletableFuture<Boolean> previewThumbnailsGeneration = videoProcessor.generatePreviewThumbnailsSprite(VIDEO);

        //then
        assertThat(previewThumbnailsGeneration).isCompletedWithValue(true);
        //basic comparison of files if matches exactly byte to byte. Might fail on different machine because generated files might be slightly different I guess
        assertThat(Path.of("videos/preview_thumbnails/testsrc.jpg")).hasSameBinaryContentAs(VideoTestUtils.THUMBNAIL_PREVIEW);
        assertThat(Path.of("videos/preview_thumbnails/testsrc.vtt")).hasSameTextualContentAs(VideoTestUtils.PREVIEW_VTT);
    }

    @Test
    void shouldGenerateHlsAssets() throws IOException {
        Path outputDir = Path.of("videos/hls/testsrc");
        if (Files.exists(outputDir)) {
            Files.walk(outputDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        videoProcessor.generateHlsAssets(VIDEO, outputDir.toString());

        assertThat(outputDir.resolve("index.m3u8")).exists();
        assertThat(outputDir.resolve("segment_000.ts")).exists();
    }
}
