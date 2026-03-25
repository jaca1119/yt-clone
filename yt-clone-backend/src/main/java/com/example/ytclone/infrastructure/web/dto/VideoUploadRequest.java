package com.example.ytclone.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;

public record VideoUploadRequest(@NotEmpty String title) {
}
