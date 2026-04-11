package com.example.ytclone.infrastructure.web.dto;

import java.util.UUID;

public record UserCommentInteractionDTO(UUID commentId, String rate) {
}
