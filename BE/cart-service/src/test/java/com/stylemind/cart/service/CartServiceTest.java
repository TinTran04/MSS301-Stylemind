package com.stylemind.cart.service;

import com.stylemind.cart.dto.CartItemRequest;
import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.cart.entity.CartItem;
import com.stylemind.cart.entity.ShoppingCart;
import com.stylemind.cart.feign.ProductClient;
import com.stylemind.cart.repository.CartItemRepository;
import com.stylemind.cart.repository.ShoppingCartRepository;
import com.stylemind.common.dto.ApiResponse;
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
    @Mock ProductClient productClient;

    @InjectMocks CartService cartService;

    // ─── addItem ─────────────────────────────────────────────────────────────

    @Test
    void addItem_newItem_savesItem() {
        ShoppingCart cart = cart("user-1");
        when(productClient.getVariantSnapshot("var-A")).thenReturn(activeVariant("var-A"));
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
        when(productClient.getVariantSnapshot("var-A")).thenReturn(activeVariant("var-A"));
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

    @Test
    void addItem_variantNotFound_throws404_andNeverSaves() {
        when(productClient.getVariantSnapshot("var-missing")).thenReturn(ApiResponse.error("VARIANT_NOT_FOUND", "Not found"));

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-missing");
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem("user-1", null, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(404));

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_variantInactive_throws400_andNeverSaves() {
        when(productClient.getVariantSnapshot("var-A")).thenReturn(inactiveVariant("var-A"));

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem("user-1", null, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(400));

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_zeroQuantity_throws400_andNeverCallsProductService() {
        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(0);

        assertThatThrownBy(() -> cartService.addItem("user-1", null, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(400));

        verify(productClient, never()).getVariantSnapshot(any());
        verify(cartItemRepository, never()).save(any());
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

    @Test
    void mergeCart_noExistingUserCart_reassignsGuestItemsToUserCart() {
        ShoppingCart guestCart = cart("guest_sess-1");
        CartItem guestItem = cartItem("g-item-1", "guest_sess-1", "var-A", 2);

        when(cartRepository.findById("guest_sess-1")).thenReturn(Optional.of(guestCart));
        when(cartRepository.findById("user-1")).thenReturn(Optional.empty());
        when(cartItemRepository.findByCartId("guest_sess-1")).thenReturn(List.of(guestItem));
        when(cartItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(guestItem));

        CartMergeRequest req = new CartMergeRequest();
        req.setGuestSessionId("sess-1");

        cartService.mergeCart("user-1", req);

        // The guest item's cartId must be repointed to the user's cart, not left dangling
        assertThat(guestItem.getCartId()).isEqualTo("user-1");
        verify(cartItemRepository).saveAll(List.of(guestItem));
        verify(cartRepository).delete(guestCart);
    }

    @Test
    void mergeCart_existingUserCart_sumsQuantitiesWithoutDuplicateVariants() {
        ShoppingCart guestCart = cart("guest_sess-1");
        ShoppingCart userCart = cart("user-1");
        CartItem guestItem = cartItem("g-item-1", "guest_sess-1", "var-A", 2);
        CartItem userItem = cartItem("u-item-1", "user-1", "var-A", 3);

        when(cartRepository.findById("guest_sess-1")).thenReturn(Optional.of(guestCart));
        when(cartRepository.findById("user-1")).thenReturn(Optional.of(userCart));
        when(cartItemRepository.findByCartId("guest_sess-1")).thenReturn(List.of(guestItem));
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-A")).thenReturn(Optional.of(userItem));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(userItem));

        CartMergeRequest req = new CartMergeRequest();
        req.setGuestSessionId("sess-1");

        cartService.mergeCart("user-1", req);

        assertThat(userItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).deleteAll(List.of(guestItem));
        verify(cartItemRepository, never()).save(argThat(item -> item.getCartId().equals("guest_sess-1")));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ApiResponse<ProductClient.VariantSnapshot> activeVariant(String variantId) {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId(variantId);
        snapshot.setStatus("ACTIVE");
        return ApiResponse.success(snapshot);
    }

    private ApiResponse<ProductClient.VariantSnapshot> inactiveVariant(String variantId) {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId(variantId);
        snapshot.setStatus("INACTIVE");
        return ApiResponse.success(snapshot);
    }

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
