package com.example.ytclone.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "user_comment_interaction")
@Entity
public class UserCommentInteractionEntity {
    @Id
    private UUID id;
    private String username;
    @EqualsAndHashCode.Exclude
    @ManyToOne
    private CommentEntity comment;
    /**
     * Stored as JSONB in postgresql, something like loose schema with Mongodb but in sql
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private UserCommentInteraction userCommentInteraction;
}
