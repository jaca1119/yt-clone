package com.example.ytcloneauthservice.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public final class RegisterRequest {
    private @NotBlank String username;
    private @Size(min = 6, max = 50) String password;
}
