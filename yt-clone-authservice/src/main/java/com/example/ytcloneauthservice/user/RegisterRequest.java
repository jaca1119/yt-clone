package com.example.ytcloneauthservice.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank String username, @Size(min = 6) String password) {
}
