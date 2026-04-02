package com.example.ytcloneauthservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestContainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AuthTests {

    @Autowired
    MockMvcTester mvcTester;

    @Test
    void shouldRedirectToLoginWhenUnauthorized() {
        mvcTester.get().uri("/random-uri")
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login");
    }

    @Test
    void shouldNotFoundWhenAuthorizedUser() {
        mvcTester.get().uri("/random-uri")
                .with(user("mock user"))
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldLoginUser() {
        mvcTester.post().uri("/login")
                .formField("username", "user")
                .formField("password", "password")
                .with(csrf())
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/")
                .matches(authenticated());
    }

    @Test
    void shouldNotLoginInvalidCredentials() {
        mvcTester.post().uri("/login")
                .formField("username", "user")
                .formField("password", "wrong password")
                .with(csrf())
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error")
                .matches(unauthenticated());
    }

    @Test
    void shouldRegisterAndLoginNewUser() {
        String username = "new user";
        String password = "new user password";
        //try login not existing user
        mvcTester.post().uri("/login")
                .formField("username", username)
                .formField("password", password)
                .with(csrf())
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error")
                .matches(unauthenticated());

        //register
        mvcTester.post().uri("/register")
                .formField("username", username)
                .formField("password", password)
                .with(csrf())
                .assertThat()
                .hasStatus(HttpStatus.OK)
                .matches(unauthenticated());

        //try to log in with wrong credentials
        mvcTester.post().uri("/login")
                .formField("username", username)
                .formField("password", "wrong password")
                .with(csrf())
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error")
                .matches(unauthenticated());

        //log in with correct credentials
        mvcTester.post().uri("/login")
                .formField("username", username)
                .formField("password", password)
                .with(csrf())
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/")
                .matches(authenticated());
    }
}
