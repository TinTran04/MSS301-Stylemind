package com.stylemind.common.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UserRegisteredData data) {

    public record UserRegisteredData(UUID userId, String fullName) {
    }
}
