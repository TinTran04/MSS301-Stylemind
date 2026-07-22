package com.stylemind.order.service;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.dto.OrderRevenueAggregate;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.repository.OrderStatusAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRevenueService {

    private static final String METHOD_SEPAY = "sepay";
    private static final String METHOD_COD = "cod";
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_REFUNDED = "refunded";

    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final OrderStatusAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public RevenueSummary calculate(
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive,
            OrderStatus statusFilter,
            String userId) {
        List<PaymentClient.PaymentRevenueCandidate> sepayCandidates = responseData(
                paymentClient.findSepayRevenueCandidates(fromInclusive, toExclusive));

        Set<String> refundedSepayIds = sepayCandidates.stream()
                .filter(this::isSepay)
                .filter(candidate -> hasStatus(candidate, STATUS_REFUNDED))
                .map(PaymentClient.PaymentRevenueCandidate::getOrderId)
                .collect(Collectors.toSet());

        Set<String> sepayOrderIds = sepayCandidates.stream()
                .filter(this::isSepay)
                .filter(candidate -> hasStatus(candidate, STATUS_PAID))
                .filter(candidate -> candidate.getPaidAt() != null)
                .filter(candidate -> isWithin(candidate.getPaidAt(), fromInclusive, toExclusive))
                .map(PaymentClient.PaymentRevenueCandidate::getOrderId)
                .filter(orderId -> !refundedSepayIds.contains(orderId))
                .collect(Collectors.toCollection(HashSet::new));

        List<String> completedCodOrderIds = auditLogRepository.findCompletedOrderIdsBetween(
                fromInclusive, toExclusive);
        List<PaymentClient.PaymentRevenueCandidate> codCandidates = completedCodOrderIds.isEmpty()
                ? List.of()
                : responseData(paymentClient.findRevenueCandidatesByOrderIds(
                        new PaymentClient.RevenueOrderIdsRequest(completedCodOrderIds)));

        Map<String, List<PaymentClient.PaymentRevenueCandidate>> codByOrder = codCandidates.stream()
                .filter(candidate -> hasMethod(candidate, METHOD_COD))
                .collect(Collectors.groupingBy(PaymentClient.PaymentRevenueCandidate::getOrderId));

        Set<String> refundedCodIds = codByOrder.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(candidate -> hasStatus(candidate, STATUS_REFUNDED)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<String> codOrderIds = new HashSet<>(codByOrder.keySet());
        codOrderIds.removeAll(refundedCodIds);

        OrderRevenueAggregate sepay = aggregate(sepayOrderIds, statusFilter, userId);
        OrderRevenueAggregate cod = aggregate(codOrderIds, statusFilter, userId);

        Set<String> refundedOrderIds = new HashSet<>(refundedSepayIds);
        refundedOrderIds.addAll(refundedCodIds);
        BigDecimal refundAmount = refundAmount(refundedOrderIds, sepayCandidates, codCandidates, statusFilter, userId);

        return RevenueSummary.builder()
                .netRevenue(add(sepay.netRevenue(), cod.netRevenue()))
                .vatCollected(add(sepay.vatCollected(), cod.vatCollected()))
                .shippingFeesCollected(add(sepay.shippingFeesCollected(), cod.shippingFeesCollected()))
                .grossCustomerPayments(add(sepay.grossCustomerPayments(), cod.grossCustomerPayments()))
                .refundAmount(refundAmount)
                .recognizedOrderCount(sepay.orderCount() + cod.orderCount())
                .sepayRecognizedRevenue(sepay.netRevenue())
                .codRecognizedRevenue(cod.netRevenue())
                .build();
    }

    private OrderRevenueAggregate aggregate(Set<String> orderIds, OrderStatus statusFilter, String userId) {
        if (orderIds.isEmpty()) {
            return OrderRevenueAggregate.zero();
        }
        OrderRevenueAggregate aggregate = orderRepository.aggregateRevenueForOrderIds(orderIds, statusFilter, userId);
        return aggregate == null ? OrderRevenueAggregate.zero() : aggregate;
    }

    private BigDecimal refundAmount(
            Set<String> refundedOrderIds,
            List<PaymentClient.PaymentRevenueCandidate> sepayCandidates,
            List<PaymentClient.PaymentRevenueCandidate> codCandidates,
            OrderStatus statusFilter,
            String userId) {
        if (refundedOrderIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> visibleOrderIds = orderRepository.findAllById(refundedOrderIds).stream()
                .filter(order -> statusFilter == null || order.getOrderStatus() == statusFilter)
                .filter(order -> userId == null || userId.equals(order.getUserId()))
                .map(Order::getId)
                .collect(Collectors.toSet());
        return StreamSupport.concat(sepayCandidates, codCandidates)
                .filter(candidate -> hasStatus(candidate, STATUS_REFUNDED))
                .filter(candidate -> visibleOrderIds.contains(candidate.getOrderId()))
                .map(PaymentClient.PaymentRevenueCandidate::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> List<T> responseData(ApiResponse<List<T>> response) {
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private boolean isSepay(PaymentClient.PaymentRevenueCandidate candidate) {
        return hasMethod(candidate, METHOD_SEPAY);
    }

    private boolean hasMethod(PaymentClient.PaymentRevenueCandidate candidate, String method) {
        return candidate.getMethod() != null && method.equalsIgnoreCase(candidate.getMethod());
    }

    private boolean hasStatus(PaymentClient.PaymentRevenueCandidate candidate, String status) {
        return candidate.getStatus() != null && status.equalsIgnoreCase(candidate.getStatus());
    }

    private boolean isWithin(LocalDateTime value, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return (fromInclusive == null || !value.isBefore(fromInclusive))
                && (toExclusive == null || value.isBefore(toExclusive));
    }

    private BigDecimal add(BigDecimal first, BigDecimal second) {
        return first.add(second);
    }

    private static final class StreamSupport {
        private StreamSupport() {
        }

        private static java.util.stream.Stream<PaymentClient.PaymentRevenueCandidate> concat(
                List<PaymentClient.PaymentRevenueCandidate> first,
                List<PaymentClient.PaymentRevenueCandidate> second) {
            return java.util.stream.Stream.concat(first.stream(), second.stream());
        }
    }

    @lombok.Value
    @lombok.Builder
    public static class RevenueSummary {
        BigDecimal netRevenue;
        BigDecimal vatCollected;
        BigDecimal shippingFeesCollected;
        BigDecimal grossCustomerPayments;
        BigDecimal refundAmount;
        long recognizedOrderCount;
        BigDecimal sepayRecognizedRevenue;
        BigDecimal codRecognizedRevenue;
    }
}
