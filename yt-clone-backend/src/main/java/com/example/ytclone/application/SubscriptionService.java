package com.example.ytclone.application;

import com.example.ytclone.infrastructure.persistence.SubscriptionEntity;
import com.example.ytclone.infrastructure.persistence.SubscriptionRepository;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final VideoRepository videoRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, VideoRepository videoRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.videoRepository = videoRepository;
    }

    @Transactional
    public boolean toggleSubscription(String subscriberUsername, String creatorUsername) {
        validateSubscriptionRequest(subscriberUsername, creatorUsername);

        boolean alreadySubscribed = subscriptionRepository
                .existsBySubscriberUsernameAndCreatorUsername(subscriberUsername, creatorUsername);
        if (alreadySubscribed) {
            subscriptionRepository.deleteBySubscriberUsernameAndCreatorUsername(subscriberUsername, creatorUsername);
            return false;
        }

        subscriptionRepository.save(
                new SubscriptionEntity(UUID.randomUUID(), subscriberUsername, creatorUsername, LocalDateTime.now()));
        return true;
    }

    public boolean isSubscribed(String subscriberUsername, String creatorUsername) {
        validateCreatorExists(creatorUsername);
        return subscriptionRepository.existsBySubscriberUsernameAndCreatorUsername(subscriberUsername, creatorUsername);
    }

    public long countSubscribers(String creatorUsername) {
        validateCreatorExists(creatorUsername);
        return subscriptionRepository.countByCreatorUsername(creatorUsername);
    }

    private void validateSubscriptionRequest(String subscriberUsername, String creatorUsername) {
        if (subscriberUsername.equals(creatorUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot subscribe to yourself");
        }
        validateCreatorExists(creatorUsername);
    }

    private void validateCreatorExists(String creatorUsername) {
        if (!videoRepository.existsByCreatedBy(creatorUsername)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found");
        }
    }
}
