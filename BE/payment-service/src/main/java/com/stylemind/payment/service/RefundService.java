package com.stylemind.payment.service;

import com.stylemind.payment.dto.CompleteRefundRequest;
import com.stylemind.payment.dto.CreateRefundRequest;
import com.stylemind.payment.dto.FailRefundRequest;
import com.stylemind.payment.dto.RefundResponse;

public interface RefundService {
    RefundResponse createPendingRefund(CreateRefundRequest request);
    RefundResponse createRefundForLatePayment(String transactionId);
    RefundResponse getRefundByOrderId(String orderId);
    RefundResponse completeRefund(String refundId, CompleteRefundRequest request);
    RefundResponse failRefund(String refundId, FailRefundRequest request);
    java.util.List<RefundResponse> getAllRefunds();
}
