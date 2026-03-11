package com.example.ytclone;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
public class YtCloneApplication implements CommandLineRunner {
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(YtCloneApplication.class, args);
    }

    @Override
    public void run(String @NonNull ... args) throws Exception {
        //Load initial data from videos directory in the project
        try (Stream<Path> pathStream = Files.list(Path.of("videos"))) {
            List<Video> videos = pathStream.filter(file -> file.getFileName().toString().endsWith(".mp4"))
                    .map(file -> {
                        ProcessBuilder processBuilder = new ProcessBuilder("ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", file.toAbsolutePath().toString());
                        processBuilder.redirectErrorStream(true);
                        try {
                            Process process = processBuilder.start();
                            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                JsonNode jsonNode = objectMapper.readTree(bufferedReader);
                                String duration = jsonNode.get("format").get("duration").asString();
                                Duration videoDuration = Duration.ofMillis(Math.round(Double.parseDouble(duration) * 1000));

                                String filenameWithoutExtension = file.getFileName().toString().split(".mp4")[0];
                                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-ss", "00:00:01.000", "-i", file.toAbsolutePath().toString(), "-vframes", "1", "videos/thumbnails/%s.jpg".formatted(filenameWithoutExtension));
                                pb.redirectErrorStream(true);
                                //wait for?
                                pb.start();

                                return new Video(UUID.randomUUID(), file.getFileName().toString(), filenameWithoutExtension, videoDuration.getSeconds(), LocalDateTime.now());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();

            videoRepository.save(videos);
        }
    }
}
