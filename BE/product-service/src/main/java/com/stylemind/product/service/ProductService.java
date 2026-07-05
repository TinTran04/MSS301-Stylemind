package com.stylemind.product.service;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.product.dto.*;
import com.stylemind.product.entity.*;
import com.stylemind.product.repository.*;
import com.stylemind.product.service.image.ProductImageStorage;
import com.stylemind.product.service.image.StoredProductImage;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ProductAuditLogRepository auditLogRepository;
    private final ProductImageStorage imageStorage;

    @Value("${app.product.default-currency:VND}")
    private String defaultCurrency;

    private void recordAudit(String actorId, String action, String productId, String detail) {
        auditLogRepository.save(ProductAuditLog.builder()
                .id(StringUtil.generateUniqueId())
                .actorId(actorId)
                .action(action)
                .productId(productId)
                .detail(detail)
                .build());
        log.info("Product admin action | actor={} action={} productId={} detail={}", actorId, action, productId, detail);
    }

    // Product CRUD
    public ProductResponse createProduct(ProductRequest request) {
        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "Danh mục không tồn tại", 400);
        }
        
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

        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "Danh mục không tồn tại", 400);
        }

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

    public void deleteProduct(String id, String actorId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        product.setStatus("INACTIVE");
        productRepository.save(product);
        recordAudit(actorId, "DELETE_PRODUCT", id, null);
    }

    public ProductResponse getProduct(String id) {
        Product product = productRepository.findByIdAndStatus(id, "ACTIVE")
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm hoặc sản phẩm không hoạt động", 404));
        return mapToResponse(product);
    }

    public PageResponse<ProductResponse> getProducts(Long categoryId, String search, BigDecimal minPrice, 
                                             BigDecimal maxPrice, String sort, Pageable pageable) {
        String keyword = (search != null && !search.isBlank()) ? search : null;
        Page<Product> page = productRepository.searchAndFilter(keyword, categoryId, minPrice, maxPrice, pageable);
        return mapPage(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsAdmin(Long categoryId, String search, String status, Pageable pageable) {
        String keyword = (search != null && !search.isBlank()) ? search : null;
        Page<Product> page = productRepository.searchAndFilterAdmin(keyword, categoryId, status, pageable);
        return mapPage(page);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductAdmin(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));
        return mapToResponse(product);
    }

    /** Real catalogue counts for the admin dashboard. */
    @Transactional(readOnly = true)
    public AdminProductSummaryResponse getAdminSummary() {
        long total = productRepository.count();
        long active = productRepository.countByStatus("ACTIVE");
        return AdminProductSummaryResponse.builder()
                .totalProducts(total)
                .activeProducts(active)
                .inactiveProducts(total - active)
                .build();
    }

    public ProductResponse updateProductStatus(String id, String status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));
        product.setStatus(status);
        product = productRepository.save(product);
        return mapToResponse(product);
    }

    // Variants
    public ProductVariantResponse addVariant(String productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        if (variantRepository.findBySku(request.getSku()).isPresent()) {
            throw new BusinessException("SKU_EXISTS", "SKU đã tồn tại: " + request.getSku(), 400);
        }

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

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(String productId) {
        // Public callers must never see variants belonging to an INACTIVE/DISCONTINUED product.
        productRepository.findByIdAndStatus(productId, "ACTIVE")
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm hoặc sản phẩm không hoạt động", 404));
        return variantRepository.findByProductId(productId).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());
    }

    public ProductImageResponse uploadImage(String productId, MultipartFile file, boolean isPrimary) {
        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        validateImage(file);

        StoredProductImage storedImage;
        try {
            storedImage = imageStorage.upload(productId, file);
        } catch (Exception e) {
            log.warn("Product image upload failed for product {}", productId);
            throw new BusinessException("IMAGE_UPLOAD_FAILED", "Tải ảnh lên thất bại", 500);
        }

        if (isPrimary) {
            imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(img -> {
                        img.setIsPrimary(false);
                        imageRepository.save(img);
                    });
        }

        ProductImage image = ProductImage.builder()
                .productId(productId)
                .imageUrl(storedImage.imageUrl())
                .imagePublicId(storedImage.publicId())
                .isPrimary(isPrimary)
                .build();

        image = imageRepository.save(image);
        return mapToImageResponse(image);
    }

    public ProductVariantResponse updateVariant(String productId, String variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể", 404));
        
        if (!variant.getProductId().equals(productId)) {
            throw new BusinessException("VARIANT_MISMATCH", "Biến thể không thuộc sản phẩm này", 400);
        }

        if (!variant.getSku().equals(request.getSku()) && variantRepository.findBySku(request.getSku()).isPresent()) {
            throw new BusinessException("SKU_EXISTS", "SKU đã tồn tại: " + request.getSku(), 400);
        }

        variant.setSku(request.getSku());
        variant.setSize(request.getSize());
        variant.setColor(request.getColor());
        variant.setMaterial(request.getMaterial());
        variant.setPriceOverride(request.getPriceOverride());

        variant = variantRepository.save(variant);
        return mapToVariantResponse(variant);
    }

    public void deleteVariant(String productId, String variantId, String actorId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể", 404));
        if (!variant.getProductId().equals(productId)) {
            throw new BusinessException("VARIANT_MISMATCH", "Biến thể không thuộc sản phẩm này", 400);
        }
        variantRepository.delete(variant);
        recordAudit(actorId, "DELETE_VARIANT", productId, "variantId=" + variantId);
    }

    public void deleteImage(String productId, Long imageId, String actorId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("IMAGE_NOT_FOUND", "Không tìm thấy ảnh", 404));
        if (!image.getProductId().equals(productId)) {
            throw new BusinessException("IMAGE_MISMATCH", "Ảnh không thuộc sản phẩm này", 400);
        }

        if (image.getImagePublicId() != null && !image.getImagePublicId().isBlank()) {
            try {
                imageStorage.delete(image.getImagePublicId());
            } catch (Exception e) {
                log.warn("Cloud image deletion failed for image {}", imageId);
            }
        }

        imageRepository.delete(image);
        recordAudit(actorId, "DELETE_IMAGE", productId, "imageId=" + imageId);
    }

    @Transactional(readOnly = true)
    public VariantSnapshotResponse getVariantSnapshot(String variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể", 404));
        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        BigDecimal effectivePrice = variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : product.getBasePrice();

        String primaryImageUrl = imageRepository.findByProductIdAndIsPrimaryTrue(product.getId())
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return VariantSnapshotResponse.builder()
                .variantId(variant.getId())
                .productId(product.getId())
                .productName(product.getName())
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .material(variant.getMaterial())
                .effectivePrice(effectivePrice)
                .currency(defaultCurrency)
                .status(product.getStatus())
                .primaryImageUrl(primaryImageUrl)
                .build();
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductImageResponse> images = imageRepository.findByProductId(product.getId()).stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());
        
        List<ProductVariantResponse> variants = variantRepository.findByProductId(product.getId()).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());
        String categoryName = product.getCategoryId() == null
                ? null
                : categoryRepository.findById(product.getCategoryId()).map(Category::getName).orElse(null);

        return mapToResponse(product, categoryName, images, variants);
    }

    private PageResponse<ProductResponse> mapPage(Page<Product> page) {
        List<Product> products = page.getContent();
        if (products.isEmpty()) {
            return PageResponse.of(page.map(product -> mapToResponse(product, null, List.of(), List.of())));
        }

        List<String> productIds = products.stream().map(Product::getId).toList();
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<String, List<ProductImageResponse>> imagesByProduct = imageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        ProductImage::getProductId,
                        Collectors.mapping(this::mapToImageResponse, Collectors.toList())));
        Map<String, List<ProductVariantResponse>> variantsByProduct = variantRepository.findByProductIdIn(productIds).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.groupingBy(ProductVariantResponse::getProductId));

        return PageResponse.of(page.map(product -> mapToResponse(
                product,
                categoryNames.get(product.getCategoryId()),
                imagesByProduct.getOrDefault(product.getId(), List.of()),
                variantsByProduct.getOrDefault(product.getId(), List.of()))));
    }

    private ProductResponse mapToResponse(
            Product product,
            String categoryName,
            List<ProductImageResponse> images,
            List<ProductVariantResponse> variants) {
        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
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
                .publicId(image.getImagePublicId())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_IMAGE", "Vui lòng chọn tệp ảnh", 400);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("IMAGE_TOO_LARGE", "Ảnh không được vượt quá 10 MB", 400);
        }
        Set<String> allowedTypes = Set.of(
                "image/jpeg", "image/png", "image/webp", "image/gif", "image/avif");
        if (file.getContentType() == null || !allowedTypes.contains(file.getContentType().toLowerCase())) {
            throw new BusinessException("INVALID_IMAGE_TYPE", "Định dạng ảnh không được hỗ trợ", 400);
        }
    }
}
