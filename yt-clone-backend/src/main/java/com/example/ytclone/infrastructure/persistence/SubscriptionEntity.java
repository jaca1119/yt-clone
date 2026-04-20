package com.example.ytclone.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscription_subscriber_creator", columnNames = {"subscriberUsername", "creatorUsername"})
)
public class SubscriptionEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String subscriberUsername;

    @Column(nullable = false)
    private String creatorUsername;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
