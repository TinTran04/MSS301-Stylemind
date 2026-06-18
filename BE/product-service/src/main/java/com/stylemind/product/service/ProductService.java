package com.stylemind.product.service;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.product.dto.*;
import com.stylemind.product.entity.*;
import com.stylemind.product.repository.*;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final S3Client s3Client;

    @Value("${s3.bucket:stylemind-products}")
    private String bucket;

    // Product CRUD
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .id(StringUtil.generateUniqueId())
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .aestheticStyle(request.getAestheticStyle())
                .targetDemographic(request.getTargetDemographic())
                .seasonalProperty(request.getSeasonalProperty())
                .status(request.getStatus())
                .build();

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setAestheticStyle(request.getAestheticStyle());
        product.setTargetDemographic(request.getTargetDemographic());
        product.setSeasonalProperty(request.getSeasonalProperty());
        product.setStatus(request.getStatus());

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));
        
        // Delete images from S3
        List<ProductImage> images = imageRepository.findByProductId(id);
        for (ProductImage img : images) {
            try {
                String key = extractS3Key(img.getImageUrl());
                s3Client.deleteObject(b -> b.bucket(bucket).key(key));
            } catch (Exception e) {
                log.warn("Failed to delete image from S3: {}", img.getImageUrl(), e);
            }
        }
        
        productRepository.delete(product);
    }

    public ProductResponse getProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));
        return mapToResponse(product);
    }

    public PageResponse<ProductResponse> getProducts(Long categoryId, String search, BigDecimal minPrice, 
                                             BigDecimal maxPrice, String sort, Pageable pageable) {
        Page<Product> page;
        
        if (search != null && !search.isBlank()) {
            // Simplified search - in production use FullTextSearch or Qdrant
            page = productRepository.findByStatus("ACTIVE", pageable);
        } else if (categoryId != null) {
            page = productRepository.findByCategoryIdAndStatus(categoryId, "ACTIVE", pageable);
        } else {
            page = productRepository.findByStatus("ACTIVE", pageable);
        }
        
        return PageResponse.of(page.map(this::mapToResponse));
    }

    // Variants
    public ProductVariantResponse addVariant(String productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        ProductVariant variant = ProductVariant.builder()
                .id(StringUtil.generateUniqueId())
                .productId(productId)
                .sku(request.getSku())
                .size(request.getSize())
                .color(request.getColor())
                .material(request.getMaterial())
                .priceOverride(request.getPriceOverride())
                .build();

        variant = variantRepository.save(variant);
        return mapToVariantResponse(variant);
    }

    public List<ProductVariantResponse> getVariants(String productId) {
        return variantRepository.findByProductId(productId).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());
    }

    public ProductImageResponse uploadImage(String productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        String key = "products/" + productId + "/" + StringUtil.generateUniqueId() + "_" + file.getOriginalFilename();
        
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            
            s3Client.putObject(putRequest, 
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            throw new BusinessException("IMAGE_UPLOAD_FAILED", "Tải ảnh lên thất bại: " + e.getMessage(), 500);
        }

        String imageUrl = "https://" + bucket + ".s3.amazonaws.com/" + key;

        if (isPrimary) {
            imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(img -> {
                        img.setIsPrimary(false);
                        imageRepository.save(img);
                    });
        }

        ProductImage image = ProductImage.builder()
                .productId(productId)
                .imageUrl(imageUrl)
                .isPrimary(isPrimary)
                .build();

        image = imageRepository.save(image);
        return mapToImageResponse(image);
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductImageResponse> images = imageRepository.findByProductId(product.getId()).stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());
        
        List<ProductVariantResponse> variants = variantRepository.findByProductId(product.getId()).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .aestheticStyle(product.getAestheticStyle())
                .targetDemographic(product.getTargetDemographic())
                .seasonalProperty(product.getSeasonalProperty())
                .status(product.getStatus())
                .images(images)
                .variants(variants)
                .createdAt(product.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(product.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private ProductVariantResponse mapToVariantResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProductId())
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .material(variant.getMaterial())
                .priceOverride(variant.getPriceOverride())
                .build();
    }

    private ProductImageResponse mapToImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    private String extractS3Key(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}