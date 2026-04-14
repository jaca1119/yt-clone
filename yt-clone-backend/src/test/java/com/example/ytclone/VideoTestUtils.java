package com.example.ytclone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class VideoTestUtils {
    public static final File THUMBNAIL_PREVIEW = new File("src/test/resources/expected_thumbanil_preview.jpg");
    /**
     * Generate file with color bars and time counter
     * @return generated file
     */
    public static File generateTestSrcVideoFile() {
        File file = new File("videos/tests/testsrc.mp4");

        if (!file.exists()) {
            try {
                Files.createDirectories(Path.of("videos/tests"));
                String duration = "154"; //154s = 2min 34sec
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-f", "lavfi", "-i", "testsrc", "-t", duration, "-g", "25", "-keyint_min", "25", "-pix_fmt", "yuv420p", file.getPath());
                pb.redirectErrorStream(true);
                pb.start().waitFor(Duration.ofSeconds(5));
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }
}
