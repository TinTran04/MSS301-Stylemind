package com.stylemind.auth.service;

import com.stylemind.auth.entity.Account;

public interface EmailSender {

    void sendEmailVerification(Account account, String rawToken);

    void sendPasswordReset(Account account, String rawToken);
}
