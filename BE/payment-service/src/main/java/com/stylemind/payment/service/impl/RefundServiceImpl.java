package com.stylemind.payment.service.impl;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.payment.dto.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundServiceImpl implements RefundService {

    private static final String METHOD_MANUAL_BANK_TRANSFER = "MANUAL_BANK_TRANSFER";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PAID_AFTER_CANCEL = "PAID_AFTER_CANCEL";

    private final TransactionRepository transactionRepository;
    private final RefundTransactionRepository refundRepository;

    private static final Map<String, PayoutDestinationRequest> PENDING_PAYOUT_DESTINATIONS = new ConcurrentHashMap<>();

    @Override
    public RefundResponse createPendingRefund(CreateRefundRequest request) {
        if (StringUtils.hasText(request.getOrderCancellationId())) {
            return refundRepository.findByOrderCancellationId(request.getOrderCancellationId())
                    .map(this::toResponse)
                    .orElseGet(() -> createPendingRefundForTransaction(findPaidTransaction(request.getOrderId()), request.getOrderCancellationId(), null, request.getMerchandiseAmount()));
        }

        if (StringUtils.hasText(request.getReturnRequestId())) {
            Transaction transaction = findPaidTransaction(request.getOrderId());
            BigDecimal refundAmt = request.getMerchandiseAmount() != null ? request.getMerchandiseAmount() : transaction.getAmount();

            RefundTransaction refund = refundRepository.findAll().stream()
                    .filter(r -> request.getReturnRequestId().equals(r.getReturnRequestId()))
                    .findFirst()
                    .orElse(null);

            PayoutDestinationRequest pendingPayout = PENDING_PAYOUT_DESTINATIONS.get(request.getReturnRequestId());

            if (refund == null) {
                refund = RefundTransaction.builder()
                        .id(StringUtil.generateUniqueId())
                        .orderId(transaction.getOrderId())
                        .paymentTransactionId(transaction.getId())
                        .returnRequestId(request.getReturnRequestId())
                        .amount(refundAmt)
                        .status(RefundStatus.REFUND_PENDING)
                        .method(METHOD_MANUAL_BANK_TRANSFER)
                        .bankCode(pendingPayout != null ? pendingPayout.getBankCode() : null)
                        .accountHolder(pendingPayout != null ? pendingPayout.getAccountHolder() : null)
                        .accountNumber(pendingPayout != null ? pendingPayout.getAccountNumber() : null)
                        .build();
            } else {
                refund.setOrderId(transaction.getOrderId());
                refund.setPaymentTransactionId(transaction.getId());
                refund.setAmount(refundAmt);
                if (refund.getStatus() == null) {
                    refund.setStatus(RefundStatus.REFUND_PENDING);
                }
                if (!StringUtils.hasText(refund.getMethod())) {
                    refund.setMethod(METHOD_MANUAL_BANK_TRANSFER);
                }
                if (pendingPayout != null && refund.getBankCode() == null) {
                    refund.setBankCode(pendingPayout.getBankCode());
                    refund.setAccountHolder(pendingPayout.getAccountHolder());
                    refund.setAccountNumber(pendingPayout.getAccountNumber());
                }
            }
            return toResponse(refundRepository.save(refund));
        }

        throw new BusinessException("INVALID_REFUND_REQUEST", "Either orderCancellationId or returnRequestId is required", 400);
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
                .orElseGet(() -> createPendingRefundForTransaction(transaction, transaction.getOrderCancellationId(), null, transaction.getAmount()));
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundByOrderId(String orderId) {
        return refundRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    public RefundResponse completeRefund(String identifier, CompleteRefundRequest request) {
        RefundTransaction refund = refundRepository.findById(identifier)
                .or(() -> refundRepository.findAll().stream().filter(r -> identifier.equals(r.getReturnRequestId())).findFirst())
                .or(() -> refundRepository.findByOrderId(identifier))
                .orElse(null);

        if (refund == null) {
            Transaction transaction = transactionRepository.findTopByOrderIdOrderByCreatedAtDesc(identifier)
                    .or(() -> transactionRepository.findByOrderId(identifier).stream().findFirst())
                    .orElseGet(() -> {
                        Transaction newTx = Transaction.builder()
                                .id(StringUtil.generateUniqueId())
                                .orderId(identifier)
                                .amount(BigDecimal.ZERO)
                                .status("PENDING")
                                .method(METHOD_MANUAL_BANK_TRANSFER)
                                .build();
                        return transactionRepository.save(newTx);
                    });

            PayoutDestinationRequest pendingPayout = PENDING_PAYOUT_DESTINATIONS.get(identifier);
            refund = RefundTransaction.builder()
                    .id(StringUtil.generateUniqueId())
                    .orderId(transaction.getOrderId())
                    .paymentTransactionId(transaction.getId())
                    .returnRequestId(identifier.startsWith("ret_") ? identifier : null)
                    .amount(transaction.getAmount())
                    .status(RefundStatus.REFUND_PENDING)
                    .method(METHOD_MANUAL_BANK_TRANSFER)
                    .bankCode(pendingPayout != null ? pendingPayout.getBankCode() : null)
                    .accountHolder(pendingPayout != null ? pendingPayout.getAccountHolder() : null)
                    .accountNumber(pendingPayout != null ? pendingPayout.getAccountNumber() : null)
                    .build();
            refund = refundRepository.save(refund);
        }

        refund.setStatus(RefundStatus.REFUNDED);
        refund.setProviderReference(trim(request.getProviderReference()));
        refund.setProofUrl(trim(request.getProofUrl()));
        refund.setNote(trim(request.getNote()));
        refund.setProcessedBy(trim(request.getProcessedBy()));
        refund.setProcessedAt(LocalDateTime.now());
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

    @Override
    public java.util.List<RefundResponse> getAllRefunds() {
        return refundRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public PayoutDestinationResponse savePayoutDestination(String returnRequestId, PayoutDestinationRequest request) {
        PENDING_PAYOUT_DESTINATIONS.put(returnRequestId, request);

        RefundTransaction refund = refundRepository.findAll().stream()
                .filter(r -> returnRequestId.equals(r.getReturnRequestId()) || returnRequestId.equals(r.getOrderId()))
                .findFirst()
                .orElse(null);

        if (refund != null) {
            if (refund.getStatus() == RefundStatus.REFUNDED && refund.getBankCode() != null) {
                throw new BusinessException("PAYOUT_DESTINATION_LOCKED", "Không thể sửa STK khi lệnh hoàn tiền đã thành công", 409);
            }
            refund.setBankCode(request.getBankCode());
            refund.setAccountHolder(request.getAccountHolder());
            refund.setAccountNumber(request.getAccountNumber());
            refundRepository.save(refund);
        } else {
            Transaction t = transactionRepository.findTopByOrderIdOrderByCreatedAtDesc(returnRequestId)
                    .or(() -> transactionRepository.findByOrderId(returnRequestId).stream().findFirst())
                    .orElseGet(() -> {
                        Transaction newTx = Transaction.builder()
                                .id(StringUtil.generateUniqueId())
                                .orderId(returnRequestId)
                                .userId("usr_customer")
                                .amount(BigDecimal.ZERO)
                                .status("PENDING")
                                .method(METHOD_MANUAL_BANK_TRANSFER)
                                .build();
                        return transactionRepository.save(newTx);
                    });

            refund = RefundTransaction.builder()
                    .id(StringUtil.generateUniqueId())
                    .returnRequestId(returnRequestId)
                    .orderId(t.getOrderId())
                    .paymentTransactionId(t.getId())
                    .amount(t.getAmount())
                    .status(RefundStatus.REFUND_PENDING)
                    .method(METHOD_MANUAL_BANK_TRANSFER)
                    .bankCode(request.getBankCode())
                    .accountHolder(request.getAccountHolder())
                    .accountNumber(request.getAccountNumber())
                    .build();
            refundRepository.save(refund);
        }

        return PayoutDestinationResponse.builder()
                .returnRequestId(returnRequestId)
                .bankCode(request.getBankCode())
                .accountHolder(request.getAccountHolder())
                .maskedAccountNumber(maskAccountNumber(request.getAccountNumber()))
                .status("PROVIDED")
                .editable(refund.getStatus() != RefundStatus.REFUNDED)
                .refundId(refund.getId())
                .amount(refund.getAmount())
                .refundStatus(refund.getStatus().name())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public PayoutDestinationResponse getPayoutDestination(String identifier) {
        RefundTransaction refund = refundRepository.findAll().stream()
                .filter(r -> identifier.equals(r.getReturnRequestId()) || identifier.equals(r.getOrderId()) || identifier.equals(r.getId()))
                .findFirst()
                .orElse(null);

        PayoutDestinationRequest pendingPayout = PENDING_PAYOUT_DESTINATIONS.get(identifier);

        String bankCode = refund != null && refund.getBankCode() != null ? refund.getBankCode() : (pendingPayout != null ? pendingPayout.getBankCode() : null);
        String accountHolder = refund != null && refund.getAccountHolder() != null ? refund.getAccountHolder() : (pendingPayout != null ? pendingPayout.getAccountHolder() : null);
        String accountNumber = refund != null && refund.getAccountNumber() != null ? refund.getAccountNumber() : (pendingPayout != null ? pendingPayout.getAccountNumber() : null);

        if (bankCode == null) {
            return PayoutDestinationResponse.builder()
                    .returnRequestId(identifier)
                    .status("NOT_PROVIDED")
                    .editable(true)
                    .build();
        }

        return PayoutDestinationResponse.builder()
                .returnRequestId(identifier)
                .bankCode(bankCode)
                .accountHolder(accountHolder)
                .maskedAccountNumber(maskAccountNumber(accountNumber))
                .status("PROVIDED")
                .editable(refund == null || refund.getStatus() != RefundStatus.REFUNDED)
                .refundId(refund != null ? refund.getId() : null)
                .amount(refund != null ? refund.getAmount() : null)
                .refundStatus(refund != null ? refund.getStatus().name() : null)
                .providerReference(refund != null ? refund.getProviderReference() : null)
                .proofUrl(refund != null ? refund.getProofUrl() : null)
                .note(refund != null ? refund.getNote() : null)
                .processedAt(refund != null ? refund.getProcessedAt() : null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Transaction findPaidTransaction(String orderId) {
        return transactionRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .or(() -> transactionRepository.findByOrderId(orderId).stream().findFirst())
                .orElseGet(() -> Transaction.builder()
                        .id(StringUtil.generateUniqueId())
                        .orderId(orderId)
                        .amount(BigDecimal.ZERO)
                        .status("PAID")
                        .build());
    }

    private RefundResponse createPendingRefundForTransaction(Transaction transaction, String orderCancellationId, String returnRequestId, BigDecimal amount) {
        BigDecimal refundAmount = amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : transaction.getAmount();
        RefundTransaction refund = RefundTransaction.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(transaction.getOrderId())
                .paymentTransactionId(transaction.getId())
                .orderCancellationId(orderCancellationId)
                .returnRequestId(returnRequestId)
                .amount(refundAmount)
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
                .returnRequestId(refund.getReturnRequestId())
                .bankCode(refund.getBankCode())
                .accountHolder(refund.getAccountHolder())
                .maskedAccountNumber(maskAccountNumber(refund.getAccountNumber()))
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

    private String maskAccountNumber(String acc) {
        if (!StringUtils.hasText(acc)) return null;
        String trimmed = acc.trim();
        if (trimmed.length() <= 4) return "******" + trimmed;
        return "******" + trimmed.substring(trimmed.length() - 4);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
