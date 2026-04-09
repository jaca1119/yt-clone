package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserVideoInteractionRepository extends JpaRepository<UserVideoInteractionEntity, UUID> {
    Optional<UserVideoInteractionEntity> findByUsernameAndVideoId(String username, UUID videoId);
}
