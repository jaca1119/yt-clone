package com.example.ytclone.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "videos")
public class VideoEntity {
    @Id
    private UUID id;

    private String filename;
    private String title;
    @Column(nullable = false, updatable = false)
    private String createdBy;
    private Long length;
    @OneToMany(mappedBy = "video", cascade = CascadeType.REMOVE)
    private List<CommentEntity> comments;
    private LocalDateTime uploadDate;
}
