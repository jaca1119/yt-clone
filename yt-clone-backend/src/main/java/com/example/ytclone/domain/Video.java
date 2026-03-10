package com.example.ytclone.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Video {
    private UUID id;
    private String filename;
    private String title;
    private long length;
}
