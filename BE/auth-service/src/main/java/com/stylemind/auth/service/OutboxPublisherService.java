package com.stylemind.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.auth.entity.OutboxEvent;
import com.stylemind.auth.entity.OutboxEventStatus;
import com.stylemind.auth.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "auth.outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.exchange:stylemind.events}")
    private String exchange;

    @Value("${app.rabbitmq.user-registered.routing-key:user.registered}")
    private String userRegisteredRoutingKey;

    @Value("${auth.outbox.publisher.batch-size:20}")
    private int batchSize;

    @Value("${auth.outbox.publisher.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    @Scheduled(
            fixedDelayString = "${auth.outbox.publisher.fixed-delay-ms:5000}",
            initialDelayString = "${auth.outbox.publisher.initial-delay-ms:5000}")
    public void publishPendingEvents() {
        publishPendingBatch();
    }

    @Transactional
    public int publishPendingBatch() {
        List<OutboxEvent> events = outboxEventRepository.findPendingForPublish(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, batchSize));

        int published = 0;
        for (OutboxEvent event : events) {
            publishEvent(event);
            published++;
        }
        return published;
    }

    private void publishEvent(OutboxEvent event) {
        String routingKey = routingKeyFor(event);
        Message message = MessageBuilder.withBody(event.getPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(event.getId().toString())
                .setCorrelationId(event.getId().toString())
                .setHeader("eventId", event.getId().toString())
                .setHeader("eventType", event.getEventType())
                .build();

        try {
            rabbitTemplate.invoke(operations -> {
                operations.send(exchange, routingKey, message);
                operations.waitForConfirmsOrDie(confirmTimeoutMs);
                return null;
            });
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            log.info("Published outbox event eventId={} eventType={} routingKey={}", event.getId(), event.getEventType(), routingKey);
        } catch (AmqpException ex) {
            log.warn("Could not publish outbox event eventId={} eventType={} routingKey={}: {}",
                    event.getId(), event.getEventType(), routingKey, ex.getMessage());
            throw ex;
        }
    }

    private String routingKeyFor(OutboxEvent event) {
        if ("USER_REGISTERED".equals(event.getEventType())) {
            validateUserRegisteredPayload(event);
            return userRegisteredRoutingKey;
        }
        throw new IllegalStateException("Unsupported outbox event type: " + event.getEventType());
    }

    private void validateUserRegisteredPayload(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            if (containsForbiddenField(payload)) {
                throw new IllegalStateException("USER_REGISTERED payload contains forbidden fields");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid USER_REGISTERED payload for eventId=" + event.getId(), ex);
        }
    }

    private boolean containsForbiddenField(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                if ("password".equals(fieldName)
                        || "passwordHash".equals(fieldName)
                        || "email".equals(fieldName)
                        || "phone".equals(fieldName)
                        || "address".equals(fieldName)
                        || "refreshToken".equals(fieldName)) {
                    return true;
                }
                if (containsForbiddenField(field.getValue())) {
                    return true;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsForbiddenField(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
