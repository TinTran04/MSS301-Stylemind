package com.stylemind.order.service;

import com.stylemind.order.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OrderReturnService {
    OrderReturnRequestResponse requestCustomerReturn(
            String userId,
            String orderId,
            String idempotencyKey,
            CreateOrderReturnRequest request,
            List<MultipartFile> images);

    List<OrderReturnRequestResponse> getCustomerReturns(String userId, String orderId);

    OrderReturnRequestResponse submitBankInfo(
            String userId,
            String orderId,
            String returnRequestId,
            SubmitReturnBankInfoRequest request);

    OrderReturnRequestResponse approveReturn(String adminUserId, String returnRequestId);

    OrderReturnRequestResponse rejectReturn(
            String adminUserId,
            String returnRequestId,
            RejectOrderReturnRequest request,
            List<MultipartFile> images);

    OrderReturnRequestResponse completeReturn(
            String adminUserId,
            String orderId,
            String returnRequestId,
            CompleteOrderReturnRequest request,
            List<MultipartFile> billImages);
}
