package com.stylemind.product.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.product.entity.ProductVariant;
import com.stylemind.product.entity.RestockLog;
import com.stylemind.product.repository.ProductVariantRepository;
import com.stylemind.product.repository.RestockLogRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InternalInventoryController {

    private final ProductVariantRepository productVariantRepository;
    private final RestockLogRepository restockLogRepository;

    @PostMapping("/return-restocks")
    @Transactional
    public ApiResponse<Void> restockInventory(@RequestBody ReturnRestockRequest request) {
        log.info("Processing return restock request for operationKey: {}, variantId: {}, quantity: {}",
                request.getOperationKey(), request.getVariantId(), request.getQuantity());

        if (request.getOperationKey() != null) {
            Optional<RestockLog> existing = restockLogRepository.findByOperationKey(request.getOperationKey());
            if (existing.isPresent()) {
                log.info("Restock operationKey {} already processed. Idempotent skip.", request.getOperationKey());
                return ApiResponse.success(null);
            }
        }

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new BusinessException("VARIANT_NOT_FOUND", "Biến thể sản phẩm không tồn tại", 404));

        int currentStock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        int restockQty = request.getQuantity() != null ? request.getQuantity() : 0;
        variant.setStockQuantity(currentStock + restockQty);
        productVariantRepository.save(variant);

        RestockLog restockLog = RestockLog.builder()
                .id("rst_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .operationKey(request.getOperationKey())
                .variantId(request.getVariantId())
                .quantity(restockQty)
                .reason(request.getReason())
                .referenceId(request.getReferenceId())
                .createdAt(LocalDateTime.now())
                .build();
        restockLogRepository.save(restockLog);

        log.info("Restocked {} units for variant {}. New stock: {}", restockQty, variant.getId(), variant.getStockQuantity());
        return ApiResponse.success(null);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnRestockRequest {
        private String operationKey;
        private String variantId;
        private Integer quantity;
        private String reason;
        private String referenceId;
    }
}
