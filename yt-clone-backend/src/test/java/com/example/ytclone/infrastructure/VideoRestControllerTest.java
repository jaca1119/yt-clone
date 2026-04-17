package com.example.ytclone.infrastructure;

import com.example.ytclone.AsyncTestConfig;
import com.example.ytclone.TestcontainersConfiguration;
import com.example.ytclone.VideoTestUtils;
import com.example.ytclone.domain.Video;
import com.example.ytclone.infrastructure.persistence.CommentDTO;
import com.example.ytclone.infrastructure.persistence.VideoRate;
import com.example.ytclone.infrastructure.web.dto.*;
import org.assertj.core.api.Condition;
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
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Import({TestcontainersConfiguration.class, AsyncTestConfig.class})
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
    UUID videoId2;

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
        videoId2 = startVideoUpload();
        uploadVideo(videoId2);
    }

    @AfterEach
    void cleanupTest() {
        deleteVideo(videoId);
        deleteVideo(videoId2);
    }

    @Test
    void shouldGetAllVideos() {
        restTestClient.get().uri("/videos")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Video>>() {
                })
                .value(videos -> {
                            assertThat(videos).hasSizeGreaterThanOrEqualTo(1);
                            assertThat(videos).isSortedAccordingTo(Comparator.comparing(Video::getUploadDate));
                        }
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

    @Test
    void shouldGetVideoThumbnail() {
        ExchangeResult exchangeResult = restTestClient.get().uri("/videos/{id}/thumbnail", videoId)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult();

        assertThat(exchangeResult.getResponseBodyContent()).isNotEmpty();
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

        List<Video> videos = restTestClient.get().uri("/videos").exchange().returnResult(new ParameterizedTypeReference<List<Video>>() {
        }).getResponseBody();
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
    void shouldUploadVideoForUserAndGenerateThumbnailPreview() throws Exception {
        String initialTitle = "test title";
        MockMultipartFile file = new MockMultipartFile("file", testUploadFile.getName(), "video/mp4", Files.newInputStream(testUploadFile.toPath()));
        //given initial videos
        List<Video> videos = restTestClient.get().uri("/videos").exchange().returnResult(new ParameterizedTypeReference<List<Video>>() {
        }).getResponseBody();
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

        //then expect preview thumbnail file is generated
        assertThat(Path.of("videos/preview_thumbnails/%s.jpg".formatted(videoUploadResponse.videoId()))).exists();

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

        //then default user should have 2 videos
        mockMvcTester.get().uri("/videos/by-user")
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.LIST)
                .hasSize(2);

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
    void shouldAddCommentToVideoAndReplyToComment() throws UnsupportedEncodingException {
        CommentResponse createdComment = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments", videoId)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);

        assertThat(createdComment).isNotNull();
        assertThat(createdComment.commentId()).isNotNull();

        CommentResponse responseComment = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments/{parentId}", videoId, createdComment.commentId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test response to comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);

        CommentResponse responseComment2 = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments/{parentId}", videoId, createdComment.commentId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test response 2 to comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);

        assertThat(responseComment).isNotNull();
        assertThat(responseComment.commentId()).isNotNull();
        assertThat(responseComment.commentId()).isNotEqualTo(createdComment.commentId());
        assertThat(responseComment2).isNotNull();
        assertThat(responseComment2.commentId()).isNotNull();
        assertThat(responseComment2.commentId()).isNotEqualTo(createdComment.commentId());

        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> {
                    assertThat(c.comments()).hasSize(1);
                    assertThat(c.comments().getFirst().replyCount()).isEqualTo(2);
                });
    }

    @Test
    void shouldPaginateComments() {
        //given no comments
        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(0));

        //when
        List<UUID> comments = createComments();

        //then get latest 10 comments
        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(10))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .map(CommentDTO::id)
                .containsExactly(comments.reversed().stream().limit(10).toArray(UUID[]::new));

        //then get next page of latest 10 comments
        mockMvcTester.get().uri("/videos/{videoId}/comments/newest?offset={offset}", videoId, 10)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(10))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .map(CommentDTO::id)
                .containsExactly(comments.reversed().stream().skip(10).limit(10).toArray(UUID[]::new));
    }

    @Test
    void shouldGetCommentReplies() throws UnsupportedEncodingException {
        //Create comment
        CommentResponse createdComment = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments", videoId)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);

        //add replies
        CommentResponse replyResponse = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments/{parentId}", videoId, createdComment.commentId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test response to comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);
        CommentResponse replyResponse2 = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments/{parentId}", videoId, createdComment.commentId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test response 2 to comment")))
                .exchange()
                .getResponse().getContentAsString(), CommentResponse.class);

        //Create different comments with reply
        List<UUID> comments = createComments(5);
        mockMvcTester.post().uri("/videos/{id}/comments/{parentId}", videoId, comments.getFirst())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentRequest("Test response to comment")))
                .assertThat()
                .hasStatus(HttpStatus.CREATED);

        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> {
                    assertThat(c.comments()).hasSize(6);
                    assertThat(c.comments().getLast().replyCount()).isEqualTo(2);
                    assertThat(c.comments().reversed().stream().skip(1)).haveExactly(1, new Condition<>(commentDTO -> commentDTO.replyCount() == 1, ""));
                    assertThat(c.comments().reversed().stream().skip(1)).haveExactly(4, new Condition<>(commentDTO -> commentDTO.replyCount() == 0, ""));
                });

        //get replies
        mockMvcTester.get().uri("/videos/{videoId}/comments/{parentId}/newest", videoId, createdComment.commentId())
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(2).noneMatch(r -> r.replyCount() != 0))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .map(CommentDTO::id)
                .containsExactly(replyResponse2.commentId(), replyResponse.commentId());
    }

    @Test
    void shouldTrackVideoView() {
        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getViewsCount()).isZero());

        mockMvcTester.post().uri("/videos/{videoId}/views", videoId)
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getViewsCount()).isOne());
    }

    @Test
    void shouldNotLikeVideoAsUnauthenticated() {
        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getLikes()).isZero());

        mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                .assertThat()
                .hasStatus(HttpStatus.UNAUTHORIZED);

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getLikes()).isZero());
    }

    @Test
    void shouldToggleLikeAndDislikeOnVideo() {

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getLikes()).isZero());

        mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getLikes()).isOne());

        mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getLikes()).isZero());

        //like again
        mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        //add ten likes from different users
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                    .with(jwt().jwt(jwt -> jwt.subject("user" + finalI)))
                    .assertThat()
                    .hasStatus(HttpStatus.OK);
        }

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> {
                    assertThat(video.getLikes()).isEqualTo(11);
                    assertThat(video.getDislikes()).isZero();
                });

        mockMvcTester.post().uri("/videos/{videoId}/toggle-dislike", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> {
                    assertThat(video.getLikes()).isEqualTo(10);
                    assertThat(video.getDislikes()).isOne();
                });

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            mockMvcTester.post().uri("/videos/{videoId}/toggle-dislike", videoId)
                    .with(jwt().jwt(jwt -> jwt.subject("user" + finalI)))
                    .assertThat()
                    .hasStatus(HttpStatus.OK);
        }

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> {
                    assertThat(video.getLikes()).isEqualTo(5);
                    assertThat(video.getDislikes()).isEqualTo(6);
                });
    }

    @Test
    void shouldGetUserInteractionForVideo() {
        mockMvcTester.get().uri("/videos/{videoId}/user-interactions", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);

        mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.get().uri("/videos/{videoId}/user-interactions", videoId)
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(UserVideoInteractionDTO.class)
                .satisfies(video -> assertThat(video.rate()).hasValue(VideoRate.LIKE));

        mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.get().uri("/videos/{videoId}/user-interactions", videoId)
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(UserVideoInteractionDTO.class)
                .satisfies(video -> assertThat(video.rate()).isNotPresent());

        mockMvcTester.post().uri("/videos/{videoId}/toggle-dislike", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.get().uri("/videos/{videoId}/user-interactions", videoId)
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(UserVideoInteractionDTO.class)
                .satisfies(video -> assertThat(video.rate()).hasValue(VideoRate.DISLIKE));
    }

    @Test
    void shouldRateComment() {
        List<UUID> comments = createComments(10);
        mockMvcTester.post().uri("/videos/{videoId}/comments/user-interactions", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(comments.get(0), comments.get(1))))
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.list(UserVideoInteractionDTO.class))
                .hasSize(0);

        mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-like", videoId, comments.get(0))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.post().uri("/videos/{videoId}/comments/user-interactions", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(comments.get(0), comments.get(1))))
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.list(UserCommentInteractionDTO.class))
                .hasSize(1)
                .anyMatch(u -> u.commentId().equals(comments.getFirst()) && u.rate().equals("LIKE"));

        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(10))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .anyMatch(c -> c.likes() == 1);

        //add ten likes from different users to same comment
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-like", videoId, comments.get(0))
                    .with(jwt().jwt(jwt -> jwt.subject("user" + finalI)))
                    .assertThat()
                    .hasStatus(HttpStatus.OK);
        }

        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(10))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .anyMatch(c -> c.likes() == 11);

        //toggle like again so it should remove like
        mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-like", videoId, comments.get(0))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.post().uri("/videos/{videoId}/comments/user-interactions", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(comments.get(0), comments.get(1))))
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.list(UserCommentInteractionDTO.class))
                .hasSize(1)
                .anyMatch(u -> u.commentId().equals(comments.getFirst()) && u.rate() == null);

        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(10))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .anyMatch(c -> c.likes() == 10);

        //toggle dislike
        mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-dislike", videoId, comments.get(0))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.post().uri("/videos/{videoId}/comments/user-interactions", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(comments.get(0), comments.get(1))))
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.list(UserCommentInteractionDTO.class))
                .hasSize(1)
                .anyMatch(u -> u.commentId().equals(comments.getFirst()) && u.rate().equals("DISLIKE"));

        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(10))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .anyMatch(c -> c.dislikes() == 1);

        //switch to like
        mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-like", videoId, comments.get(0))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.post().uri("/videos/{videoId}/comments/user-interactions", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(comments.get(0), comments.get(1))))
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.list(UserCommentInteractionDTO.class))
                .hasSize(1)
                .anyMatch(u -> u.commentId().equals(comments.getFirst()) && u.rate().equals("LIKE"));

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-dislike", videoId, comments.get(0))
                    .with(jwt().jwt(jwt -> jwt.subject("user" + finalI)))
                    .assertThat()
                    .hasStatus(HttpStatus.OK);
        }

        mockMvcTester.get().uri("/videos/{videoId}/comments/newest", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(CommentsPageOffset.class)
                .satisfies(c -> assertThat(c.comments()).hasSize(10))
                .extracting(CommentsPageOffset::comments)
                .asInstanceOf(InstanceOfAssertFactories.list(CommentDTO.class))
                .anyMatch(c -> c.likes() == 6 && c.dislikes() == 5);
    }

    @Test
    void shouldRateMultipleVideos() {
        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getLikes()).isZero());

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId2)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> assertThat(video.getLikes()).isZero());


        mockMvcTester.post().uri("/videos/{videoId}/toggle-like", videoId)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.post().uri("/videos/{videoId}/toggle-dislike", videoId2)
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> {
                    assertThat(video.getLikes()).isOne();
                    assertThat(video.getDislikes()).isZero();
                });

        mockMvcTester.get().uri("/videos/{videoId}/metadata", videoId2)
                .assertThat()
                .bodyJson()
                .convertTo(Video.class)
                .satisfies(video -> {
                    assertThat(video.getLikes()).isZero();
                    assertThat(video.getDislikes()).isOne();
                });
    }

    @Test
    void shouldRateMultipleComments() {
        List<UUID> comments = createComments(10);
        mockMvcTester.post().uri("/videos/{videoId}/comments/user-interactions", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(comments.get(0), comments.get(1))))
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.list(UserVideoInteractionDTO.class))
                .hasSize(0);

        mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-like", videoId, comments.get(0))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.post().uri("/videos/{videoId}/comments/{commentId}/toggle-dislike", videoId, comments.get(1))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.OK);

        mockMvcTester.post().uri("/videos/{videoId}/comments/user-interactions", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(comments.get(0), comments.get(1))))
                .with(jwt())
                .assertThat()
                .bodyJson()
                .convertTo(InstanceOfAssertFactories.list(UserCommentInteractionDTO.class))
                .hasSize(2)
                .anyMatch(u -> u.commentId().equals(comments.getFirst()) && u.rate().equals("LIKE"))
                .anyMatch(u -> u.commentId().equals(comments.get(1)) && u.rate().equals("DISLIKE"));
    }

    @Test
    void shouldGetVideoPreviewThumbnailsAndWEBVTT() throws IOException {
        String expectedVTT = Files.readString(VideoTestUtils.THUMBNAIL_PREVIEW_VTT_REST).replaceAll("\\{videoId}", videoId.toString());

        mockMvcTester.get().uri("/videos/{videoId}/preview_thumbnails", videoId)
                .assertThat()
                .hasStatus(HttpStatus.OK)
                .hasContentType(MediaType.IMAGE_JPEG)
                .body().isEqualTo(Files.readAllBytes(VideoTestUtils.THUMBNAIL_PREVIEW_REST));

        mockMvcTester.get().uri("/videos/{videoId}/preview_thumbnails_vtt", videoId)
                .assertThat()
                .hasStatus(HttpStatus.OK)
                .hasContentType("text/vtt")
                .bodyText().isEqualTo(expectedVTT);
    }

    @Test
    void shouldSearchVideoByTitle() {
        //given - prepare titles of videos for test
        String newTitle = "New video1 title";
        mockMvcTester.put().uri("/videos/{id}", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VideoUpdateDTO(Optional.of(newTitle))))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        mockMvcTester.put().uri("/videos/{id}", videoId2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VideoUpdateDTO(Optional.of("New video2 title"))))
                .with(jwt())
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        //view one video
        mockMvcTester.post().uri("/videos/{videoId}/views", videoId2)
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        //then
        String searchQuery = "video1";
        mockMvcTester.get().uri("/videos/search?q={searchQuery}", searchQuery)
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .convertTo(VideoSearchResponse.class)
                .satisfies(videoSearchResponse -> {
                    assertThat(videoSearchResponse.videos()).hasSize(1);
                    assertThat(videoSearchResponse.videos().getFirst().getId()).isEqualTo(videoId);
                });

        mockMvcTester.get().uri("/videos/search?q={searchQuery}", "not exist")
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .convertTo(VideoSearchResponse.class)
                .satisfies(videoSearchResponse -> assertThat(videoSearchResponse.videos()).hasSize(0));

        mockMvcTester.get().uri("/videos/search?q={searchQuery}", "video2")
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .convertTo(VideoSearchResponse.class)
                .satisfies(videoSearchResponse -> {
                    assertThat(videoSearchResponse.videos()).hasSize(1);
                    assertThat(videoSearchResponse.videos().getFirst().getId()).isEqualTo(videoId2);
                });

        //should get videos sorted by views
        mockMvcTester.get().uri("/videos/search?q={searchQuery}", "New video")
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .convertTo(VideoSearchResponse.class)
                .satisfies(videoSearchResponse -> {
                    assertThat(videoSearchResponse.videos()).hasSize(2);
                    assertThat(videoSearchResponse.videos().getFirst().getId()).isEqualTo(videoId2);
                    assertThat(videoSearchResponse.videos().get(1).getId()).isEqualTo(videoId);
                    assertThat(videoSearchResponse.videos()).isSortedAccordingTo((o1, o2) ->  Math.toIntExact(o2.getViewsCount() - o1.getViewsCount()));
                });

        //view other video more times
        mockMvcTester.post().uri("/videos/{videoId}/views", videoId)
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);
        mockMvcTester.post().uri("/videos/{videoId}/views", videoId)
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        //should get different sorting
        mockMvcTester.get().uri("/videos/search?q={searchQuery}", "New video")
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .convertTo(VideoSearchResponse.class)
                .satisfies(videoSearchResponse -> {
                    assertThat(videoSearchResponse.videos()).hasSize(2);
                    assertThat(videoSearchResponse.videos().getFirst().getId()).isEqualTo(videoId);
                    assertThat(videoSearchResponse.videos().get(1).getId()).isEqualTo(videoId2);
                    assertThat(videoSearchResponse.videos()).isSortedAccordingTo((o1, o2) ->  Math.toIntExact(o2.getViewsCount() - o1.getViewsCount()));
                });
    }

    List<UUID> createComments() {
        return createComments(100);
    }

    List<UUID> createComments(int size) {
        try {
            List<UUID> commentsIds = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                CommentResponse createdComment = objectMapper.readValue(mockMvcTester.post().uri("/videos/{id}/comments", videoId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentRequest("Test comment")))
                        .exchange()
                        .getResponse().getContentAsString(), CommentResponse.class);
                commentsIds.add(createdComment.commentId());
            }

            return commentsIds;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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
