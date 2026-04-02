package com.example.ytcloneauthservice.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityRestController {

    private final UserService userService;

    public SecurityRestController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/register")
    public ResponseEntity register(@Valid @ModelAttribute RegisterRequest registerRequest) {
        userService.register(registerRequest.username(), registerRequest.password());
        return ResponseEntity.ok().build();
    }
}
