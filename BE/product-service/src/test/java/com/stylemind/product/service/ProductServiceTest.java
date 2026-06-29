package com.stylemind.product.service;

import com.stylemind.product.dto.*;
import com.stylemind.product.entity.*;
import com.stylemind.product.repository.*;
import com.stylemind.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import software.amazon.awssdk.services.s3.S3Client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ProductImageRepository imageRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private S3Client s3Client;

    @InjectMocks
    private ProductService productService;

    private Product activeProduct;
    private Product inactiveProduct;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        activeProduct = Product.builder()
                .id("p1")
                .name("Active Product")
                .basePrice(new BigDecimal("100.00"))
                .status("ACTIVE")
                .build();
        activeProduct.setCreatedAt(java.time.LocalDateTime.now());
        activeProduct.setUpdatedAt(java.time.LocalDateTime.now());
                
        inactiveProduct = Product.builder()
                .id("p2")
                .name("Inactive Product")
                .basePrice(new BigDecimal("100.00"))
                .status("INACTIVE")
                .build();
        inactiveProduct.setCreatedAt(java.time.LocalDateTime.now());
        inactiveProduct.setUpdatedAt(java.time.LocalDateTime.now());

        variant = ProductVariant.builder()
                .id("v1")
                .productId("p1")
                .sku("SKU-1")
                .priceOverride(null)
                .build();
    }

    @Test
    void getProduct_inactive_throws404() {
        when(productRepository.findByIdAndStatus("p2", "ACTIVE")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> productService.getProduct("p2"));
    }

    @Test
    void getProduct_active_returnsProduct() {
        when(productRepository.findByIdAndStatus("p1", "ACTIVE")).thenReturn(Optional.of(activeProduct));
        when(imageRepository.findByProductId("p1")).thenReturn(List.of());
        when(variantRepository.findByProductId("p1")).thenReturn(List.of());

        ProductResponse response = productService.getProduct("p1");

        assertNotNull(response);
        assertEquals("p1", response.getId());
    }

    @Test
    void addVariant_duplicateSku_throws400() {
        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("SKU-1");

        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
        when(variantRepository.findBySku("SKU-1")).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.addVariant("p1", request));
        assertEquals("SKU_EXISTS", ex.getErrorCode());
    }

    @Test
    void getVariantSnapshot_priceOverrideNull_usesBasePrice() {
        when(variantRepository.findById("v1")).thenReturn(Optional.of(variant));
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
        when(imageRepository.findByProductIdAndIsPrimaryTrue("p1")).thenReturn(Optional.empty());

        VariantSnapshotResponse response = productService.getVariantSnapshot("v1");

        assertEquals(new BigDecimal("100.00"), response.getEffectivePrice());
    }

    @Test
    void getVariantSnapshot_priceOverrideSet_usesPriceOverride() {
        variant.setPriceOverride(new BigDecimal("120.00"));
        when(variantRepository.findById("v1")).thenReturn(Optional.of(variant));
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
        when(imageRepository.findByProductIdAndIsPrimaryTrue("p1")).thenReturn(Optional.empty());

        VariantSnapshotResponse response = productService.getVariantSnapshot("v1");

        assertEquals(new BigDecimal("120.00"), response.getEffectivePrice());
    }

    @Test
    void deleteProduct_setsInactiveStatus() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
        
        productService.deleteProduct("p1");

        assertEquals("INACTIVE", activeProduct.getStatus());
        verify(productRepository).save(activeProduct);
    }
}
