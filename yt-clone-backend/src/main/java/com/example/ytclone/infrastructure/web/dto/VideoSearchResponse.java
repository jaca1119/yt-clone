package com.example.ytclone.infrastructure.web.dto;

import com.example.ytclone.domain.Video;
import org.jspecify.annotations.NonNull;

import java.util.List;


public record VideoSearchResponse(@NonNull List<Video> videos) {
}
