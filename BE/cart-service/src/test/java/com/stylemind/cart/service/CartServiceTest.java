package com.stylemind.cart.service;

import com.stylemind.cart.dto.CartItemRequest;
import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.cart.entity.CartItem;
import com.stylemind.cart.entity.ShoppingCart;
import com.stylemind.cart.repository.CartItemRepository;
import com.stylemind.cart.repository.ShoppingCartRepository;
import com.stylemind.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    @Mock ShoppingCartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;

    @InjectMocks CartService cartService;

    // ─── addItem ─────────────────────────────────────────────────────────────

    @Test
    void addItem_newItem_savesItem() {
        ShoppingCart cart = cart("user-1");
        when(cartRepository.findById("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-A")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of());

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(2);

        cartService.addItem("user-1", null, req);

        verify(cartItemRepository).save(argThat(item -> item.getVariantId().equals("var-A") && item.getQuantity() == 2));
    }

    @Test
    void addItem_existingItem_mergesQuantity() {
        ShoppingCart cart = cart("user-1");
        CartItem existing = cartItem("item-1", "user-1", "var-A", 3);
        when(cartRepository.findById("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-A")).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(existing));

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(2);

        cartService.addItem("user-1", null, req);

        // Quantity should now be 3 + 2 = 5
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
    }

    // ─── updateQuantity ───────────────────────────────────────────────────────

    @Test
    void updateQuantity_zeroOrBelow_deletesItem() {
        CartItem item = cartItem("item-1", "user-1", "var-A", 3);
        when(cartItemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of());

        cartService.updateQuantity("user-1", null, "item-1", 0);

        verify(cartItemRepository).delete(item);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateQuantity_wrongOwner_throws() {
        CartItem item = cartItem("item-1", "other-user", "var-A", 1);
        when(cartItemRepository.findById("item-1")).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateQuantity("user-1", null, "item-1", 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không có quyền");
    }

    // ─── clearCart ────────────────────────────────────────────────────────────

    @Test
    void clearCart_deletesAllItemsAndCart() {
        CartItem i1 = cartItem("i1", "user-1", "var-A", 1);
        CartItem i2 = cartItem("i2", "user-1", "var-B", 2);
        ShoppingCart cart = cart("user-1");

        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(i1, i2));
        when(cartRepository.findById("user-1")).thenReturn(Optional.of(cart));

        cartService.clearCart("user-1", null);

        verify(cartItemRepository).deleteAll(List.of(i1, i2));
        verify(cartRepository).delete(cart);
    }

    @Test
    void clearCart_emptyCart_noDeleteCalled() {
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of());
        when(cartRepository.findById("user-1")).thenReturn(Optional.empty());

        cartService.clearCart("user-1", null);

        verify(cartItemRepository, never()).deleteAll(any());
        verify(cartRepository, never()).delete(any(ShoppingCart.class));
    }

    // ─── mergeCart ────────────────────────────────────────────────────────────

    @Test
    void mergeCart_noGuestCart_returnsUserCart() {
        when(cartRepository.findById("guest_sess-1")).thenReturn(Optional.empty());
        when(cartRepository.findById("user-1")).thenReturn(Optional.of(cart("user-1")));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of());

        CartMergeRequest req = new CartMergeRequest();
        req.setGuestSessionId("sess-1");

        CartResponse result = cartService.mergeCart("user-1", req);

        assertThat(result.getCartId()).isEqualTo("user-1");
        verify(cartItemRepository, never()).save(any());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ShoppingCart cart(String id) {
        ShoppingCart c = new ShoppingCart();
        c.setId(id);
        c.setUserId(id);
        return c;
    }

    private CartItem cartItem(String id, String cartId, String variantId, int qty) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setCartId(cartId);
        item.setVariantId(variantId);
        item.setQuantity(qty);
        item.setIsAiRecommended(false);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        return item;
    }
}
