package com.stylemind.order.service.impl;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.dto.*;
import com.stylemind.order.entity.*;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.feign.ProductClient;
import com.stylemind.order.repository.*;
import com.stylemind.order.service.ReturnEligibilityService;
import com.stylemind.order.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnEligibilityService eligibilityService;
    private final PaymentClient paymentClient;
    private final ProductClient productClient;

    @Override
    public ReturnResponse createReturnRequest(String userId, String orderId, CreateReturnRequest request) {
        log.info("Creating return request for orderId: {}, userId: {}", orderId, userId);

        ReturnEligibilityResponse eligibility = eligibilityService.evaluateEligibility(userId, orderId);
        if (!eligibility.isEligible()) {
            throw new BusinessException(eligibility.getReasonCode(), eligibility.getMessage(), 400);
        }

        ReturnReason reason;
        try {
            reason = ReturnReason.valueOf(request.getReason().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_RETURN_REASON", "Lý do trả hàng không hợp lệ", 400);
        }

        if (reason.isEvidenceRequired() && (request.getEvidences() == null || request.getEvidences().isEmpty())) {
            throw new BusinessException("RETURN_EVIDENCE_REQUIRED", "Bắt buộc phải tải ảnh hoặc video bằng chứng đối với lý do này", 400);
        }

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));

        Map<String, OrderItem> orderItemMap = orderItemRepository.findByOrderId(orderId).stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));

        List<ReturnItem> returnItems = new ArrayList<>();
        String requestId = "ret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        for (ReturnItemRequest itemReq : request.getItems()) {
            OrderItem orderItem = orderItemMap.get(itemReq.getOrderItemId());
            if (orderItem == null) {
                throw new BusinessException("RETURN_ITEM_NOT_IN_ORDER", "Sản phẩm không thuộc đơn hàng này: " + itemReq.getOrderItemId(), 400);
            }

            int remainingQty = eligibilityService.computeRemainingReturnableQuantity(orderId, orderItem.getId(), orderItem.getQuantity());
            if (itemReq.getQuantity() > remainingQty) {
                throw new BusinessException("RETURN_QUANTITY_EXCEEDS_AVAILABLE",
                        "Số lượng yêu cầu trả vượt quá số lượng khả dụng cho item: " + orderItem.getId(), 400);
            }

            ReturnItem returnItem = ReturnItem.builder()
                    .id("ri_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                    .orderItemId(orderItem.getId())
                    .productId(orderItem.getId()) // Or variant mapping
                    .variantId(orderItem.getVariantId())
                    .quantity(itemReq.getQuantity())
                    .restockStatus("PENDING")
                    .build();
            returnItems.add(returnItem);
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .id(requestId)
                .orderId(orderId)
                .userId(userId)
                .status(ReturnStatus.REQUESTED)
                .reason(reason)
                .customerNote(request.getCustomerNote())
                .isPhysicalReturn(true)
                .payoutState("PROVIDED")
                .requestedAt(LocalDateTime.now())
                .build();

        for (ReturnItem item : returnItems) {
            returnRequest.addItem(item);
        }

        if (request.getEvidences() != null) {
            for (EvidenceRequest evReq : request.getEvidences()) {
                ReturnEvidence evidence = ReturnEvidence.builder()
                        .id("ev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                        .publicId(evReq.getPublicId())
                        .secureUrl(evReq.getSecureUrl())
                        .resourceType(evReq.getResourceType() != null ? evReq.getResourceType() : "image")
                        .uploadedAt(LocalDateTime.now())
                        .build();
                returnRequest.addEvidence(evidence);
            }
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getCustomerReturns(String userId, String orderId) {
        return returnRequestRepository.findByUserIdAndOrderIdOrderByRequestedAtDesc(userId, orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getCustomerReturnById(String userId, String returnId) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException("RETURN_NOT_FOUND", "Yêu cầu trả hàng không tồn tại", 404));
        return mapToResponse(returnRequest);
    }

    @Override
    public ReturnResponse cancelReturnRequest(String userId, String returnId) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException("RETURN_NOT_FOUND", "Yêu cầu trả hàng không tồn tại", 404));

        if (returnRequest.getStatus() != ReturnStatus.REQUESTED) {
            throw new BusinessException("RETURN_STATE_CONFLICT", "Chỉ có thể hủy yêu cầu khi đang ở trạng thái Chờ duyệt", 409);
        }

        returnRequest.setStatus(ReturnStatus.CANCELLED);
        returnRequest.setClosedAt(LocalDateTime.now());
        return mapToResponse(returnRequestRepository.save(returnRequest));
    }

    @Override
    public ReturnResponse submitShipment(String userId, String returnId, SubmitShipmentRequest request) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException("RETURN_NOT_FOUND", "Yêu cầu trả hàng không tồn tại", 404));

        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
            throw new BusinessException("RETURN_STATE_CONFLICT", "Chỉ có thể nhập mã vận đơn khi yêu cầu đã được duyệt", 409);
        }

        ReturnShipment shipment = ReturnShipment.builder()
                .id("ship_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .trackingCode(request.getTrackingCode())
                .carrierName(request.getCarrierName())
                .shippedAt(LocalDateTime.now())
                .build();

        returnRequest.setShipment(shipment);
        returnRequest.setStatus(ReturnStatus.RETURN_IN_TRANSIT);

        return mapToResponse(returnRequestRepository.save(returnRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnResponse> adminGetReturns(String statusStr, Pageable pageable) {
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                ReturnStatus status = ReturnStatus.valueOf(statusStr.toUpperCase());
                return returnRequestRepository.findByStatus(status, pageable).map(this::mapToResponse);
            } catch (IllegalArgumentException ignored) {}
        }
        return returnRequestRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse adminGetReturnById(String returnId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException("RETURN_NOT_FOUND", "Yêu cầu trả hàng không tồn tại", 404));
        return mapToResponse(returnRequest);
    }

    @Override
    public ReturnResponse adminReviewReturn(String adminUserId, String returnId, AdminReviewReturnRequest request) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new BusinessException("RETURN_NOT_FOUND", "Yêu cầu trả hàng không tồn tại", 404));

        if (returnRequest.getStatus() != ReturnStatus.REQUESTED && returnRequest.getStatus() != ReturnStatus.UNDER_REVIEW) {
            throw new BusinessException("RETURN_STATE_CONFLICT", "Yêu cầu trả hàng không ở trạng thái chờ duyệt", 409);
        }

        String action = request.getAction().toUpperCase();
        returnRequest.setReviewedBy(adminUserId);
        returnRequest.setReviewedAt(LocalDateTime.now());
        returnRequest.setAdminNote(request.getAdminNote());

        if ("REJECT".equals(action)) {
            returnRequest.setStatus(ReturnStatus.REJECTED);
            returnRequest.setRejectionReason(request.getRejectionReason());
            returnRequest.setClosedAt(LocalDateTime.now());
        } else if ("APPROVE".equals(action)) {
            boolean isPhysical = request.getIsPhysicalReturn() != null ? request.getIsPhysicalReturn() : true;
            returnRequest.setIsPhysicalReturn(isPhysical);
            returnRequest.setStatus(ReturnStatus.APPROVED);

            if (!isPhysical) { // No-return refund flow
                triggerRefundCreation(returnRequest);
            }
        } else {
            throw new BusinessException("INVALID_ACTION", "Hành động không hợp lệ", 400);
        }

        return mapToResponse(returnRequestRepository.save(returnRequest));
    }

    @Override
    public ReturnResponse adminReceiveAndQc(String adminUserId, String returnId, AdminQcRequest request) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new BusinessException("RETURN_NOT_FOUND", "Yêu cầu trả hàng không tồn tại", 404));

        if (returnRequest.getStatus() != ReturnStatus.APPROVED && returnRequest.getStatus() != ReturnStatus.RETURN_IN_TRANSIT
                && returnRequest.getStatus() != ReturnStatus.RECEIVED) {
            throw new BusinessException("RETURN_STATE_CONFLICT", "Yêu cầu trả hàng chưa được duyệt hoặc chưa gửi hàng", 409);
        }

        if (returnRequest.getShipment() != null) {
            returnRequest.getShipment().setReceivedAt(LocalDateTime.now());
        }

        returnRequest.setAdminNote(request.getAdminNote());
        returnRequest.setQcCompletedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(request.getQcPassed())) {
            returnRequest.setStatus(ReturnStatus.QC_PASSED);

            // Restock inventory idempotently
            restockItems(returnRequest);

            // Trigger refund creation in payment-service
            triggerRefundCreation(returnRequest);
        } else {
            returnRequest.setStatus(ReturnStatus.QC_FAILED);
            returnRequest.setClosedAt(LocalDateTime.now());
        }

        return mapToResponse(returnRequestRepository.save(returnRequest));
    }

    private void restockItems(ReturnRequest returnRequest) {
        for (ReturnItem item : returnRequest.getItems()) {
            if ("RESTOCKED".equals(item.getRestockStatus())) continue;

            try {
                String opKey = "return:" + item.getId() + ":restock";
                productClient.restockInventory(ProductClient.ReturnRestockRequest.builder()
                        .operationKey(opKey)
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .reason("RETURN_QC_PASSED")
                        .referenceId(returnRequest.getId())
                        .build());
                item.setRestockStatus("RESTOCKED");
            } catch (Exception e) {
                log.error("Failed to restock item: {}", item.getId(), e);
                item.setRestockStatus("FAILED");
            }
        }
    }

    private void triggerRefundCreation(ReturnRequest returnRequest) {
        try {
            Order order = orderRepository.findById(returnRequest.getOrderId()).orElse(null);
            if (order == null) return;

            Map<String, OrderItem> orderItemMap = orderItemRepository.findByOrderId(order.getId()).stream()
                    .collect(Collectors.toMap(OrderItem::getId, item -> item));

            BigDecimal merchandiseAmount = BigDecimal.ZERO;
            for (ReturnItem item : returnRequest.getItems()) {
                OrderItem orderItem = orderItemMap.get(item.getOrderItemId());
                if (orderItem != null && orderItem.getPriceAtPurchase() != null) {
                    merchandiseAmount = merchandiseAmount.add(orderItem.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }

            var paymentResp = paymentClient.createRefund(PaymentClient.CreateRefundRequest.builder()
                    .orderId(returnRequest.getOrderId())
                    .returnRequestId(returnRequest.getId())
                    .merchandiseAmount(merchandiseAmount)
                    .taxAmount(BigDecimal.ZERO)
                    .shippingAmount(BigDecimal.ZERO)
                    .reason("RETURN_REFUND: " + returnRequest.getReason())
                    .build());

            if (paymentResp != null && paymentResp.getData() != null) {
                returnRequest.setRefundId(paymentResp.getData().getId());
            }
        } catch (Exception e) {
            log.error("Failed to trigger refund creation for returnRequestId: {}", returnRequest.getId(), e);
        }
    }

    private ReturnResponse mapToResponse(ReturnRequest entity) {
        List<ReturnItemResponse> itemResponses = entity.getItems().stream()
                .map(item -> ReturnItemResponse.builder()
                        .id(item.getId())
                        .orderItemId(item.getOrderItemId())
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .restockStatus(item.getRestockStatus())
                        .build())
                .collect(Collectors.toList());

        List<ReturnEvidenceResponse> evidenceResponses = entity.getEvidences().stream()
                .map(ev -> ReturnEvidenceResponse.builder()
                        .id(ev.getId())
                        .publicId(ev.getPublicId())
                        .secureUrl(ev.getSecureUrl())
                        .resourceType(ev.getResourceType())
                        .uploadedAt(ev.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        ReturnShipmentResponse shipmentResponse = null;
        if (entity.getShipment() != null) {
            shipmentResponse = ReturnShipmentResponse.builder()
                    .id(entity.getShipment().getId())
                    .trackingCode(entity.getShipment().getTrackingCode())
                    .carrierName(entity.getShipment().getCarrierName())
                    .shippedAt(entity.getShipment().getShippedAt())
                    .receivedAt(entity.getShipment().getReceivedAt())
                    .build();
        }

        return ReturnResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .status(entity.getStatus().name())
                .reason(entity.getReason().name())
                .customerNote(entity.getCustomerNote())
                .adminNote(entity.getAdminNote())
                .rejectionReason(entity.getRejectionReason())
                .isPhysicalReturn(entity.getIsPhysicalReturn())
                .payoutState(entity.getPayoutState())
                .refundId(entity.getRefundId())
                .reviewedBy(entity.getReviewedBy())
                .requestedAt(entity.getRequestedAt())
                .reviewedAt(entity.getReviewedAt())
                .qcCompletedAt(entity.getQcCompletedAt())
                .closedAt(entity.getClosedAt())
                .items(itemResponses)
                .evidences(evidenceResponses)
                .shipment(shipmentResponse)
                .build();
    }
}
