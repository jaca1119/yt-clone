package com.example.ytclone.application;

import com.example.ytclone.TestcontainersConfiguration;
import com.example.ytclone.infrastructure.persistence.SubscriptionRepository;
import com.example.ytclone.infrastructure.persistence.VideoEntity;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SubscriptionServiceTest {
    @Autowired
    SubscriptionService subscriptionService;
    @Autowired
    SubscriptionRepository subscriptionRepository;
    @Autowired
    VideoRepository videoRepository;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        videoRepository.deleteAll();
    }

    @Test
    void shouldToggleSubscriptionState() {
        createCreatorVideo("creator");

        boolean subscribeState = subscriptionService.toggleSubscription("subscriber", "creator");
        assertThat(subscribeState).isTrue();
        assertThat(subscriptionService.isSubscribed("subscriber", "creator")).isTrue();
        assertThat(subscriptionService.countSubscribers("creator")).isOne();


        boolean unsubscribeState = subscriptionService.toggleSubscription("subscriber", "creator");
        assertThat(unsubscribeState).isFalse();
        assertThat(subscriptionService.isSubscribed("subscriber", "creator")).isFalse();
        assertThat(subscriptionService.countSubscribers("creator")).isZero();

        subscriptionService.toggleSubscription("subscriber", "creator");
        subscriptionService.toggleSubscription("subscriber2", "creator");
        subscriptionService.toggleSubscription("subscriber3", "creator");
        assertThat(subscriptionService.countSubscribers("creator")).isEqualTo(3);
    }

    @Test
    void shouldRejectSelfSubscription() {
        createCreatorVideo("creator");

        assertThatThrownBy(() -> subscriptionService.toggleSubscription("creator", "creator"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldRejectUnknownCreator() {
        assertThatThrownBy(() -> subscriptionService.toggleSubscription("subscriber", "missing-creator"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void createCreatorVideo(String creator) {
        videoRepository.saveAndFlush(new VideoEntity(
                UUID.randomUUID(),
                "video.mp4",
                "title",
                creator,
                42L,
                null,
                LocalDateTime.now(),
                0,
                0,
                0,
                null,
                0
        ));
    }
}
