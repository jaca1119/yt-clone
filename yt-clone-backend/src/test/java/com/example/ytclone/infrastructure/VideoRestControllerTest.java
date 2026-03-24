package com.example.ytclone.infrastructure;

import com.example.ytclone.TestcontainersConfiguration;
import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.web.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Import(TestcontainersConfiguration.class)
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
    UUID videoId;

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

    @BeforeEach
    void setUpTest() throws Exception {
        videoId = startVideoUpload();
        uploadVideo(videoId);
    }

    @AfterEach
    void cleanupTest() {
        deleteVideo(videoId);
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
                        assertThat(videos).hasSizeGreaterThanOrEqualTo(1)
                );
    }

    @Test
    void shouldGetVideoMetadata() {
        restTestClient.get().uri("/videos/{id}/metadata", videoId)
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
        restTestClient.get().uri("/videos/{id}", videoId)
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
        //given
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "file.xd", MediaType.IMAGE_JPEG_VALUE, "fake bytes".getBytes());

        List<Video> videos = restTestClient.get().uri("/videos").exchange().returnResult(new ParameterizedTypeReference<List<Video>>(){}).getResponseBody();
        assertThat(videos).hasSizeGreaterThanOrEqualTo(1);

        //when
        mockMvc.perform(multipart("/videos/{id}", id).file(file))
                .andExpect(status().isUnauthorized());

        //then
        assertThat(mockMvcTester.get().uri("/videos"))
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.LIST)
                .hasSize(videos.size());
    }

    @Test
    void shouldUploadVideoForUser() throws Exception {
        String initialTitle = "test title";
        MockMultipartFile file = new MockMultipartFile("file", testUploadFile.getName(), "video/mp4", Files.newInputStream(testUploadFile.toPath()));
        //given initial videos
        List<Video> videos = restTestClient.get().uri("/videos").exchange().returnResult(new ParameterizedTypeReference<List<Video>>(){}).getResponseBody();
        assertThat(videos).hasSizeGreaterThanOrEqualTo(1);
        //when start video upload
        VideoUploadResponse videoUploadResponse = objectMapper.readValue(mockMvcTester.post().uri("/videos")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VideoUploadRequest(initialTitle)))
                .exchange().getResponse().getContentAsString(), VideoUploadResponse.class);

        //when upload file
        mockMvc.perform(multipart("/videos/{id}", videoUploadResponse.videoId()).file(file).with(jwt()))
                .andExpect(status().isOk());

        //then expect videos + 1
        assertThat(mockMvcTester.get().uri("/videos"))
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.LIST)
                .hasSize(videos.size() + 1);

        mockMvcTester.get().uri("/videos/{id}/metadata", videoUploadResponse.videoId())
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> {
                    assertThat(video.getCreator()).isEqualTo("user");
                    assertThat(video.getTitle()).isEqualTo(initialTitle);
                });

        //cleanup
        deleteVideo(videoUploadResponse.videoId());
    }

    @Test
    void shouldNotAuthorizeWhenNotValidJwtAlgNone() throws Exception {
        String jwtTokenAlgNone = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.";
        MockMultipartFile file = new MockMultipartFile("file", "file.xd", MediaType.IMAGE_JPEG_VALUE, "fake bytes".getBytes());

        UUID id = UUID.randomUUID();
        mockMvc.perform(multipart("/videos/{id}", id).file(file).header("Authorization", "Bearer " + jwtTokenAlgNone))
                .andExpect(status().isUnauthorized());

        //ugly hack. Test is too fast and thumbnail isn't generated before deleting. This ensures it will be deleted and not trash dev file system
        Thread.sleep(100);
    }

    @Test
    void shouldUpdateVideoTitle() throws Exception {
        //given
        String newTitle = "new Title";
        Video videoBeforeUpdate = restTestClient.get().uri("/videos/{id}/metadata", videoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Video.class)
                .returnResult().getResponseBody();

        //when
        mockMvc.perform(put("/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VideoUpdateDTO(Optional.of(newTitle))))
                        .with(jwt()))
                .andExpect(status().isNoContent());

        Video videoAfterUpdate = restTestClient.get().uri("/videos/{id}/metadata", videoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Video.class)
                .returnResult().getResponseBody();

        assertThat(videoBeforeUpdate.getTitle()).isNotEqualTo(videoAfterUpdate.getTitle());
        assertThat(videoAfterUpdate.getTitle()).isEqualTo(newTitle);
    }

    @Test
    void shouldNotUpdateVideoTitleOfDifferentUser() throws Exception {

        Video videoBeforeUpdate = restTestClient.get().uri("/videos/{id}/metadata", videoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Video.class)
                .returnResult().getResponseBody();


        String newTitle = "new Title";
        String differentUser = "different user";
        mockMvc.perform(put("/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VideoUpdateDTO(Optional.of(newTitle))))
                        .with(jwt().jwt((jwt) -> jwt.subject(differentUser))))
                .andExpect(status().isNotFound());

        Video videoAfterUpdate = restTestClient.get().uri("/videos/{id}/metadata", videoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Video.class)
                .returnResult().getResponseBody();

        assertThat(videoBeforeUpdate.getTitle()).isEqualTo(videoAfterUpdate.getTitle());
    }

    @Test
    void shouldDeleteUserVideo() throws Exception {
        //given
        UUID id = startVideoUpload();
        uploadVideo(id);

        //when
        mockMvcTester.delete().uri("/videos/{id}", id)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        //then
        mockMvcTester.get().uri("/videos/{id}/metadata", id)
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);
        mockMvcTester.get().uri("/videos/{id}", id)
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteVideoOfDifferentUser() throws Exception {
        //when
        mockMvcTester.delete().uri("/videos/{id}", videoId)
                .with(jwt().jwt(jwt -> jwt.subject("different user")))
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);

        //then
        mockMvcTester.get().uri("/videos/{id}/metadata", videoId)
                .assertThat()
                .hasStatusOk();
        mockMvcTester.get().uri("/videos/{id}", videoId)
                .assertThat()
                .hasStatusOk();
    }

    @Test
    void shouldCreateVideoUpload() throws UnsupportedEncodingException {
        MvcTestResult result = mockMvcTester.post().uri("/videos")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VideoUploadRequest("title")))
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .convertTo(VideoUploadResponse.class)
                .satisfies(videoUploadResponse -> {
                    assertThat(videoUploadResponse.videoId()).isNotNull();
                });

        //cleanup
        deleteVideo(objectMapper.readValue(result.getResponse().getContentAsString(), VideoUploadResponse.class).videoId());
    }

    @Test
    void shouldNotShowStartedButNotUploadedYetVideo() {
        UUID id = startVideoUpload();

        restTestClient.get().uri("/videos")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Video>>() {
                })
                .value(videos -> {
                            assertThat(videos).hasSizeGreaterThanOrEqualTo(1);
                            assertThat(videos).extracting(Video::getId).doesNotContain(id);
                        }
                );

        //cleanup
        deleteVideo(id);
    }

    @Test
    void shouldGetAllVideosUploadedByUser() {
        String differentUser = "different user";

        //given all videos
        List<Video> videos = restTestClient.get().uri("/videos")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Video>>() {
                }).returnResult().getResponseBody();

        //when upload video by different user
        UUID id = startVideoUpload(differentUser);
        uploadVideo(id, differentUser);

        //then default user should have 1 video
        mockMvcTester.get().uri("/videos/by-user")
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.LIST)
                .hasSize(1);

        //different user should have 1 video
        mockMvcTester.get().uri("/videos/by-user")
                .with(jwt().jwt(jwt -> jwt.subject(differentUser)))
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.LIST)
                .hasSize(1);

        //all videos should be initial + 1
        mockMvcTester.get().uri("/videos")
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.LIST)
                .hasSize(videos.size() + 1);

        //cleanup
        deleteVideo(id, differentUser);
    }

    @Test
    void shouldAddCommentToVideo() throws UnsupportedEncodingException {
        CommentResponse createdComment = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments", videoId)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);

        assertThat(createdComment).isNotNull();
        assertThat(createdComment.commentId()).isGreaterThan(0);


        CommentResponse responseComment = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments/{parentId}", videoId, createdComment.commentId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test response to comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);

        assertThat(responseComment).isNotNull();
        assertThat(responseComment.commentId()).isGreaterThan(0);
        assertThat(responseComment.commentId()).isNotEqualTo(createdComment.commentId());
    }

    UUID startVideoUpload() {
        return startVideoUpload("user");
    }

    UUID startVideoUpload(String user) {
        try {
            VideoUploadResponse videoUploadResponse = objectMapper.readValue(mockMvcTester.post().uri("/videos")
                    .with(jwt().jwt(jwt -> jwt.subject(user)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new VideoUploadRequest("test title")))
                    .exchange().getResponse().getContentAsString(), VideoUploadResponse.class);
            return videoUploadResponse.videoId();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    void uploadVideo(UUID id) {
        uploadVideo(id, "user");
    }

    void uploadVideo(UUID id, String user) {
        try {
            MockMultipartFile file = new MockMultipartFile("file", testUploadFile.getName(), "video/mp4", Files.newInputStream(testUploadFile.toPath()));
            mockMvc.perform(multipart("/videos/{id}", id).file(file).with(jwt().jwt(jwt -> jwt.subject(user))))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void deleteVideo(UUID id) {
        deleteVideo(id, "user");
    }

    void deleteVideo(UUID id, String user) {
        mockMvcTester.delete().uri("/videos/{id}", id)
                .with(jwt().jwt(jwt -> jwt.subject(user)))
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);
    }
}
