package com.stylemind.payment.service;

import com.stylemind.payment.dto.*;
import com.stylemind.payment.entity.Transaction;
import com.stylemind.payment.repository.TransactionRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final TransactionRepository transactionRepository;

    public PaymentResponse checkout(CheckoutRequest request) {
        // Create transaction record
        Transaction transaction = Transaction.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(request.getOrderId())
                .userId("") // Will be filled by order service
                .amount(request.getAmount())
                .method(request.getMethod())
                .status("PENDING")
                .transactionRef(StringUtil.generateUniqueId())
                .build();

        transaction = transactionRepository.save(transaction);

        // Simulate payment processing
        if ("online_simulated".equals(request.getMethod())) {
            // In real implementation, integrate with payment gateway
            // For now, always succeed
            transaction.setStatus("COMPLETED");
            transaction = transactionRepository.save(transaction);
            
            return PaymentResponse.builder()
                    .transactionId(transaction.getId())
                    .status("COMPLETED")
                    .amount(transaction.getAmount())
                    .build();
        } else {
            // COD - pending until delivery
            return PaymentResponse.builder()
                    .transactionId(transaction.getId())
                    .status("PENDING")
                    .amount(transaction.getAmount())
                    .build();
        }
    }

    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Không tìm thấy giao dịch", 404));

        if (!transaction.getOrderId().equals(request.getOrderId())) {
            throw new BusinessException("TRANSACTION_MISMATCH", "Transaction ID không khớp với Order ID", 400);
        }

        // Simulate payment gateway call
        boolean success = simulatePaymentGateway(request.getAmount());
        
        if (success) {
            transaction.setStatus("COMPLETED");
        } else {
            transaction.setStatus("FAILED");
        }
        
        transaction = transactionRepository.save(transaction);

        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .build();
    }

    private boolean simulatePaymentGateway(BigDecimal amount) {
        // Simulate 99% success rate
        return Math.random() > 0.01;
    }

    public void refund(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Không tìm thấy giao dịch", 404));

        if (!"COMPLETED".equals(transaction.getStatus())) {
            throw new BusinessException("INVALID_REFUND", "Chỉ có thể hoàn tiền giao dịch đã hoàn thành", 400);
        }

        transaction.setStatus("REFUNDED");
        transactionRepository.save(transaction);
    }
}