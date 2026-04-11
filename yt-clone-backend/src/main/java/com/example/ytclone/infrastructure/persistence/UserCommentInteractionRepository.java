package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCommentInteractionRepository extends JpaRepository<UserCommentInteractionEntity, UUID> {
    List<UserCommentInteractionEntity> findAllByUsernameAndCommentIdIn(String username, List<UUID> commentId);
    Optional<UserCommentInteractionEntity> findByUsernameAndCommentId(String username, UUID commentId);
}
