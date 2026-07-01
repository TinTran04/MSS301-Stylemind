package com.stylemind.payment.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.payment.dto.*;
import com.stylemind.payment.entity.Transaction;
import com.stylemind.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private static final String SANDBOX_SUCCESS_CODE = "123456";
    private static final String SANDBOX_FAILED_CODE = "000000";

    private final TransactionRepository transactionRepository;

    public PaymentResponse checkout(CheckoutRequest request) {
        Transaction transaction = Transaction.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(request.getOrderId())
                .userId("")
                .amount(request.getAmount())
                .method(request.getMethod())
                .status("PENDING")
                .transactionRef(StringUtil.generateUniqueId())
                .build();

        transaction = transactionRepository.save(transaction);

        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .build();
    }

    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", 404));

        if (!transaction.getOrderId().equals(request.getOrderId())) {
            throw new BusinessException("TRANSACTION_MISMATCH", "Transaction ID does not match Order ID", 400);
        }

        if (transaction.getAmount().compareTo(request.getAmount()) != 0) {
            throw new BusinessException("AMOUNT_MISMATCH", "Transaction amount does not match order amount", 400);
        }

        if (!"PENDING".equals(transaction.getStatus())) {
            return toResponse(transaction);
        }

        if (!"online_simulated".equals(transaction.getMethod())) {
            throw new BusinessException("INVALID_PAYMENT_METHOD", "Only online_simulated can be confirmed by sandbox code", 400);
        }

        String verificationCode = request.getVerificationCode();
        if (SANDBOX_SUCCESS_CODE.equals(verificationCode)) {
            transaction.setStatus("COMPLETED");
        } else if (SANDBOX_FAILED_CODE.equals(verificationCode)) {
            transaction.setStatus("FAILED");
        } else {
            throw new BusinessException("INVALID_SANDBOX_CODE", "Sandbox code must be 123456 or 000000", 400);
        }

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public void refund(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", 404));

        if (!"COMPLETED".equals(transaction.getStatus())) {
            throw new BusinessException("INVALID_REFUND", "Only completed transactions can be refunded", 400);
        }

        transaction.setStatus("REFUNDED");
        transactionRepository.save(transaction);
    }

    private PaymentResponse toResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .build();
    }
}
