package com.example.ytclone.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoEntity {
    @Id
    private UUID id;

    private String filename;
    private String title;
    @Column(nullable = false, updatable = false)
    private String createdBy;
    private Long length;
    private LocalDateTime uploadDate;
}
