package com.example.ytclone.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVideoInteraction {
    @Nullable
    private VideoRate rate;
}
