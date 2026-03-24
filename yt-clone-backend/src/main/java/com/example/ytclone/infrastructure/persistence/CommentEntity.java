package com.example.ytclone.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class CommentEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private VideoEntity video;
    @OneToOne(fetch = FetchType.LAZY)
    private CommentEntity parent;
    private int likes;
    private int dislikes;

    private LocalDateTime createdAt;
    private String createdBy;
}
