package com.example.ytclone.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;

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
    @NonNull
    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    private UserVideoInteraction userVideoInteraction;
}
