package com.example.ytclone.infrastructure.web.dto;

import com.example.ytclone.infrastructure.persistence.CommentDTO;

import java.util.List;

public record CommentsPageOffset(List<CommentDTO> comments, boolean hasNext) {
}
