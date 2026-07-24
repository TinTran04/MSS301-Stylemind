package com.stylemind.order.service;

import com.stylemind.order.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReturnService {

    // Customer operations
    ReturnResponse createReturnRequest(String userId, String orderId, CreateReturnRequest request);

    List<ReturnResponse> getCustomerReturns(String userId, String orderId);

    ReturnResponse getCustomerReturnById(String userId, String returnId);

    ReturnResponse cancelReturnRequest(String userId, String returnId);

    ReturnResponse submitShipment(String userId, String returnId, SubmitShipmentRequest request);

    // Admin operations
    Page<ReturnResponse> adminGetReturns(String status, Pageable pageable);

    ReturnResponse adminGetReturnById(String returnId);

    ReturnResponse adminReviewReturn(String adminUserId, String returnId, AdminReviewReturnRequest request);

    ReturnResponse adminReceiveAndQc(String adminUserId, String returnId, AdminQcRequest request);
}
