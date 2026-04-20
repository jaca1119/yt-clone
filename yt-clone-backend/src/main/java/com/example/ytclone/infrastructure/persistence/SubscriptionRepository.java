package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
    long countByCreatorUsername(String creatorUsername);

    boolean existsBySubscriberUsernameAndCreatorUsername(String subscriberUsername, String creatorUsername);

    void deleteBySubscriberUsernameAndCreatorUsername(String subscriberUsername, String creatorUsername);
}
