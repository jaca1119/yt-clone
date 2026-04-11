package com.example.ytclone.infrastructure.persistence;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class UserCommentInteraction {
    @Nullable
    private CommentRate rate;
}
