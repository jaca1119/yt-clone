package com.example.ytclone.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Comment(UUID id, String content, int likes, int dislikes, String createdBy, LocalDateTime createdAt) {
}
