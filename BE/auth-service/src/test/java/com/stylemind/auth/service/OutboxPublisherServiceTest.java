package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stylemind.auth.entity.OutboxEvent;
import com.stylemind.auth.entity.OutboxEventStatus;
import com.stylemind.auth.repository.OutboxEventRepository;
import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "auth.outbox.publisher.enabled=true",
        "auth.outbox.publisher.initial-delay-ms=600000"
})
@ActiveProfiles("test")
class OutboxPublisherServiceTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void publishPendingEventMarksEventPublishedAfterBrokerConfirm() {
        OutboxEvent event = outboxEventRepository.save(userRegisteredOutboxEvent());
        when(rabbitTemplate.invoke(any())).thenReturn(null);

        int published = outboxPublisherService.publishPendingBatch();

        OutboxEvent reloaded = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(published).isEqualTo(1);
        assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(reloaded.getPublishedAt()).isNotNull();
    }

    @Test
    void rabbitUnavailableKeepsOutboxEventPending() {
        OutboxEvent event = outboxEventRepository.save(userRegisteredOutboxEvent());
        when(rabbitTemplate.invoke(any())).thenThrow(new AmqpConnectException(new ConnectException("rabbit unavailable")));

        assertThatThrownBy(() -> outboxPublisherService.publishPendingBatch())
                .isInstanceOf(AmqpConnectException.class);

        OutboxEvent reloaded = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(reloaded.getPublishedAt()).isNull();
    }

    private OutboxEvent userRegisteredOutboxEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        return OutboxEvent.builder()
                .id(eventId)
                .aggregateId(userId)
                .eventType("USER_REGISTERED")
                .payload("""
                        {
                          "eventId": "%s",
                          "eventType": "USER_REGISTERED",
                          "occurredAt": "2026-06-22T00:00:00Z",
                          "data": {
                            "userId": "%s",
                            "fullName": "Nguyen Van A"
                          }
                        }
                        """.formatted(eventId, userId))
                .status(OutboxEventStatus.PENDING)
                .build();
    }
}
