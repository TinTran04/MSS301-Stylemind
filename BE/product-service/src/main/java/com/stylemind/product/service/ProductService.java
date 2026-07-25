package com.stylemind.product.service;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.product.dto.AdminProductSummaryResponse;
import com.stylemind.product.dto.ProductImageResponse;
import com.stylemind.product.dto.ProductRequest;
import com.stylemind.product.dto.ProductResponse;
import com.stylemind.product.dto.ProductVariantRequest;
import com.stylemind.product.dto.ProductVariantResponse;
import com.stylemind.product.dto.VariantSnapshotResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(String id, ProductRequest request);

    void deleteProduct(String id, String actorId);

    ProductResponse getProduct(String id);

    PageResponse<ProductResponse> getProducts(Long categoryId, String categorySlug, String search, String targetDemographic,
                                              BigDecimal minPrice, BigDecimal maxPrice, String sort, Pageable pageable);

    PageResponse<ProductResponse> getProductsAdmin(Long categoryId, String search, String status, Pageable pageable);

    ProductResponse getProductAdmin(String id);

    AdminProductSummaryResponse getAdminSummary();

    ProductResponse updateProductStatus(String id, String status);

    ProductVariantResponse addVariant(String productId, ProductVariantRequest request);

    List<ProductVariantResponse> getVariants(String productId);

    ProductImageResponse uploadImage(String productId, MultipartFile file, boolean isPrimary);

    ProductVariantResponse updateVariant(String productId, String variantId, ProductVariantRequest request);

    void deleteVariant(String productId, String variantId, String actorId);

    void deleteImage(String productId, Long imageId, String actorId);

    VariantSnapshotResponse getVariantSnapshot(String variantId);
}
