package com.stylemind.cart.service;

import com.stylemind.cart.dto.CartItemRequest;
import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;

public interface CartService {

    CartResponse getCart(String userId, String guestSessionId);

    CartResponse addItem(String userId, String guestSessionId, CartItemRequest request);

    CartResponse updateQuantity(String userId, String guestSessionId, String itemId, Integer quantity);

    void removeItem(String userId, String guestSessionId, String itemId);

    CartResponse mergeCart(String userId, CartMergeRequest request);

    void clearCart(String userId, String guestSessionId);
}
