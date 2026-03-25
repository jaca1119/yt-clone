package com.example.ytclone.infrastructure.web;

import jakarta.validation.constraints.NotEmpty;

public record VideoUploadRequest(@NotEmpty String title) {
}
