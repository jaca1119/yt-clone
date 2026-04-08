package com.example.ytclone.infrastructure.persistence;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "user_video_interaction")
@Entity
public class UserVideoInteractionEntity {
    @Id
    private UUID id;
    private String username;
    @EqualsAndHashCode.Exclude
    @ManyToOne
    private VideoEntity video;
    @Nullable
    private VideoRate rate;
}
