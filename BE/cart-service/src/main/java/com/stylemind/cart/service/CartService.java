package com.stylemind.cart.service;

import com.stylemind.cart.dto.*;
import com.stylemind.cart.entity.CartItem;
import com.stylemind.cart.entity.ShoppingCart;
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

        ShoppingCart userCart = cartRepository.findById(userCartId).orElse(null);
        if (userCart == null) {
            cartRepository.delete(guestCart);
            guestCart.setId(userCartId);
            guestCart.setUserId(userId);
            cartRepository.save(guestCart);
            return getCart(userId, null);
        }

        List<CartItem> guestItems = cartItemRepository.findByCartId(guestCartId);
        for (CartItem guestItem : guestItems) {
            CartItem existing = cartItemRepository.findByCartIdAndVariantId(userCartId, guestItem.getVariantId()).orElse(null);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + guestItem.getQuantity());
                cartItemRepository.save(existing);
            } else {
                guestItem.setCartId(userCartId);
                cartItemRepository.save(guestItem);
            }
        }

        cartItemRepository.deleteAll(guestItems);
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

    private CartResponse buildCartResponse(ShoppingCart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream().map(item ->
            CartItemResponse.builder()
                .id(item.getId())
                .cartId(item.getCartId())
                .variantId(item.getVariantId())
                .quantity(item.getQuantity())
                .isAiRecommended(item.getIsAiRecommended())
                .sourceBundleId(item.getSourceBundleId())
                .addedAt(item.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build()
        ).collect(Collectors.toList());

        BigDecimal totalAmount = itemResponses.stream()
            .filter(ir -> ir.getVariant() != null && ir.getVariant().getProduct() != null)
            .map(ir -> ir.getVariant().getPriceOverride() != null ? ir.getVariant().getPriceOverride() : ir.getVariant().getProduct().getBasePrice())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();

        return CartResponse.builder()
            .cartId(cart.getId())
            .items(itemResponses)
            .totalAmount(totalAmount)
            .totalQuantity(totalQuantity)
            .build();
    }
}