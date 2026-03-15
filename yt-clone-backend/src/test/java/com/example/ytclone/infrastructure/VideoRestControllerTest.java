package com.example.ytclone.infrastructure;

import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.web.VideoUpdateDTO;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


//mock server, when resttestclient then security is omitted but when used with mockmvc then it test also security layer
@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc //to configure security chain
public class VideoRestControllerTest {
    @Autowired
    RestTestClient restTestClient;
    @Autowired
    MockMvc mockMvc;
    @Autowired //TODO move to mockmvcTester
    MockMvcTester mockMvcTester;
    @Autowired
    ObjectMapper objectMapper;

    static File testUploadFile;

    @BeforeAll
    static void setUp() throws IOException, InterruptedException {
        //TODO test if already exist then skip
        if (!Files.exists(Path.of("videos/tests/smpte.mp4"))) {
            Files.createDirectories(Path.of("videos/tests"));
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-f", "lavfi", "-i", "smptebars", "-t", "30", "videos/tests/smpte.mp4");
            pb.redirectErrorStream(true);
            pb.start().waitFor(Duration.ofSeconds(5));
        }
        testUploadFile = new File("videos/tests/smpte.mp4");
    }

    @Test
    void shouldGetAllVideos() {
        restTestClient.get().uri("/videos")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Video>>() {
                })
                .value(videos ->
                        assertThat(videos).hasSizeGreaterThanOrEqualTo(4)
                );
    }

    @Test
    void shouldGetVideoMetadata() {
        List<Video> videos = restTestClient.get().uri("/videos")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Video>>() {
                }).returnResult().getResponseBody();
        assertThat(videos).hasSizeGreaterThanOrEqualTo(4);

        restTestClient.get().uri("/videos/{id}/metadata", videos.getFirst().getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Video.class)
                .value(video -> {
                    assertThat(video).isNotNull();
                    assertThat(video.getFilename()).isNotEmpty();
                });
    }

    /**
     * in the browser it will send Range: bytes=0- header which means full file but it will stop reading at some point
     * so server will not send more data. But in test it's easier to just send Range with bytes set like Range: bytes=0-999
     */
    @Test
    void shouldStreamVideoWithRange() {
        List<Video> videos = restTestClient.get().uri("/videos")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Video>>() {
                }).returnResult().getResponseBody();
        assertThat(videos).hasSizeGreaterThanOrEqualTo(4);

        restTestClient.get().uri("/videos/{id}", videos.getFirst().getId())
                .header("Range", "bytes=0-999") //get 1000 bytes, browser send 'bytes=0-' which sends full file but browser stop reading so server stop sending more
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PARTIAL_CONTENT)
                .expectHeader()
                .valueEquals("Accept-Ranges", "bytes")
                .expectHeader()
                .contentType(MediaType.valueOf("video/mp4"))
                .expectHeader()
                .exists("Content-Length")
                .expectHeader()
                .exists("Content-Range")
                .expectBody()
                .consumeWith(response -> assertThat(response.getResponseBody()).hasSize(1000));
    }

    @Test
    void shouldNotUploadVideoAsUnauthenticatedUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "file.xd", MediaType.IMAGE_JPEG_VALUE, "fake bytes".getBytes());
        mockMvc.perform(multipart("/videos").file(file))
                        .andExpect(status().isForbidden());
    }

    @Test
    void shouldUploadVideoForUser() throws Exception {
        String videoId = null;
        try {
            MockMultipartFile file = new MockMultipartFile("file", testUploadFile.getName(), "video/mp4", Files.newInputStream(testUploadFile.toPath()));
            //given 4 initial videos
            assertThat(mockMvcTester.get().uri("/videos"))
                    .bodyJson()
                    .convertTo(InstanceOfAssertFactories.LIST)
                    .hasSizeGreaterThanOrEqualTo(1);

            //when upload file
            String contentId = mockMvc.perform(multipart("/videos").file(file).with(jwt()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String id = contentId.substring(1, contentId.lastIndexOf("\""));
            videoId = id;

            assertThatCode(() -> UUID.fromString(id)).doesNotThrowAnyException();

            //then expect 5 videos
            assertThat(mockMvcTester.get().uri("/videos"))
                    .bodyJson()
                    .convertTo(InstanceOfAssertFactories.LIST)
                    .hasSizeGreaterThanOrEqualTo(2);

            mockMvcTester.get().uri("/videos/{id}/metadata", id)
                    .assertThat()
                    .bodyJson()
                    .convertTo(Video.class)
                    .satisfies(video -> {
                        assertThat(video.getCreator()).isEqualTo("user");
                    });

        } finally {
            if (videoId != null) {
                //cleanup delete created files
                Files.deleteIfExists(Path.of("videos/%s.mp4".formatted(videoId)));
                Files.deleteIfExists(Path.of("videos/thumbnails/%s.jpg".formatted(videoId)));
            }
        }
    }

    @Test
    void shouldNotAuthorizeWhenNotValidJwtAlgNone() throws Exception {
        String jwtTokenAlgNone = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.";
        MockMultipartFile file = new MockMultipartFile("file", "file.xd", MediaType.IMAGE_JPEG_VALUE, "fake bytes".getBytes());

        mockMvc.perform(multipart("/videos").file(file).header("Authorization", "Bearer " + jwtTokenAlgNone))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateVideoTitle() throws IOException {
            UUID id = null;
        try {
            id = uploadVideo();

            Video videoBeforeUpdate = restTestClient.get().uri("/videos/{id}/metadata", id)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Video.class)
                    .returnResult().getResponseBody();


            String newTitle = "new Title";
            mockMvc.perform(put("/videos/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VideoUpdateDTO(Optional.of(newTitle))))
                            .with(jwt()))
                            .andExpect(status().isNoContent());

            Video videoAfterUpdate = restTestClient.get().uri("/videos/{id}/metadata", id)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Video.class)
                    .returnResult().getResponseBody();

            assertThat(videoBeforeUpdate.getTitle()).isNotEqualTo(videoAfterUpdate.getTitle());
            assertThat(videoAfterUpdate.getTitle()).isEqualTo(newTitle);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (id != null) {
                //cleanup delete created files
                Files.deleteIfExists(Path.of("videos/%s.mp4".formatted(id)));
                Files.deleteIfExists(Path.of("videos/thumbnails/%s.jpg".formatted(id)));
            }
        }
    }

    @Test
    void shouldNotUpdateVideoTitleOfDifferentUser() throws IOException {
        UUID id = null;
        try {
            id = uploadVideo();

            Video videoBeforeUpdate = restTestClient.get().uri("/videos/{id}/metadata", id)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Video.class)
                    .returnResult().getResponseBody();


            String newTitle = "new Title";
            String differentUser = "different user";
            mockMvc.perform(put("/videos/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VideoUpdateDTO(Optional.of(newTitle))))
                            .with(jwt().jwt((jwt) -> jwt.subject(differentUser))))
                    .andExpect(status().isNotFound());

            Video videoAfterUpdate = restTestClient.get().uri("/videos/{id}/metadata", id)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Video.class)
                    .returnResult().getResponseBody();

            assertThat(videoBeforeUpdate.getTitle()).isEqualTo(videoAfterUpdate.getTitle());

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (id != null) {
                //cleanup delete created files
                Files.deleteIfExists(Path.of("videos/%s.mp4".formatted(id)));
                Files.deleteIfExists(Path.of("videos/thumbnails/%s.jpg".formatted(id)));
            }
        }
    }


    UUID uploadVideo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", testUploadFile.getName(), "video/mp4", Files.newInputStream(testUploadFile.toPath()));
        String contentId = mockMvc.perform(multipart("/videos").file(file).with(jwt()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(contentId.substring(1, contentId.lastIndexOf("\"")));

    }

}
