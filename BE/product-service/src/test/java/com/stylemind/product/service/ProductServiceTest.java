package com.stylemind.product.service;

import com.stylemind.product.dto.*;
import com.stylemind.product.entity.*;
import com.stylemind.product.repository.*;
import com.stylemind.product.service.image.ProductImageStorage;
import com.stylemind.product.service.image.StoredProductImage;
import com.stylemind.common.dto.PageResponse;
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
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private ProductAuditLogRepository auditLogRepository;
    @Mock
    private ProductImageStorage imageStorage;

    @InjectMocks
    private ProductService productService;

    @org.junit.jupiter.api.BeforeEach
    void setDefaultCurrency() {
        org.springframework.test.util.ReflectionTestUtils.setField(productService, "defaultCurrency", "VND");
    }

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
        activeProduct.setCategoryId(10L);
        when(productRepository.findByIdAndStatus("p1", "ACTIVE")).thenReturn(Optional.of(activeProduct));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(
                Category.builder().id(10L).name("Áo sơ mi").slug("ao-so-mi").build()));
        when(imageRepository.findByProductId("p1")).thenReturn(List.of());
        when(variantRepository.findByProductId("p1")).thenReturn(List.of());

        ProductResponse response = productService.getProduct("p1");

        assertNotNull(response);
        assertEquals("p1", response.getId());
        assertEquals("Áo sơ mi", response.getCategoryName());
    }

    @Test
    void getProducts_batchLoadsCategoryImagesAndVariants() {
        activeProduct.setCategoryId(10L);
        PageRequest pageable = PageRequest.of(0, 20);
        when(productRepository.searchAndFilter(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(activeProduct), pageable, 1));
        when(categoryRepository.findAllById(List.of(10L))).thenReturn(List.of(
                Category.builder().id(10L).name("Áo sơ mi").slug("ao-so-mi").build()));
        when(imageRepository.findByProductIdIn(List.of("p1"))).thenReturn(List.of());
        when(variantRepository.findByProductIdIn(List.of("p1"))).thenReturn(List.of(variant));

        PageResponse<ProductResponse> response = productService.getProducts(
                null, null, null, null, "createdAt,desc", pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("Áo sơ mi", response.getContent().get(0).getCategoryName());
        assertEquals(1, response.getContent().get(0).getVariants().size());
        verify(imageRepository, never()).findByProductId("p1");
        verify(variantRepository, never()).findByProductId("p1");
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
    void getVariants_inactiveProduct_throws404() {
        when(productRepository.findByIdAndStatus("p2", "ACTIVE")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> productService.getVariants("p2"));
    }

    @Test
    void getVariants_activeProduct_returnsVariants() {
        when(productRepository.findByIdAndStatus("p1", "ACTIVE")).thenReturn(Optional.of(activeProduct));
        when(variantRepository.findByProductId("p1")).thenReturn(List.of(variant));

        List<ProductVariantResponse> variants = productService.getVariants("p1");

        assertEquals(1, variants.size());
        assertEquals("SKU-1", variants.get(0).getSku());
    }

    @Test
    void getVariantSnapshot_includesCurrency() {
        when(variantRepository.findById("v1")).thenReturn(Optional.of(variant));
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
        when(imageRepository.findByProductIdAndIsPrimaryTrue("p1")).thenReturn(Optional.empty());

        VariantSnapshotResponse response = productService.getVariantSnapshot("v1");

        assertEquals("VND", response.getCurrency());
    }

    @Test
    void deleteProduct_setsInactiveStatus() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));

        productService.deleteProduct("p1", "admin-1");

        assertEquals("INACTIVE", activeProduct.getStatus());
        verify(productRepository).save(activeProduct);
    }

    @Test
    void deleteProduct_recordsAuditLogWithActor() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));

        productService.deleteProduct("p1", "admin-1");

        org.mockito.ArgumentCaptor<ProductAuditLog> captor = org.mockito.ArgumentCaptor.forClass(ProductAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("admin-1", captor.getValue().getActorId());
        assertEquals("DELETE_PRODUCT", captor.getValue().getAction());
        assertEquals("p1", captor.getValue().getProductId());
    }

    @Test
    void deleteVariant_recordsAuditLogWithActor() {
        when(variantRepository.findById("v1")).thenReturn(Optional.of(variant));

        productService.deleteVariant("p1", "v1", "admin-1");

        org.mockito.ArgumentCaptor<ProductAuditLog> captor = org.mockito.ArgumentCaptor.forClass(ProductAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("admin-1", captor.getValue().getActorId());
        assertEquals("DELETE_VARIANT", captor.getValue().getAction());
    }

    @Test
    void uploadImage_persistsSecureUrlAndPublicId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shirt.png", "image/png", new byte[]{1, 2, 3});
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
        when(imageStorage.upload("p1", file))
                .thenReturn(new StoredProductImage(
                        "https://res.cloudinary.com/stylemind/image/upload/shirt.png",
                        "stylemind/products/shirt"));
        when(imageRepository.save(any(ProductImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductImageResponse response = productService.uploadImage("p1", file, true);

        assertEquals("https://res.cloudinary.com/stylemind/image/upload/shirt.png", response.getImageUrl());
        assertEquals("stylemind/products/shirt", response.getPublicId());
        verify(imageRepository).save(argThat(image ->
                "stylemind/products/shirt".equals(image.getImagePublicId())));
    }

    @Test
    void uploadImage_rejectsNonImageContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", new byte[]{1, 2, 3});
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.uploadImage("p1", file, false));

        assertEquals("INVALID_IMAGE_TYPE", exception.getErrorCode());
        verifyNoInteractions(imageStorage);
    }

    @Test
    void uploadImage_hidesStorageProviderFailureDetails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shirt.png", "image/png", new byte[]{1, 2, 3});
        when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
        when(imageStorage.upload("p1", file))
                .thenThrow(new IllegalStateException("api_secret=do-not-leak"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.uploadImage("p1", file, false));

        assertEquals("IMAGE_UPLOAD_FAILED", exception.getErrorCode());
        assertFalse(exception.getMessage().contains("do-not-leak"));
    }
}
