package com.stylemind.user.messaging;

import com.stylemind.common.event.UserRegisteredEvent;
import com.stylemind.user.service.UserRegisteredEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "user-profile.events.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UserRegisteredListener {

    private final UserRegisteredEventHandler handler;

    @RabbitListener(
            queues = "${app.rabbitmq.user-registered.queue:user-profile.user-registered}",
            containerFactory = "userRegisteredRabbitListenerContainerFactory")
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            handler.handle(event);
        } catch (RuntimeException ex) {
            log.warn("Failed to process USER_REGISTERED eventId={}: {}", event.eventId(), ex.getMessage());
            throw ex;
        }
    }
}
