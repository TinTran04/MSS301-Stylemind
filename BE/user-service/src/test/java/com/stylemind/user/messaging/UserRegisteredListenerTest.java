package com.stylemind.user.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.stylemind.common.event.UserRegisteredEvent;
import com.stylemind.user.service.UserRegisteredEventHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRegisteredListenerTest {

    @Test
    void listenerPropagatesConsumerFailureSoRabbitCanRetry() {
        UserRegisteredEventHandler handler = mock(UserRegisteredEventHandler.class);
        UserRegisteredListener listener = new UserRegisteredListener(handler);
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(),
                "USER_REGISTERED",
                Instant.now(),
                new UserRegisteredEvent.UserRegisteredData(UUID.randomUUID(), "Nguyen Van A"));
        doThrow(new IllegalStateException("database unavailable")).when(handler).handle(event);

        assertThatThrownBy(() -> listener.onUserRegistered(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }
}
