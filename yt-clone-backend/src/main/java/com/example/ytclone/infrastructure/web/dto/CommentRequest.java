package com.example.ytclone.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;

public record CommentRequest(@NotEmpty String comment) {
}
