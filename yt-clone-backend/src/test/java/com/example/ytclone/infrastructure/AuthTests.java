package com.example.ytclone.infrastructure;

import com.example.ytclone.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AuthTests {
    @Autowired
    MockMvc mvc;

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

    @Test
    void shouldNotAuthorizeWhenNotValidJwtAlgNone() throws Exception {
        String jwtTokenAlgNone = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.";
        MockMultipartFile file = new MockMultipartFile("file", "file.xd", MediaType.IMAGE_JPEG_VALUE, "fake bytes".getBytes());

        mvc.perform(multipart("/videos/{id}", UUID.randomUUID()).file(file).header("Authorization", "Bearer " + jwtTokenAlgNone))
                .andExpect(status().isUnauthorized());
    }
}
