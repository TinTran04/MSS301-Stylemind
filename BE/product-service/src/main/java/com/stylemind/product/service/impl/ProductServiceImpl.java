package com.stylemind.product.service.impl;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.product.dto.*;
import com.stylemind.product.entity.*;
import com.stylemind.product.repository.*;
import com.stylemind.product.service.ProductService;
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
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
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

    private void ensureProductCanBeActive(String productId, String status) {
        if ("ACTIVE".equals(status) && !variantRepository.existsByProductId(productId)) {
            throw new BusinessException(
                    "PRODUCT_REQUIRES_VARIANT",
                    "Cannot activate a product without variants. Add at least one variant before publishing it.",
                    409);
        }
    }

    private TargetDemographic resolveTargetDemographic(String value) {
        if (value == null || value.isBlank()) {
            return TargetDemographic.UNISEX;
        }
        try {
            return TargetDemographic.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_TARGET_DEMOGRAPHIC",
                    "Đối tượng mục tiêu phải là MALE, FEMALE hoặc UNISEX", 400);
        }
    }

    private List<Long> distinctCategoryIds(List<Long> categoryIds) {
        return categoryIds == null ? List.of() : categoryIds.stream().distinct().toList();
    }

    private void validateCategoryIds(List<Long> categoryIds) {
        for (Long categoryId : categoryIds) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new BusinessException("CATEGORY_NOT_FOUND", "Danh mục không tồn tại", 400);
            }
        }
    }

    private void replaceProductCategories(String productId, List<Long> categoryIds) {
        productCategoryRepository.deleteByProductId(productId);
        if (!categoryIds.isEmpty()) {
            productCategoryRepository.saveAll(categoryIds.stream()
                    .map(categoryId -> ProductCategory.builder().productId(productId).categoryId(categoryId).build())
                    .toList());
        }
    }

    // Product CRUD
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        List<Long> categoryIds = distinctCategoryIds(request.getCategoryIds());
        validateCategoryIds(categoryIds);

        Product product = Product.builder()
                .id(StringUtil.generateUniqueId())
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .targetDemographic(resolveTargetDemographic(request.getTargetDemographic()))
                .status("INACTIVE")
                .build();

        product = productRepository.save(product);
        replaceProductCategories(product.getId(), categoryIds);
        return mapToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        List<Long> categoryIds = distinctCategoryIds(request.getCategoryIds());
        validateCategoryIds(categoryIds);

        ensureProductCanBeActive(id, request.getStatus());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setTargetDemographic(resolveTargetDemographic(request.getTargetDemographic()));
        product.setStatus(request.getStatus());

        product = productRepository.save(product);
        replaceProductCategories(id, categoryIds);
        return mapToResponse(product);
    }

    @Override
    public void deleteProduct(String id, String actorId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        product.setStatus("INACTIVE");
        productRepository.save(product);
        recordAudit(actorId, "DELETE_PRODUCT", id, null);
    }

    @Override
    public ProductResponse getProduct(String id) {
        Product product = productRepository.findSellableById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm hoặc sản phẩm không hoạt động", 404));
        return mapToResponse(product);
    }

    @Override
    public PageResponse<ProductResponse> getProducts(Long categoryId, String categorySlug, String search, String targetDemographic,
                                             BigDecimal minPrice, BigDecimal maxPrice, String sort, Pageable pageable) {
        String keyword = (search != null && !search.isBlank()) ? search : null;
        Long resolvedCategoryId = resolveCategoryId(categoryId, categorySlug);
        Page<Product> page = productRepository.searchAndFilter(
                keyword,
                resolvedCategoryId,
                parseDemographicFilter(targetDemographic),
                minPrice,
                maxPrice,
                pageable);
        return mapPage(page);
    }

    private Long resolveCategoryId(Long categoryId, String categorySlug) {
        if (categoryId != null) {
            return categoryId;
        }
        if (categorySlug == null || categorySlug.isBlank()) {
            return null;
        }
        return categoryRepository.findBySlug(categorySlug.trim().toLowerCase())
                .map(Category::getId)
                .orElse(null);
    }

    // A GET filter param, unlike a create/update payload: an unrecognized value
    // is treated as "no filter" rather than a 400, so a stale/typo'd query
    // string just returns the unfiltered list instead of erroring the page.
    private TargetDemographic parseDemographicFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TargetDemographic.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ProductResponse> getProductsAdmin(Long categoryId, String search, String status, Pageable pageable) {
        String keyword = (search != null && !search.isBlank()) ? search : null;
        Page<Product> page = productRepository.searchAndFilterAdmin(keyword, categoryId, status, pageable);
        return mapPage(page);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductResponse getProductAdmin(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));
        return mapToResponse(product);
    }

    /** Real catalogue counts for the admin dashboard. */
    @Transactional(readOnly = true)
    @Override
    public AdminProductSummaryResponse getAdminSummary() {
        long total = productRepository.count();
        long active = productRepository.countByStatus("ACTIVE");
        return AdminProductSummaryResponse.builder()
                .totalProducts(total)
                .activeProducts(active)
                .inactiveProducts(total - active)
                .build();
    }

    @Override
    public ProductResponse updateProductStatus(String id, String status) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));
        ensureProductCanBeActive(id, status);
        product.setStatus(status);
        product = productRepository.save(product);
        return mapToResponse(product);
    }

    // Variants
    private static String normalizeCombo(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void ensureNoDuplicateCombo(String productId, String excludeVariantId, String size, String color, String material) {
        boolean duplicate = variantRepository.findByProductId(productId).stream()
                .filter(v -> excludeVariantId == null || !v.getId().equals(excludeVariantId))
                .anyMatch(v -> normalizeCombo(v.getSize()).equals(normalizeCombo(size))
                        && normalizeCombo(v.getColor()).equals(normalizeCombo(color))
                        && normalizeCombo(v.getMaterial()).equals(normalizeCombo(material)));
        if (duplicate) {
            throw new BusinessException("DUPLICATE_VARIANT",
                    "Biến thể này đã tồn tại. Vui lòng kiểm tra lại kích cỡ, màu sắc và chất liệu.", 409);
        }
    }

    @Override
    public ProductVariantResponse addVariant(String productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        if (variantRepository.findBySku(request.getSku()).isPresent()) {
            throw new BusinessException("SKU_EXISTS", "SKU đã tồn tại: " + request.getSku(), 400);
        }
        ensureNoDuplicateCombo(productId, null, request.getSize(), request.getColor(), request.getMaterial());

        ProductVariant variant = ProductVariant.builder()
                .id(StringUtil.generateUniqueId())
                .productId(productId)
                .sku(request.getSku())
                .size(request.getSize())
                .color(request.getColor())
                .material(request.getMaterial())
                .priceOverride(request.getPriceOverride())
                .stockQuantity(request.getStockQuantity())
                .active(request.getActive() == null || request.getActive())
                .build();

        variant = variantRepository.save(variant);
        return mapToVariantResponse(variant);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductVariantResponse> getVariants(String productId) {
        // Public callers must never see variants belonging to an INACTIVE/DISCONTINUED product.
        productRepository.findSellableById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm hoặc sản phẩm không hoạt động", 404));
        return variantRepository.findByProductId(productId).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductImageResponse uploadImage(String productId, MultipartFile file, boolean isPrimary) {
        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        validateImage(file);

        if (!imageStorage.isConfigured()) {
            log.warn("Product image upload blocked for product {}: image storage provider has no credentials configured", productId);
            throw new BusinessException("IMAGE_STORAGE_NOT_CONFIGURED",
                    "Chưa cấu hình dịch vụ lưu ảnh. Vui lòng kiểm tra Cloudinary.", 503);
        }

        StoredProductImage storedImage;
        try {
            storedImage = imageStorage.upload(productId, file);
        } catch (Exception e) {
            log.warn("Product image upload failed for product {} file={} contentType={} size={} cause={}: {}",
                    productId, file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    e.getClass().getSimpleName(), e.getMessage());
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

    @Override
    public ProductVariantResponse updateVariant(String productId, String variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể", 404));

        if (!variant.getProductId().equals(productId)) {
            throw new BusinessException("VARIANT_MISMATCH", "Biến thể không thuộc sản phẩm này", 400);
        }

        if (!variant.getSku().equals(request.getSku()) && variantRepository.findBySku(request.getSku()).isPresent()) {
            throw new BusinessException("SKU_EXISTS", "SKU đã tồn tại: " + request.getSku(), 400);
        }
        ensureNoDuplicateCombo(productId, variantId, request.getSize(), request.getColor(), request.getMaterial());

        variant.setSku(request.getSku());
        variant.setSize(request.getSize());
        variant.setColor(request.getColor());
        variant.setMaterial(request.getMaterial());
        variant.setPriceOverride(request.getPriceOverride());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setActive(request.getActive() == null || request.getActive());

        variant = variantRepository.save(variant);
        return mapToVariantResponse(variant);
    }

    @Override
    public void deleteVariant(String productId, String variantId, String actorId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể", 404));
        if (!variant.getProductId().equals(productId)) {
            throw new BusinessException("VARIANT_MISMATCH", "Biến thể không thuộc sản phẩm này", 400);
        }
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));
        if ("ACTIVE".equals(product.getStatus()) && variantRepository.countByProductId(productId) <= 1) {
            throw new BusinessException(
                    "LAST_ACTIVE_VARIANT",
                    "Cannot delete the last variant of an active product. Deactivate the product before deleting its final variant.",
                    409);
        }
        variantRepository.delete(variant);
        recordAudit(actorId, "DELETE_VARIANT", productId, "variantId=" + variantId);
    }

    @Override
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
    @Override
    public VariantSnapshotResponse getVariantSnapshot(String variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .or(() -> variantRepository.findBySku(variantId))
                .orElseThrow(() -> new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể", 404));
        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", 404));

        BigDecimal effectivePrice = variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : product.getBasePrice();

        List<ProductImage> productImages = imageRepository.findByProductId(product.getId());
        String primaryImageUrl = productImages.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> productImages.stream()
                        .map(ProductImage::getImageUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .findFirst()
                        .orElse(null));

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
                .stockQuantity(variant.getStockQuantity())
                .active(variant.getActive())
                .build();
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductImageResponse> images = imageRepository.findByProductId(product.getId()).stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());

        List<ProductVariantResponse> variants = variantRepository.findByProductId(product.getId()).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());
        List<Long> categoryIds = productCategoryRepository.findByProductId(product.getId()).stream()
                .map(ProductCategory::getCategoryId)
                .toList();
        List<CategorySummaryResponse> categories = categoryRepository.findAllById(categoryIds).stream()
                .map(this::mapToCategorySummary)
                .toList();

        return mapToResponse(product, categories, images, variants);
    }

    private PageResponse<ProductResponse> mapPage(Page<Product> page) {
        List<Product> products = page.getContent();
        if (products.isEmpty()) {
            return PageResponse.of(page.map(product -> mapToResponse(product, List.of(), List.of(), List.of())));
        }

        List<String> productIds = products.stream().map(Product::getId).toList();
        List<ProductCategory> productCategories = productCategoryRepository.findByProductIdIn(productIds);
        List<Long> categoryIds = productCategories.stream()
                .map(ProductCategory::getCategoryId)
                .distinct()
                .toList();
        Map<Long, CategorySummaryResponse> categorySummaries = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, this::mapToCategorySummary));
        Map<String, List<CategorySummaryResponse>> categoriesByProduct = productCategories.stream()
                .collect(Collectors.groupingBy(
                        ProductCategory::getProductId,
                        Collectors.mapping(pc -> categorySummaries.get(pc.getCategoryId()), Collectors.toList())));
        Map<String, List<ProductImageResponse>> imagesByProduct = imageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        ProductImage::getProductId,
                        Collectors.mapping(this::mapToImageResponse, Collectors.toList())));
        Map<String, List<ProductVariantResponse>> variantsByProduct = variantRepository.findByProductIdIn(productIds).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.groupingBy(ProductVariantResponse::getProductId));

        return PageResponse.of(page.map(product -> mapToResponse(
                product,
                categoriesByProduct.getOrDefault(product.getId(), List.of()),
                imagesByProduct.getOrDefault(product.getId(), List.of()),
                variantsByProduct.getOrDefault(product.getId(), List.of()))));
    }

    private CategorySummaryResponse mapToCategorySummary(Category category) {
        return CategorySummaryResponse.builder().id(category.getId()).name(category.getName()).build();
    }

    private ProductResponse mapToResponse(
            Product product,
            List<CategorySummaryResponse> categories,
            List<ProductImageResponse> images,
            List<ProductVariantResponse> variants) {
        return ProductResponse.builder()
                .id(product.getId())
                .categories(categories)
                .name(product.getName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .targetDemographic(product.getTargetDemographic().name())
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
                .stockQuantity(variant.getStockQuantity())
                .active(variant.getActive())
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
