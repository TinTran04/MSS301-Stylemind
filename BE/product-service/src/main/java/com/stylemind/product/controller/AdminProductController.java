package com.stylemind.product.controller;

import com.stylemind.product.dto.*;
import com.stylemind.product.service.ProductService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<ProductResponse> response = productService.getProductsAdmin(category, search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminProductSummaryResponse>> getSummary() {
        AdminProductSummaryResponse summary = productService.getAdminSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê sản phẩm thành công", summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable String id) {
        ProductResponse product = productService.getProductAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo sản phẩm thành công", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id, 
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", product));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStatus(
            @PathVariable String id, 
            @Valid @RequestBody StatusUpdateRequest request) {
        ProductResponse product = productService.updateProductStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        productService.deleteProduct(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariant(
            @PathVariable String productId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse variant = productService.addVariant(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Thêm biến thể thành công", variant));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable String productId,
            @PathVariable String variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse variant = productService.updateVariant(productId, variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật biến thể thành công", variant));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable String productId,
            @PathVariable String variantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        productService.deleteVariant(productId, variantId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Xóa biến thể thành công", null));
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadImage(
            @PathVariable String productId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        ProductImageResponse image = productService.uploadImage(productId, file, isPrimary);
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công", image));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable String productId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        productService.deleteImage(productId, imageId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Xóa ảnh thành công", null));
    }
}
