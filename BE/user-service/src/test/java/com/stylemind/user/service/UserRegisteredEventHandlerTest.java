package com.stylemind.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.stylemind.common.event.UserRegisteredEvent;
import com.stylemind.user.repository.ProcessedEventRepository;
import com.stylemind.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserRegisteredEventHandlerTest {

    @Autowired
    private UserRegisteredEventHandler handler;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private Queue userRegisteredQueue;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void consumerCreatesProfileFromUserRegisteredEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        handler.handle(userRegistered(eventId, userId, "Nguyen Van A"));

        assertThat(processedEventRepository.existsById(eventId)).isTrue();
        assertThat(userProfileRepository.findById(userId)).hasValueSatisfying(profile ->
                assertThat(profile.getFullName()).isEqualTo("Nguyen Van A"));
    }

    @Test
    void duplicateEventDoesNotCreateDuplicateProfile() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        handler.handle(userRegistered(eventId, userId, "Nguyen Van A"));
        handler.handle(userRegistered(eventId, userId, "Changed Name"));

        assertThat(processedEventRepository.count()).isEqualTo(1);
        assertThat(userProfileRepository.count()).isEqualTo(1);
        assertThat(userProfileRepository.findById(userId)).hasValueSatisfying(profile ->
                assertThat(profile.getFullName()).isEqualTo("Nguyen Van A"));
    }

    @Test
    void userRegisteredQueueIsConfiguredWithDeadLetterPolicy() {
        assertThat(userRegisteredQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", "stylemind.events.dlx")
                .containsEntry("x-dead-letter-routing-key", "user.registered.dlq");
    }

    private UserRegisteredEvent userRegistered(UUID eventId, UUID userId, String fullName) {
        return new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                Instant.now(),
                new UserRegisteredEvent.UserRegisteredData(userId, fullName));
    }
}
