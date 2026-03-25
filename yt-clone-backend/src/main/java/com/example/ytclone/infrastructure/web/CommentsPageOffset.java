package com.example.ytclone.infrastructure.web;

import com.example.ytclone.domain.Comment;

import java.util.List;

public record CommentsPageOffset(List<Comment> comments, boolean hasNext) {
}
