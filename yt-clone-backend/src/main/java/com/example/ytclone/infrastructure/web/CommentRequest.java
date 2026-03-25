package com.example.ytclone.infrastructure.web;

import jakarta.validation.constraints.NotEmpty;

public record CommentRequest(@NotEmpty String comment) {
}
