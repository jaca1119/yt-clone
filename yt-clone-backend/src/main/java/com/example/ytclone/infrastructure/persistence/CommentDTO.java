package com.example.ytclone.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentDTO(UUID id, long replyCount, String content, LocalDateTime createdAt, String createdBy) {
}
