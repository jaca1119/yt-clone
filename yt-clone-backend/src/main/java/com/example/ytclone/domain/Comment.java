package com.example.ytclone.domain;

import java.time.LocalDateTime;

public record Comment(Long id, String content, int likes, int dislikes, String createdBy, LocalDateTime createdAt) {
}
