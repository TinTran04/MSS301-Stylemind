package com.stylemind.payment.service.impl;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.payment.dto.CompleteRefundRequest;
import com.stylemind.payment.dto.CreateRefundRequest;
import com.stylemind.payment.dto.FailRefundRequest;
import com.stylemind.payment.dto.RefundResponse;
import com.stylemind.payment.entity.RefundStatus;
import com.stylemind.payment.entity.RefundTransaction;
import com.stylemind.payment.entity.Transaction;
import com.stylemind.payment.repository.RefundTransactionRepository;
import com.stylemind.payment.repository.TransactionRepository;
import com.stylemind.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundServiceImpl implements RefundService {

    private static final String METHOD_MANUAL_BANK_TRANSFER = "MANUAL_BANK_TRANSFER";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PAID_AFTER_CANCEL = "PAID_AFTER_CANCEL";

    private final TransactionRepository transactionRepository;
    private final RefundTransactionRepository refundRepository;

    @Override
    public RefundResponse createPendingRefund(CreateRefundRequest request) {
        return refundRepository.findByOrderCancellationId(request.getOrderCancellationId())
                .map(this::toResponse)
                .orElseGet(() -> createPendingRefundForTransaction(findPaidTransaction(request.getOrderId()), request.getOrderCancellationId()));
    }

    @Override
    public RefundResponse createRefundForLatePayment(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment transaction not found", 404));
        if (!StringUtils.hasText(transaction.getOrderCancellationId())) {
            throw new BusinessException("REFUND_NOT_REQUIRED", "Cancelled payment has no cancellation reference", 409);
        }
        return refundRepository.findByOrderCancellationId(transaction.getOrderCancellationId())
                .map(this::toResponse)
                .orElseGet(() -> createPendingRefundForTransaction(transaction, transaction.getOrderCancellationId()));
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundByOrderId(String orderId) {
        return refundRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    public RefundResponse completeRefund(String refundId, CompleteRefundRequest request) {
        RefundTransaction refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException("REFUND_NOT_FOUND", "Refund transaction not found", 404));
        if (refund.getStatus() == RefundStatus.REFUNDED) {
            return toResponse(refund);
        }
        if (refund.getStatus() != RefundStatus.REFUND_PENDING) {
            throw new BusinessException("REFUND_INVALID_STATUS", "Refund is not pending", 409);
        }
        if (!StringUtils.hasText(request.getProviderReference())) {
            throw new BusinessException("REFUND_PROVIDER_REFERENCE_REQUIRED", "Provider reference is required", 400);
        }
        refund.setStatus(RefundStatus.REFUNDED);
        refund.setProviderReference(trim(request.getProviderReference()));
        refund.setProofUrl(trim(request.getProofUrl()));
        refund.setNote(trim(request.getNote()));
        refund.setProcessedBy(trim(request.getProcessedBy()));
        refund.setProcessedAt(LocalDateTime.now());
        refund.setFailureReason(null);
        return toResponse(refundRepository.save(refund));
    }

    @Override
    public RefundResponse failRefund(String refundId, FailRefundRequest request) {
        RefundTransaction refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException("REFUND_NOT_FOUND", "Refund transaction not found", 404));
        if (refund.getStatus() != RefundStatus.REFUND_PENDING) {
            throw new BusinessException("REFUND_INVALID_STATUS", "Refund is not pending", 409);
        }
        refund.setStatus(RefundStatus.REFUND_FAILED);
        refund.setFailureReason(trim(request.getFailureReason()));
        refund.setProcessedBy(trim(request.getProcessedBy()));
        refund.setProcessedAt(LocalDateTime.now());
        return toResponse(refundRepository.save(refund));
    }

    private Transaction findPaidTransaction(String orderId) {
        return transactionRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .filter(transaction -> STATUS_PAID.equalsIgnoreCase(transaction.getStatus())
                        || STATUS_PAID_AFTER_CANCEL.equalsIgnoreCase(transaction.getStatus()))
                .orElseThrow(() -> new BusinessException("REFUND_NOT_REQUIRED", "Order does not have a paid transaction", 409));
    }

    private RefundResponse createPendingRefundForTransaction(Transaction transaction, String orderCancellationId) {
        if (!STATUS_PAID.equalsIgnoreCase(transaction.getStatus())
                && !STATUS_PAID_AFTER_CANCEL.equalsIgnoreCase(transaction.getStatus())) {
            throw new BusinessException("REFUND_NOT_REQUIRED", "Only paid transactions require refund", 409);
        }
        RefundTransaction refund = RefundTransaction.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(transaction.getOrderId())
                .paymentTransactionId(transaction.getId())
                .orderCancellationId(orderCancellationId)
                .amount(transaction.getAmount())
                .status(RefundStatus.REFUND_PENDING)
                .method(METHOD_MANUAL_BANK_TRANSFER)
                .build();
        return toResponse(refundRepository.save(refund));
    }

    private RefundResponse toResponse(RefundTransaction refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .orderId(refund.getOrderId())
                .paymentTransactionId(refund.getPaymentTransactionId())
                .orderCancellationId(refund.getOrderCancellationId())
                .amount(refund.getAmount())
                .status(refund.getStatus().name())
                .method(refund.getMethod())
                .providerReference(refund.getProviderReference())
                .proofUrl(refund.getProofUrl())
                .note(refund.getNote())
                .processedBy(refund.getProcessedBy())
                .processedAt(refund.getProcessedAt() == null ? null : refund.getProcessedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .failureReason(refund.getFailureReason())
                .createdAt(refund.getCreatedAt() == null ? null : refund.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(refund.getUpdatedAt() == null ? null : refund.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
