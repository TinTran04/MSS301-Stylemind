package com.stylemind.order.service.impl;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.order.dto.*;
import com.stylemind.order.entity.*;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.repository.OrderReturnAttachmentRepository;
import com.stylemind.order.repository.OrderReturnRequestRepository;
import com.stylemind.order.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderReturnServiceImpl implements OrderReturnService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;
    private static final int MAX_IMAGES_PER_STEP = 5;

    private final OrderRepository orderRepository;
    private final OrderReturnRequestRepository returnRequestRepository;
    private final OrderReturnAttachmentRepository attachmentRepository;
    private final PaymentClient paymentClient;

    @Override
    public OrderReturnRequestResponse requestCustomerReturn(
            String userId,
            String orderId,
            String idempotencyKey,
            CreateOrderReturnRequest request,
            List<MultipartFile> images) {
        String normalizedKey = trim(idempotencyKey);
        if (StringUtils.hasText(normalizedKey)) {
            var existing = returnRequestRepository.findByRequestedByAndOrderIdAndIdempotencyKey(userId, orderId, normalizedKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        ensureCodCompletedOrder(order);
        if (returnRequestRepository.existsByOrderId(orderId)) {
            throw new BusinessException("ORDER_RETURN_ALREADY_EXISTS", "Đơn hàng đã có yêu cầu hoàn hàng.", 409);
        }
        List<MultipartFile> validImages = requireImages(images, "RETURN_CUSTOMER_IMAGE_REQUIRED");
        String reason = normalizeReason(request == null ? null : request.getReasonCode());
        String customerNote = trim(request == null ? null : request.getCustomerNote());
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("ORDER_RETURN_REASON_REQUIRED", "Vui lòng chọn lý do hoàn hàng.", 400);
        }
        if ("OTHER".equals(reason) && !StringUtils.hasText(customerNote)) {
            throw new BusinessException("ORDER_RETURN_REASON_REQUIRED", "Vui lòng nhập ghi chú khi chọn lý do khác.", 400);
        }

        LocalDateTime now = LocalDateTime.now();
        OrderReturnRequest returnRequest = returnRequestRepository.save(OrderReturnRequest.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(OrderReturnStatus.REQUESTED)
                .reasonCode(reason)
                .customerNote(customerNote)
                .requestedBy(userId)
                .requestedAt(now)
                .idempotencyKey(normalizedKey)
                .build());
        saveAttachments(returnRequest, ReturnAttachmentOwner.CUSTOMER, ReturnAttachmentKind.CUSTOMER_PROOF, validImages);
        return toResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderReturnRequestResponse> getCustomerReturns(String userId, String orderId) {
        if (orderRepository.findByIdAndUserId(orderId, userId).isEmpty()) {
            throw new BusinessException("ORDER_NOT_FOUND", "Order not found", 404);
        }
        return returnRequestRepository.findByUserIdAndOrderIdOrderByCreatedAtDesc(userId, orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OrderReturnRequestResponse submitBankInfo(
            String userId,
            String orderId,
            String returnRequestId,
            SubmitReturnBankInfoRequest request) {
        OrderReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new BusinessException("ORDER_RETURN_NOT_FOUND", "Return request not found", 404));
        ensureCustomerOwnsReturn(userId, orderId, returnRequest);
        if (returnRequest.getStatus() != OrderReturnStatus.AWAITING_BANK_INFO) {
            throw new BusinessException("ORDER_RETURN_INVALID_STATUS", "Yêu cầu hoàn hàng chưa thể cập nhật thông tin ngân hàng.", 409);
        }
        returnRequest.setBankName(trim(request.getBankName()));
        returnRequest.setBankAccountNumber(trim(request.getBankAccountNumber()));
        returnRequest.setBankAccountHolder(trim(request.getBankAccountHolder()));
        returnRequest.setBankBranch(trim(request.getBankBranch()));
        returnRequest.setBankInfoSubmittedAt(LocalDateTime.now());
        returnRequest.setStatus(OrderReturnStatus.BANK_INFO_SUBMITTED);
        return toResponse(returnRequestRepository.save(returnRequest));
    }

    @Override
    public OrderReturnRequestResponse approveReturn(String adminUserId, String returnRequestId) {
        OrderReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new BusinessException("ORDER_RETURN_NOT_FOUND", "Return request not found", 404));
        if (returnRequest.getStatus() != OrderReturnStatus.REQUESTED) {
            throw new BusinessException("ORDER_RETURN_INVALID_STATUS", "Yêu cầu hoàn hàng đã được xử lý.", 409);
        }
        returnRequest.setStatus(OrderReturnStatus.AWAITING_BANK_INFO);
        returnRequest.setReviewedBy(adminUserId);
        returnRequest.setReviewedAt(LocalDateTime.now());
        returnRequest.setApprovedAt(LocalDateTime.now());
        return toResponse(returnRequestRepository.save(returnRequest));
    }

    @Override
    public OrderReturnRequestResponse rejectReturn(
            String adminUserId,
            String returnRequestId,
            RejectOrderReturnRequest request,
            List<MultipartFile> images) {
        OrderReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new BusinessException("ORDER_RETURN_NOT_FOUND", "Return request not found", 404));
        ensureNotFinal(returnRequest);
        List<MultipartFile> validImages = requireImages(images, "RETURN_ADMIN_IMAGE_REQUIRED");
        String rejectionReason = trim(request == null ? null : request.getRejectionReason());
        if (!StringUtils.hasText(rejectionReason)) {
            throw new BusinessException("ORDER_RETURN_REJECTION_REASON_REQUIRED", "Vui lòng nhập lý do từ chối hoàn hàng.", 400);
        }
        returnRequest.setStatus(OrderReturnStatus.REJECTED);
        returnRequest.setReviewedBy(adminUserId);
        returnRequest.setReviewedAt(LocalDateTime.now());
        returnRequest.setRejectionReason(rejectionReason);
        OrderReturnRequest saved = returnRequestRepository.save(returnRequest);
        saveAttachments(saved, ReturnAttachmentOwner.ADMIN, ReturnAttachmentKind.ADMIN_REJECTION, validImages);
        return toResponse(saved);
    }

    @Override
    public OrderReturnRequestResponse completeReturn(
            String adminUserId,
            String orderId,
            String returnRequestId,
            CompleteOrderReturnRequest request,
            List<MultipartFile> billImages) {
        OrderReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new BusinessException("ORDER_RETURN_NOT_FOUND", "Return request not found", 404));
        ensureReturnBelongsToOrder(orderId, returnRequest);
        if (returnRequest.getStatus() != OrderReturnStatus.BANK_INFO_SUBMITTED) {
            throw new BusinessException("ORDER_RETURN_INVALID_STATUS", "Yêu cầu hoàn hàng chưa có thông tin ngân hàng để hoàn tiền.", 409);
        }
        List<MultipartFile> validImages = requireImages(billImages, "RETURN_REFUND_BILL_REQUIRED");
        returnRequest.setStatus(OrderReturnStatus.REFUNDED);
        returnRequest.setProcessedBy(adminUserId);
        returnRequest.setProcessedAt(LocalDateTime.now());
        returnRequest.setRefundReference(trim(request.getRefundReference()));
        returnRequest.setRefundNote(trim(request.getRefundNote()));
        OrderReturnRequest saved = returnRequestRepository.save(returnRequest);
        saveAttachments(saved, ReturnAttachmentOwner.ADMIN, ReturnAttachmentKind.ADMIN_BILL, validImages);
        return toResponse(saved);
    }

    private void ensureCodCompletedOrder(Order order) {
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("ORDER_RETURN_NOT_ALLOWED", "Chỉ có thể yêu cầu hoàn hàng khi đơn đã giao thành công.", 409);
        }
        try {
            var paymentResponse = paymentClient.getPaymentStatus(order.getId());
            String method = paymentResponse != null && paymentResponse.getData() != null
                    ? paymentResponse.getData().getMethod()
                    : null;
            if (!"cod".equalsIgnoreCase(method)) {
                throw new BusinessException("ORDER_RETURN_COD_ONLY", "Hiện chỉ hỗ trợ hoàn hàng cho đơn COD.", 409);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Unable to verify COD payment method for return request {}: {}", order.getId(), ex.getMessage());
            throw new BusinessException("ORDER_RETURN_PAYMENT_UNAVAILABLE", "Không thể xác thực phương thức thanh toán.", 502);
        }
    }

    private void ensureCustomerOwnsReturn(String userId, String orderId, OrderReturnRequest returnRequest) {
        if (!returnRequest.getOrderId().equals(orderId) || !returnRequest.getUserId().equals(userId)) {
            throw new BusinessException("ORDER_RETURN_NOT_FOUND", "Return request not found", 404);
        }
    }

    private void ensureReturnBelongsToOrder(String orderId, OrderReturnRequest returnRequest) {
        if (!returnRequest.getOrderId().equals(orderId)) {
            throw new BusinessException("ORDER_RETURN_NOT_FOUND", "Return request not found", 404);
        }
    }

    private void ensureNotFinal(OrderReturnRequest returnRequest) {
        if (returnRequest.getStatus() == OrderReturnStatus.REFUNDED || returnRequest.getStatus() == OrderReturnStatus.REJECTED) {
            throw new BusinessException("ORDER_RETURN_INVALID_STATUS", "Yêu cầu hoàn hàng đã được xử lý.", 409);
        }
    }

    private List<MultipartFile> requireImages(List<MultipartFile> images, String errorCode) {
        List<MultipartFile> validImages = images == null ? List.of() : images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();
        if (validImages.isEmpty()) {
            throw new BusinessException(errorCode, "Vui lòng tải lên ít nhất 1 ảnh bằng chứng.", 400);
        }
        if (validImages.size() > MAX_IMAGES_PER_STEP) {
            throw new BusinessException("RETURN_IMAGE_LIMIT_EXCEEDED", "Chỉ hỗ trợ tối đa 5 ảnh cho mỗi bước xử lý.", 400);
        }
        validImages.forEach(this::validateImage);
        return validImages;
    }

    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("RETURN_IMAGE_INVALID_TYPE", "Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.", 400);
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException("RETURN_IMAGE_TOO_LARGE", "Ảnh bằng chứng không được vượt quá 3MB.", 400);
        }
    }

    private void saveAttachments(
            OrderReturnRequest returnRequest,
            ReturnAttachmentOwner owner,
            ReturnAttachmentKind kind,
            List<MultipartFile> images) {
        images.forEach(image -> {
            try {
                attachmentRepository.save(OrderReturnAttachment.builder()
                        .id(StringUtil.generateUniqueId())
                        .returnRequestId(returnRequest.getId())
                        .orderId(returnRequest.getOrderId())
                        .owner(owner)
                        .kind(kind)
                        .fileName(StringUtils.hasText(image.getOriginalFilename()) ? image.getOriginalFilename() : "return-image")
                        .contentType(image.getContentType())
                        .sizeBytes(image.getSize())
                        .imageData(image.getBytes())
                        .build());
            } catch (IOException ex) {
                throw new BusinessException("RETURN_IMAGE_READ_FAILED", "Không thể đọc ảnh bằng chứng.", 400);
            }
        });
    }

    private OrderReturnRequestResponse toResponse(OrderReturnRequest returnRequest) {
        return toResponse(returnRequest, attachmentRepository.findByReturnRequestIdOrderByCreatedAtAsc(returnRequest.getId()));
    }

    public OrderReturnRequestResponse toResponse(OrderReturnRequest returnRequest, List<OrderReturnAttachment> attachments) {
        return OrderReturnRequestResponse.builder()
                .id(returnRequest.getId())
                .orderId(returnRequest.getOrderId())
                .userId(returnRequest.getUserId())
                .status(returnRequest.getStatus().name())
                .reasonCode(returnRequest.getReasonCode())
                .customerNote(returnRequest.getCustomerNote())
                .adminNote(returnRequest.getAdminNote())
                .rejectionReason(returnRequest.getRejectionReason())
                .bankName(returnRequest.getBankName())
                .bankAccountNumber(returnRequest.getBankAccountNumber())
                .bankAccountHolder(returnRequest.getBankAccountHolder())
                .bankBranch(returnRequest.getBankBranch())
                .refundReference(returnRequest.getRefundReference())
                .refundNote(returnRequest.getRefundNote())
                .requestedBy(returnRequest.getRequestedBy())
                .reviewedBy(returnRequest.getReviewedBy())
                .processedBy(returnRequest.getProcessedBy())
                .requestedAt(toInstant(returnRequest.getRequestedAt()))
                .reviewedAt(toInstant(returnRequest.getReviewedAt()))
                .approvedAt(toInstant(returnRequest.getApprovedAt()))
                .bankInfoSubmittedAt(toInstant(returnRequest.getBankInfoSubmittedAt()))
                .processedAt(toInstant(returnRequest.getProcessedAt()))
                .createdAt(toInstant(returnRequest.getCreatedAt()))
                .updatedAt(toInstant(returnRequest.getUpdatedAt()))
                .attachments(attachments.stream().map(this::mapAttachment).toList())
                .build();
    }

    private OrderReturnAttachmentResponse mapAttachment(OrderReturnAttachment attachment) {
        String contentType = attachment.getContentType();
        String imageDataUrl = "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(attachment.getImageData());
        return OrderReturnAttachmentResponse.builder()
                .id(attachment.getId())
                .returnRequestId(attachment.getReturnRequestId())
                .orderId(attachment.getOrderId())
                .owner(attachment.getOwner().name())
                .kind(attachment.getKind().name())
                .fileName(attachment.getFileName())
                .contentType(contentType)
                .sizeBytes(attachment.getSizeBytes())
                .imageDataUrl(imageDataUrl)
                .uploadedAt(toInstant(attachment.getCreatedAt()))
                .build();
    }

    private String normalizeReason(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private java.time.Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
