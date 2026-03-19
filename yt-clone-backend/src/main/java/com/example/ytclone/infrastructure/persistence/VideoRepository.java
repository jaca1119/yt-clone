package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface VideoRepository extends JpaRepository<VideoEntity, UUID> {
    Optional<VideoEntity> findByIdAndCreatedBy(UUID id, String user);

    List<VideoEntity> findAllByFilenameIsNotNull();
}
