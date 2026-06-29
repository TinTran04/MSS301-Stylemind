package com.stylemind.gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {

    private boolean enabled = true;
    private boolean failOpen = true;
    private Duration redisTimeout = Duration.ofMillis(250);
    private Policy loginIp = new Policy(10, Duration.ofMinutes(1));
    private Policy loginIdentifier = new Policy(10, Duration.ofMinutes(5));
    private Policy registerIp = new Policy(5, Duration.ofMinutes(1));
    private Policy forgotPasswordIp = new Policy(5, Duration.ofMinutes(1));
    private Policy forgotPasswordIdentifier = new Policy(5, Duration.ofMinutes(15));
    private Policy resetPasswordIp = new Policy(5, Duration.ofMinutes(1));
    private Policy resetPasswordToken = new Policy(5, Duration.ofMinutes(15));
    private Policy refreshIp = new Policy(30, Duration.ofMinutes(1));
    private Policy aiChat = new Policy(5, Duration.ofMinutes(1));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public Duration getRedisTimeout() {
        return redisTimeout;
    }

    public void setRedisTimeout(Duration redisTimeout) {
        this.redisTimeout = redisTimeout;
    }

    public Policy getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(Policy loginIp) {
        this.loginIp = loginIp;
    }

    public Policy getLoginIdentifier() {
        return loginIdentifier;
    }

    public void setLoginIdentifier(Policy loginIdentifier) {
        this.loginIdentifier = loginIdentifier;
    }

    public Policy getRegisterIp() {
        return registerIp;
    }

    public void setRegisterIp(Policy registerIp) {
        this.registerIp = registerIp;
    }

    public Policy getForgotPasswordIp() {
        return forgotPasswordIp;
    }

    public void setForgotPasswordIp(Policy forgotPasswordIp) {
        this.forgotPasswordIp = forgotPasswordIp;
    }

    public Policy getForgotPasswordIdentifier() {
        return forgotPasswordIdentifier;
    }

    public void setForgotPasswordIdentifier(Policy forgotPasswordIdentifier) {
        this.forgotPasswordIdentifier = forgotPasswordIdentifier;
    }

    public Policy getResetPasswordIp() {
        return resetPasswordIp;
    }

    public void setResetPasswordIp(Policy resetPasswordIp) {
        this.resetPasswordIp = resetPasswordIp;
    }

    public Policy getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(Policy resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public Policy getRefreshIp() {
        return refreshIp;
    }

    public void setRefreshIp(Policy refreshIp) {
        this.refreshIp = refreshIp;
    }

    public Policy getAiChat() {
        return aiChat;
    }

    public void setAiChat(Policy aiChat) {
        this.aiChat = aiChat;
    }

    public static class Policy {
        private int limit;
        private Duration window;

        public Policy() {
        }

        public Policy(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
