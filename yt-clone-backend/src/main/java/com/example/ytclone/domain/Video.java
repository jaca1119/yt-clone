package com.example.ytclone.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Video {
    private UUID id;
    private Path video;
    private Path thumbnail;
    private String title;
    private int length;
}
