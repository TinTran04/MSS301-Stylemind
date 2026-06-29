package com.stylemind.user.service;

import com.stylemind.common.event.UserRegisteredEvent;
import com.stylemind.user.entity.ProcessedEvent;
import com.stylemind.user.entity.UserProfile;
import com.stylemind.user.repository.ProcessedEventRepository;
import com.stylemind.user.repository.UserProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void handle(UserRegisteredEvent event) {
        UUID eventId = event.eventId();
        UUID userId = event.data().userId();

        if (processedEventRepository.existsById(eventId)) {
            log.info("Skip duplicate USER_REGISTERED eventId={} userId={}", eventId, userId);
            return;
        }

        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.builder()
                    .eventId(eventId)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            log.info("Skip concurrently processed USER_REGISTERED eventId={} userId={}", eventId, userId);
            return;
        }

        if (!userProfileRepository.existsById(userId)) {
            userProfileRepository.save(UserProfile.builder()
                    .userId(userId)
                    .fullName(trimToNull(event.data().fullName()))
                    .build());
        }

        log.info("Processed USER_REGISTERED eventId={} userId={}", eventId, userId);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
