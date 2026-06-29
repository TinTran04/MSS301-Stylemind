package com.stylemind.auth.service;

import com.stylemind.auth.entity.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogSafeEmailSender implements EmailSender {

    @Override
    public void sendEmailVerification(Account account, String rawToken) {
        log.info("Email verification requested for accountId={}", account.getId());
    }

    @Override
    public void sendPasswordReset(Account account, String rawToken) {
        log.info("Password reset requested for accountId={}", account.getId());
    }
}
