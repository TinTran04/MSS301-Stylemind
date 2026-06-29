package com.stylemind.product.controller;

import com.stylemind.product.dto.*;
import com.stylemind.product.service.ProductService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stylemind.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        
        PageResponse<ProductResponse> response = productService.getProducts(category, search, minPrice, maxPrice, sort, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable String id) {
        ProductResponse product = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", product));
    }

    @GetMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<java.util.List<ProductVariantResponse>>> getVariants(@PathVariable String productId) {
        java.util.List<ProductVariantResponse> variants = productService.getVariants(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách biến thể thành công", variants));
    }
}