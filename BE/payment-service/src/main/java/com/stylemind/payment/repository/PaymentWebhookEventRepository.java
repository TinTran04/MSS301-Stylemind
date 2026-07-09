package com.stylemind.payment.repository;

import com.stylemind.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, String> {
    Optional<PaymentWebhookEvent> findByProviderAndGatewayTransactionId(String provider, String gatewayTransactionId);
}
