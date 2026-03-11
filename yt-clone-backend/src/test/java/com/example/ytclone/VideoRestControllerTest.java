package com.example.ytclone;

import com.example.ytclone.domain.Video;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureRestTestClient
public class VideoRestControllerTest {
    @Autowired
    RestTestClient restTestClient;

    @Test
    void shouldGetAllVideos() {
        restTestClient.get().uri("/videos")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Video>>() {
                })
                .value(videos ->
                        assertThat(videos).hasSize(4)
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
        assertThat(videos).hasSize(4);

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
        assertThat(videos).hasSize(4);

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
                .consumeWith(response -> {
                    assertThat(response.getResponseBody()).hasSize(1000);
                });
    }

}
