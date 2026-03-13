package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface VideoRepository extends JpaRepository<VideoEntity, UUID> {
}
