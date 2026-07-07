package com.stylemind.cart.service;

import com.stylemind.cart.dto.*;
import com.stylemind.cart.entity.CartItem;
import com.stylemind.cart.entity.ShoppingCart;
import com.stylemind.cart.feign.ProductClient;
import com.stylemind.cart.repository.CartItemRepository;
import com.stylemind.cart.repository.ShoppingCartRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    private String getCartId(String userId, String guestSessionId) {
        if (userId != null) {
            return userId;
        }
        return "guest_" + guestSessionId;
    }

    public CartResponse getCart(String userId, String guestSessionId) {
        String cartId = getCartId(userId, guestSessionId);
        ShoppingCart cart = cartRepository.findById(cartId).orElse(null);

        if (cart == null) {
            return CartResponse.builder()
                    .cartId(cartId)
                    .items(List.of())
                    .totalAmount(BigDecimal.ZERO)
                    .totalQuantity(0)
                    .build();
        }

        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        return buildCartResponse(cart, items);
    }

    public CartResponse addItem(String userId, String guestSessionId, CartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "Số lượng phải lớn hơn 0", 400);
        }

        validateVariant(request.getVariantId());

        String cartId = getCartId(userId, guestSessionId);

        ShoppingCart cart = cartRepository.findById(cartId)
                .orElseGet(() -> cartRepository.save(ShoppingCart.builder()
                        .id(cartId)
                        .userId(userId)
                        .build()));

        CartItem existing = cartItemRepository.findByCartIdAndVariantId(cartId, request.getVariantId()).orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setIsAiRecommended(request.getIsAiRecommended());
            existing.setSourceBundleId(request.getSourceBundleId());
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .id(StringUtil.generateUniqueId())
                    .cartId(cartId)
                    .variantId(request.getVariantId())
                    .quantity(request.getQuantity())
                    .isAiRecommended(request.getIsAiRecommended())
                    .sourceBundleId(request.getSourceBundleId())
                    .build();
            cartItemRepository.save(item);
        }

        return getCart(userId, guestSessionId);
    }

    public CartResponse updateQuantity(String userId, String guestSessionId, String itemId, Integer quantity) {
        String cartId = getCartId(userId, guestSessionId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Không tìm thấy sản phẩm trong giỏ", 404));

        if (!item.getCartId().equals(cartId)) {
            throw new BusinessException("ACCESS_DENIED", "Không có quyền truy cập sản phẩm này", 403);
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart(userId, guestSessionId);
    }

    public void removeItem(String userId, String guestSessionId, String itemId) {
        String cartId = getCartId(userId, guestSessionId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Không tìm thấy sản phẩm trong giỏ", 404));

        if (!item.getCartId().equals(cartId)) {
            throw new BusinessException("ACCESS_DENIED", "Không có quyền xóa sản phẩm này", 403);
        }

        cartItemRepository.delete(item);
    }

    public CartResponse mergeCart(String userId, CartMergeRequest request) {
        String guestCartId = "guest_" + request.getGuestSessionId();
        String userCartId = userId;

        ShoppingCart guestCart = cartRepository.findById(guestCartId).orElse(null);
        if (guestCart == null) {
            return getCart(userId, null);
        }

        log.info("Merging guest cart into user cart: userId={}, guestCartPresent=true", userId);

        ShoppingCart userCart = cartRepository.findById(userCartId).orElse(null);
        if (userCart == null) {
            List<CartItem> guestOnlyItems = cartItemRepository.findByCartId(guestCartId);
            cartRepository.delete(guestCart);
            cartRepository.save(ShoppingCart.builder()
                    .id(userCartId)
                    .userId(userId)
                    .build());
            guestOnlyItems.forEach(item -> item.setCartId(userCartId));
            cartItemRepository.saveAll(guestOnlyItems);
            return getCart(userId, null);
        }

        List<CartItem> guestItems = cartItemRepository.findByCartId(guestCartId);
        log.info("Guest cart merge details: userId={}, guestCartId={}, guestItemCount={}", userId, guestCartId, guestItems.size());
        for (CartItem guestItem : guestItems) {
            CartItem existing = cartItemRepository.findByCartIdAndVariantId(userCartId, guestItem.getVariantId()).orElse(null);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + guestItem.getQuantity());
                cartItemRepository.save(existing);
                cartItemRepository.delete(guestItem);
            } else {
                guestItem.setCartId(userCartId);
                cartItemRepository.save(guestItem);
            }
        }

        cartRepository.delete(guestCart);

        return getCart(userId, null);
    }

    public void clearCart(String userId, String guestSessionId) {
        String cartId = getCartId(userId, guestSessionId);

        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        if (!items.isEmpty()) {
            cartItemRepository.deleteAll(items);
        }
        cartRepository.findById(cartId).ifPresent(cartRepository::delete);
    }

    private void validateVariant(String variantId) {
        ProductClient.VariantSnapshot snapshot;
        try {
            var response = productClient.getVariantSnapshot(variantId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể sản phẩm", 404);
            }
            snapshot = response.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to validate variant {}: {}", variantId, ex.getMessage());
            throw new BusinessException("VARIANT_NOT_FOUND", "Không tìm thấy biến thể sản phẩm", 404);
        }

        if (!"ACTIVE".equalsIgnoreCase(snapshot.getStatus())) {
            throw new BusinessException("PRODUCT_NOT_ACTIVE", "Sản phẩm hiện không khả dụng", 400);
        }
        if (Boolean.FALSE.equals(snapshot.getActive())
                || (snapshot.getStockQuantity() != null && snapshot.getStockQuantity() <= 0)) {
            throw new BusinessException("VARIANT_OUT_OF_STOCK", "Biến thể này đã hết hàng.", 400);
        }
    }

    private CartResponse buildCartResponse(ShoppingCart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
            .map(this::buildItemResponse)
            .collect(Collectors.toList());

        BigDecimal totalAmount = itemResponses.stream()
            .map(ir -> unitPrice(ir).multiply(BigDecimal.valueOf(ir.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();

        return CartResponse.builder()
            .cartId(cart.getId())
            .items(itemResponses)
            .totalAmount(totalAmount)
            .totalQuantity(totalQuantity)
            .build();
    }

    // Display-only price: authoritative price is re-fetched from product-service
    // by order-service at checkout and stored as price_at_purchase, so it is
    // safe (and expected) for this figure to go stale between cart view and order.
    private BigDecimal unitPrice(CartItemResponse item) {
        if (!Boolean.TRUE.equals(item.getAvailable()) || item.getVariant() == null || item.getVariant().getProduct() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal priceOverride = item.getVariant().getPriceOverride();
        BigDecimal basePrice = item.getVariant().getProduct().getBasePrice();
        BigDecimal price = priceOverride != null ? priceOverride : basePrice;
        return price != null ? price : BigDecimal.ZERO;
    }

    private CartItemResponse buildItemResponse(CartItem item) {
        CartItemResponse.CartItemResponseBuilder response = CartItemResponse.builder()
            .id(item.getId())
            .cartId(item.getCartId())
            .variantId(item.getVariantId())
            .quantity(item.getQuantity())
            .isAiRecommended(item.getIsAiRecommended())
            .sourceBundleId(item.getSourceBundleId())
            .addedAt(item.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());

        ProductClient.VariantSnapshot snapshot = fetchSnapshot(item.getVariantId());
        if (snapshot == null || !"ACTIVE".equalsIgnoreCase(snapshot.getStatus())) {
            return response
                .available(false)
                .unavailableMessage("This item is no longer available.")
                .build();
        }

        CartItemResponse.VariantInfo.ProductInfo.ImageInfo image = snapshot.getPrimaryImageUrl() != null
            ? CartItemResponse.VariantInfo.ProductInfo.ImageInfo.builder()
                .imageUrl(snapshot.getPrimaryImageUrl())
                .isPrimary(true)
                .build()
            : null;

        CartItemResponse.VariantInfo.ProductInfo product = CartItemResponse.VariantInfo.ProductInfo.builder()
            .id(snapshot.getProductId())
            .name(snapshot.getProductName())
            .basePrice(snapshot.getEffectivePrice())
            .images(image != null ? List.of(image) : List.of())
            .build();

        CartItemResponse.VariantInfo variant = CartItemResponse.VariantInfo.builder()
            .id(snapshot.getVariantId())
            .sku(snapshot.getSku())
            .size(snapshot.getSize())
            .color(snapshot.getColor())
            .material(snapshot.getMaterial())
            .product(product)
            .build();

        return response
            .available(true)
            .variant(variant)
            .build();
    }

    private ProductClient.VariantSnapshot fetchSnapshot(String variantId) {
        try {
            var response = productClient.getVariantSnapshot(variantId);
            return response != null && response.isSuccess() ? response.getData() : null;
        } catch (Exception ex) {
            log.warn("Failed to fetch variant snapshot {} for cart display: {}", variantId, ex.getMessage());
            return null;
        }
    }
}
