package com.example.ytclone;

import com.example.ytclone.infrastructure.persistence.CommentEntity;
import com.example.ytclone.infrastructure.persistence.CommentRepository;
import com.example.ytclone.infrastructure.persistence.VideoEntity;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
public class YtCloneApplication implements ApplicationRunner {
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    CommentRepository commentRepository;
    @Autowired
    ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(YtCloneApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //setup initial dirs
        Path videosDir = Path.of("videos");
        Files.createDirectories(videosDir);
        Files.createDirectories(Path.of("videos/thumbnails"));

        log.info("Args: {}, source: {}", args, Arrays.toString(args.getSourceArgs()));

        //generate videos if argument provided
        if (args.containsOption("generate") && !args.getOptionValues("generate").isEmpty()) {
            int numOfVideosToGenerate = Integer.parseInt(args.getOptionValues("generate").getFirst());
            List<Future<Integer>> futures = new ArrayList<>();
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 1; i <= numOfVideosToGenerate; i++) {
                    int finalI = i;
                    Future<Integer> submit = executorService.submit(() -> {
                        Random random = new Random();
                        String deathColor = String.format("#%06X", random.nextInt(0x1000000));
                        String liveColor = String.format("#%06X", random.nextInt(0x1000000));
                        int time = random.nextInt(60) + 7;
                        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-f", "lavfi", "-i", "life=s=300x200:mold=10:r=60:ratio=0.1:death_color=%s:life_color=%s".formatted(deathColor, liveColor), "-pix_fmt", "yuv420p", "-t", String.valueOf(time), "-n", videosDir.resolve("generated_%s.mp4".formatted(finalI)).toString());
                        try {
                            Process start = pb.start();
                            List<String> strings;
                            try (BufferedReader bufferedReader = start.errorReader()) {
                                strings = bufferedReader.readAllLines();
                            }
                            if (!strings.isEmpty()) {
                                System.err.println(strings);
                            }
                            return start.waitFor();
                        } catch (InterruptedException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    futures.add(submit);
                }

                executorService.shutdown();
                log.info("Executor terminated before timeout: {}", executorService.awaitTermination(60, TimeUnit.SECONDS));

                futures.forEach(f -> {
                    try {
                        log.info("Generation exit code: {}", f.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        //Load initial data from videos directory in the project
        try (Stream<Path> pathStream = Files.list(videosDir)) {
            List<VideoEntity> videos = pathStream.filter(file -> file.getFileName().toString().endsWith(".mp4"))
                    .filter(file -> videoRepository.findByFilename(file.getFileName().toString()).isEmpty())
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

                                log.info("Saving initial video file: {}", file.getFileName());
                                return new VideoEntity(UUID.randomUUID(), file.getFileName().toString(), filenameWithoutExtension, "System", videoDuration.getSeconds(), null, LocalDateTime.now(), 0);
                            }
                        } catch (Exception e) {
                            //skip when exception, filter later
                            return null;
                        }
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));

            videoRepository.saveAll(videos);
        }

        //Add comments
        if (args.containsOption("comments") && !args.getOptionValues("comments").isEmpty()) {
            int numOfComments = Integer.parseInt(args.getOptionValues("comments").getFirst());
            log.info("Adding: {} comments for each video", numOfComments);
            Instant start = Instant.now();
            List<VideoEntity> allVideos = videoRepository.findAll();
            log.info("Get all videos duration: {}", Duration.between(start, Instant.now()));
            start = Instant.now();
            allVideos.forEach(v -> {
                for (int i = 0; i < numOfComments; i++) {
                    commentRepository.save(new CommentEntity(UUID.randomUUID(), "Test content " + i, v, null, 0, 0, LocalDateTime.now(), "system"));
                }
            });
            log.info("Save all comments duration: {}", Duration.between(start, Instant.now()));
        }
    }
}
