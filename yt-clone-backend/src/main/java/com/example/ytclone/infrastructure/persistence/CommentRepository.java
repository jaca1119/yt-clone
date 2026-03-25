package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    @Query("select ce from CommentEntity ce where ce.video = :video and ce.parent is null order by ce.createdAt desc limit 10 offset :offset")
    List<CommentEntity> findTop10ByVideoOffset(VideoEntity video, long offset);
}
