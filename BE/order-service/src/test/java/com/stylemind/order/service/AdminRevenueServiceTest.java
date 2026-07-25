package com.stylemind.order.service;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.dto.OrderRevenueAggregate;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.entity.Order;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.repository.OrderStatusAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRevenueServiceTest {

    @Mock PaymentClient paymentClient;
    @Mock OrderRepository orderRepository;
    @Mock OrderStatusAuditLogRepository auditLogRepository;

    private AdminRevenueService revenueService;
    private final LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 8, 1, 0, 0);

    @BeforeEach
    void setUp() {
        revenueService = new AdminRevenueService(paymentClient, orderRepository, auditLogRepository);
    }

    @Test
    void recognizesPaidSepayAndCompletedCodUsingNetSubtotal() {
        when(paymentClient.findSepayRevenueCandidates(
                from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .thenReturn(ApiResponse.success(List.of(
                        candidate("sepay-order", "sepay", "PAID", "2026-07-10T10:00:00"),
                        candidate("pending-order", "sepay", "PENDING", null),
                        candidate("boundary-order", "sepay", "PAID", "2026-08-01T00:00:00"),
                        candidate("legacy-order", "SEPAY_QR", "COMPLETED", "2026-07-10T10:00:00"))));
        when(auditLogRepository.findCompletedOrderIdsBetween(from, to)).thenReturn(List.of("cod-order"));
        when(paymentClient.findRevenueCandidatesByOrderIds(
                new PaymentClient.RevenueOrderIdsRequest(List.of("cod-order"))))
                .thenReturn(ApiResponse.success(List.of(candidate("cod-order", "cod", "PENDING", null))));
        when(orderRepository.aggregateRevenueForOrderIds(any(), eq(null), eq(null)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked") java.util.Collection<String> ids = invocation.getArgument(0);
                    return ids.contains("sepay-order")
                            ? aggregate("100.00", "10.00", "5.00", "115.00", 1)
                            : aggregate("200.00", "20.00", "0.00", "220.00", 1);
                });

        AdminRevenueService.RevenueSummary result = revenueService.calculate(from, to, null, null);

        assertThat(result.getRecognizedOrderCount()).isEqualTo(2);
        assertThat(result.getNetRevenue()).isEqualByComparingTo("300.00");
        assertThat(result.getVatCollected()).isEqualByComparingTo("30.00");
        assertThat(result.getShippingFeesCollected()).isEqualByComparingTo("5.00");
        assertThat(result.getGrossCustomerPayments()).isEqualByComparingTo("335.00");
        assertThat(result.getSepayRecognizedRevenue()).isEqualByComparingTo("100.00");
        assertThat(result.getCodRecognizedRevenue()).isEqualByComparingTo("200.00");
    }

    @Test
    void excludesFullyRefundedOrdersAndDoesNotFallbackFromNullSubtotalToTotal() {
        when(paymentClient.findSepayRevenueCandidates(
                from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .thenReturn(ApiResponse.success(List.of(
                        candidate("refunded-order", "sepay", "REFUNDED", "2026-07-10T10:00:00"),
                        candidate("legacy-order", "SEPAY_QR", "COMPLETED", "2026-07-10T10:00:00"))));
        when(auditLogRepository.findCompletedOrderIdsBetween(from, to)).thenReturn(List.of());
        when(orderRepository.findAllById(any())).thenReturn(List.of(
                Order.builder().id("refunded-order").build()));

        AdminRevenueService.RevenueSummary result = revenueService.calculate(from, to, null, null);

        assertThat(result.getRecognizedOrderCount()).isZero();
        assertThat(result.getNetRevenue()).isEqualByComparingTo("0.00");
        assertThat(result.getRefundAmount()).isEqualByComparingTo("115.00");
    }

    @Test
    void unboundedCalculationUsesExplicitIsoBoundsForInternalRevenueCalls() {
        when(paymentClient.findSepayRevenueCandidates(anyString(), anyString()))
                .thenReturn(ApiResponse.success(List.of()));
        when(auditLogRepository.findCompletedOrderIdsBetween(any(), any())).thenReturn(List.of());

        revenueService.calculate(null, null, null, null);

        verify(paymentClient).findSepayRevenueCandidates(
                eq("1970-01-01T00:00:00"),
                argThat(to -> LocalDateTime.parse(to, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .isAfter(LocalDateTime.of(2026, 7, 1, 0, 0))));
        verify(auditLogRepository).findCompletedOrderIdsBetween(
                eq(LocalDateTime.of(1970, 1, 1, 0, 0)),
                argThat(to -> to.isAfter(LocalDateTime.of(2026, 7, 1, 0, 0))));
    }

    private PaymentClient.PaymentRevenueCandidate candidate(
            String orderId, String method, String status, String paidAt) {
        return PaymentClient.PaymentRevenueCandidate.builder()
                .orderId(orderId)
                .method(method)
                .status(status)
                .amount(new BigDecimal("115.00"))
                .paidAt(paidAt == null ? null : LocalDateTime.parse(paidAt))
                .build();
    }

    private OrderRevenueAggregate aggregate(
            String net, String vat, String shipping, String gross, long count) {
        return new OrderRevenueAggregate(
                new BigDecimal(net), new BigDecimal(vat), new BigDecimal(shipping),
                new BigDecimal(gross), count);
    }
}
