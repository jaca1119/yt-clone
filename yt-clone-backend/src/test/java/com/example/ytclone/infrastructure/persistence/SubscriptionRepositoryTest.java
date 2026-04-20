package com.example.ytclone.infrastructure.persistence;

import com.example.ytclone.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SubscriptionRepositoryTest {
    @Autowired
    SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
    }

    @Test
    void shouldEnforceUniqueSubscriberAndCreatorPair() {
        subscriptionRepository.saveAndFlush(new SubscriptionEntity(UUID.randomUUID(), "subscriber", "creator", LocalDateTime.now()));

        assertThatThrownBy(() -> subscriptionRepository.saveAndFlush(new SubscriptionEntity(UUID.randomUUID(), "subscriber", "creator", LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void shouldCountExistsAndDeleteBySubscriberAndCreator() {
        subscriptionRepository.saveAndFlush(new SubscriptionEntity(UUID.randomUUID(), "subscriber", "creator", LocalDateTime.now()));
        subscriptionRepository.saveAndFlush(new SubscriptionEntity(UUID.randomUUID(), "subscriber-2", "creator", LocalDateTime.now()));

        assertThat(subscriptionRepository.existsBySubscriberUsernameAndCreatorUsername("subscriber", "creator")).isTrue();
        assertThat(subscriptionRepository.countByCreatorUsername("creator")).isEqualTo(2);

        subscriptionRepository.deleteBySubscriberUsernameAndCreatorUsername("subscriber", "creator");

        assertThat(subscriptionRepository.existsBySubscriberUsernameAndCreatorUsername("subscriber", "creator")).isFalse();
        assertThat(subscriptionRepository.countByCreatorUsername("creator")).isEqualTo(1);
    }
}
