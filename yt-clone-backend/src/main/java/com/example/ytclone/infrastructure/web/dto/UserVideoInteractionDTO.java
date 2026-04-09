package com.example.ytclone.infrastructure.web.dto;

import com.example.ytclone.infrastructure.persistence.VideoRate;

import java.util.Optional;

public record UserVideoInteractionDTO(Optional<VideoRate> rate) {
}
