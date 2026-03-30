package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    @Query("select new com.example.ytclone.infrastructure.persistence.CommentDTO(ce.id, count(r.id), ce.content, ce.createdAt, ce.createdBy) from CommentEntity ce left join CommentEntity r on r.parent = ce where ce.video = :video and ce.parent is null group by ce.id order by ce.createdAt desc limit 10 offset :offset")
    List<CommentDTO> findTop10ByVideoOffsetWithReplyCount(VideoEntity video, long offset);

    @Query("select new com.example.ytclone.infrastructure.persistence.CommentDTO(ce.id, count(r.id), ce.content, ce.createdAt, ce.createdBy) from CommentEntity ce left join CommentEntity r on r.parent = ce where ce.video = :video and ce.parent.id = :parentId group by ce.id order by ce.createdAt desc limit 10 offset :offset")
    List<CommentDTO> findByVideoAndParentOrderByCreatedAtDesc(VideoEntity video, UUID parentId, long offset);
}
