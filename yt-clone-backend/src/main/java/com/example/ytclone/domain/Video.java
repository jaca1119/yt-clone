package com.example.ytclone.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Video {
    private UUID id;
    private String filename;
    private String title;
    private String creator;
    private long length;
    private LocalDateTime uploadDate;
}
