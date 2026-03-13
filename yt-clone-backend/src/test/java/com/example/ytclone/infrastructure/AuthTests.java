package com.example.ytclone.infrastructure;

import com.example.ytclone.application.VideoService;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import com.example.ytclone.infrastructure.web.VideoRestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoRestController.class)
public class AuthTests {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    VideoService videoService;
    @MockitoBean
    VideoRepository videoRepository;

    @Test
    void shouldGet401WhenCallingEndpointThatIsProtected() throws Exception {
        mvc.perform(get("/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCallAuthenticatedEndpointAndGetResponse() throws Exception {
        mvc.perform(post("/test").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
