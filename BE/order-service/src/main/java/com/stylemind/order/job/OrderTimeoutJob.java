package com.stylemind.order.job;

import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutJob {

    private static final String SYSTEM_ACTOR = "SYSTEM_TIMEOUT_JOB";

    private final OrderRepository orderRepository;
    private final OrderStatusService orderStatusService;

    @Value("${app.order.payment-timeout-minutes:30}")
    private long paymentTimeoutMinutes;

    @Scheduled(fixedDelayString = "${app.order.timeout-job-interval-ms:300000}")
    public void expireStalePaymentPendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        List<Order> staleOrders = orderRepository.findByOrderStatusAndCreatedAtBefore(OrderStatus.PAYMENT_PENDING, cutoff);

        for (Order order : staleOrders) {
            try {
                orderStatusService.changeStatus(order, OrderStatus.EXPIRED, SYSTEM_ACTOR);
            } catch (Exception ex) {
                log.warn("Failed to expire stale order {}: {}", order.getId(), ex.getMessage());
            }
        }

        if (!staleOrders.isEmpty()) {
            log.info("Expired {} stale PAYMENT_PENDING order(s) older than {} minutes", staleOrders.size(), paymentTimeoutMinutes);
        }
    }
}
