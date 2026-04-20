package com.example.ytclone.infrastructure;

import com.example.ytclone.TestcontainersConfiguration;
import com.example.ytclone.infrastructure.persistence.SubscriptionRepository;
import com.example.ytclone.infrastructure.web.dto.VideoUploadRequest;
import com.example.ytclone.infrastructure.web.dto.VideoUploadResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SubscriptionRestControllerTest {
    static File testUploadFile;
    UUID creatorVideoId;

    @Autowired
    MockMvc mvc;
    @Autowired
    MockMvcTester mockMvcTester;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    SubscriptionRepository subscriptionRepository;

    @BeforeAll
    static void setUpFiles() throws IOException, InterruptedException {
        if (!Files.exists(Path.of("videos/tests/smpte.mp4"))) {
            Files.createDirectories(Path.of("videos/tests"));
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-f", "lavfi", "-i", "smptebars", "-t", "30", "videos/tests/smpte.mp4");
            pb.redirectErrorStream(true);
            pb.start().waitFor(Duration.ofSeconds(5));
        }
        testUploadFile = new File("videos/tests/smpte.mp4");
    }

    @BeforeEach
    void setUp() throws Exception {
        creatorVideoId = startVideoUpload("creator");
        uploadVideo(creatorVideoId, "creator");
        subscriptionRepository.deleteAll();
    }

    @AfterEach
    void cleanup() throws Exception {
        deleteVideo(creatorVideoId, "creator");
    }

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mvc.perform(post("/subscriptions/{creatorUsername}/toggle", "creator"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/subscriptions/{creatorUsername}/status", "creator"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldToggleSubscriptionAndReturnStatus() throws Exception {
        mvc.perform(post("/subscriptions/{creatorUsername}/toggle", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true));

        mvc.perform(get("/subscriptions/{creatorUsername}/status", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true));

        mvc.perform(post("/subscriptions/{creatorUsername}/toggle", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false));

        mvc.perform(get("/subscriptions/{creatorUsername}/status", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false));
    }

    @Test
    void shouldCountSubscriptionsForCreator() throws Exception {
        mvc.perform(get("/subscriptions/{creatorUsername}/count", "creator"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        mvc.perform(post("/subscriptions/{creatorUsername}/toggle", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber"))))
                .andExpect(status().isOk());
        mvc.perform(post("/subscriptions/{creatorUsername}/toggle", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber2"))))
                .andExpect(status().isOk());
        mvc.perform(post("/subscriptions/{creatorUsername}/toggle", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber3"))))
                .andExpect(status().isOk());

        mvc.perform(get("/subscriptions/{creatorUsername}/count", "creator"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void shouldRejectSelfSubscription() throws Exception {
        mvc.perform(post("/subscriptions/{creatorUsername}/toggle", "creator")
                        .with(jwt().jwt(jwt -> jwt.subject("creator")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundForMissingCreator() throws Exception {
        mvc.perform(get("/subscriptions/{creatorUsername}/status", "missing-creator")
                        .with(jwt().jwt(jwt -> jwt.subject("subscriber"))))
                .andExpect(status().isNotFound());
    }

    private UUID startVideoUpload(String creator) throws Exception {
        return objectMapper.readValue(mockMvcTester.post().uri("/videos")
                .with(jwt().jwt(jwt -> jwt.subject(creator)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VideoUploadRequest("creator title")))
                .exchange()
                .getResponse().getContentAsString(), VideoUploadResponse.class).videoId();
    }

    private void uploadVideo(UUID videoId, String creator) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", testUploadFile.getName(), "video/mp4", Files.newInputStream(testUploadFile.toPath()));
        mvc.perform(multipart("/videos/{id}", videoId).file(file).with(jwt().jwt(jwt -> jwt.subject(creator))))
                .andExpect(status().isOk());
    }

    private void deleteVideo(UUID videoId, String creator) throws Exception {
        mvc.perform(delete("/videos/{id}", videoId).with(jwt().jwt(jwt -> jwt.subject(creator))))
                .andExpect(status().isNoContent());
    }
}
