package com.stylemind.product.controller;

import com.stylemind.product.dto.VariantSnapshotResponse;
import com.stylemind.product.service.ProductService;
import com.stylemind.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<VariantSnapshotResponse>> getVariantSnapshot(@PathVariable String variantId) {
        return ResponseEntity.ok(ApiResponse.success("OK", productService.getVariantSnapshot(variantId)));
    }
}
