package com.stylemind.cart.service;

import com.stylemind.cart.dto.CartItemRequest;
import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.cart.entity.CartItem;
import com.stylemind.cart.entity.ShoppingCart;
import com.stylemind.cart.feign.ProductClient;
import com.stylemind.cart.repository.CartItemRepository;
import com.stylemind.cart.repository.ShoppingCartRepository;
import com.stylemind.cart.service.impl.CartServiceImpl;
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

import java.math.BigDecimal;
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

    @InjectMocks CartServiceImpl cartService;

    // ─── addItem ─────────────────────────────────────────────────────────────

    @Test
    void addItem_existingUserCart_reusesCartByUserId_notUserIdAsCartId() {
        ShoppingCart existingCart = cart("cart_customer", "usr_customer");
        when(productClient.getVariantSnapshot("var-A")).thenReturn(activeVariant("var-A"));
        when(cartRepository.findByUserId("usr_customer")).thenReturn(Optional.of(existingCart));
        when(cartItemRepository.findByCartIdAndVariantId("cart_customer", "var-A")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("cart_customer")).thenReturn(List.of());

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(1);

        cartService.addItem("usr_customer", null, req);

        verify(cartRepository, never()).save(any(ShoppingCart.class));
        verify(cartItemRepository).save(argThat(item ->
                "cart_customer".equals(item.getCartId())
                        && "var-A".equals(item.getVariantId())));
    }

    @Test
    void addItem_firstUserCart_handlesConcurrentInsertByReloadingWinner() {
        ShoppingCart createdCart = cart("cart-winner", "usr-new");
        when(productClient.getVariantSnapshot("var-A")).thenReturn(activeVariant("var-A"));
        when(cartRepository.findByUserId("usr-new"))
                .thenReturn(Optional.empty(), Optional.of(createdCart));
        when(cartRepository.insertIfAbsent(any(), org.mockito.ArgumentMatchers.eq("usr-new")))
                .thenReturn(0);
        when(cartItemRepository.findByCartIdAndVariantId("cart-winner", "var-A")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("cart-winner")).thenReturn(List.of());

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(1);

        cartService.addItem("usr-new", null, req);

        verify(cartRepository).insertIfAbsent(any(), org.mockito.ArgumentMatchers.eq("usr-new"));
        verify(cartRepository, never()).save(any(ShoppingCart.class));
        verify(cartItemRepository).save(argThat(item -> "cart-winner".equals(item.getCartId())));
    }

    @Test
    void addItem_newUser_createsExactlyOneCartThroughIdempotentInsert() {
        ShoppingCart createdCart = cart("cart-new", "usr-new");
        when(productClient.getVariantSnapshot("var-A")).thenReturn(activeVariant("var-A"));
        when(cartRepository.findByUserId("usr-new"))
                .thenReturn(Optional.empty(), Optional.of(createdCart));
        when(cartRepository.insertIfAbsent(any(), org.mockito.ArgumentMatchers.eq("usr-new")))
                .thenReturn(1);
        when(cartItemRepository.findByCartIdAndVariantId("cart-new", "var-A")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("cart-new")).thenReturn(List.of());

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(1);

        cartService.addItem("usr-new", null, req);

        verify(cartRepository).insertIfAbsent(any(), org.mockito.ArgumentMatchers.eq("usr-new"));
        verify(cartRepository, never()).save(any(ShoppingCart.class));
        verify(cartItemRepository).save(argThat(item -> "cart-new".equals(item.getCartId())));
    }

    @Test
    void addItem_newItem_savesItem() {
        ShoppingCart cart = cart("user-1");
        when(productClient.getVariantSnapshot("var-A")).thenReturn(activeVariant("var-A"));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
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
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
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

    // ─── getCart display enrichment ────────────────────────────────────────────

    @Test
    void getCart_enrichesItem_withProductNameSizeColorMaterialFromSnapshot() {
        CartItem item = cartItem("item-1", "user-1", "var-A", 2);
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart("user-1")));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(item));
        when(productClient.getVariantSnapshot("var-A")).thenReturn(
                displayVariant("var-A", "p-1", "Silk Shirt", "M", "Black", "Silk", new BigDecimal("379000"), "http://img/1.png"));

        CartResponse cart = cartService.getCart("user-1", null);

        CartItemResponse response = cart.getItems().get(0);
        assertThat(response.getAvailable()).isTrue();
        assertThat(response.getVariant().getSize()).isEqualTo("M");
        assertThat(response.getVariant().getColor()).isEqualTo("Black");
        assertThat(response.getVariant().getMaterial()).isEqualTo("Silk");
        assertThat(response.getVariant().getProduct().getName()).isEqualTo("Silk Shirt");
        assertThat(response.getVariant().getProduct().getImages().get(0).getImageUrl()).isEqualTo("http://img/1.png");
    }

    @Test
    void getCart_unitPriceFromSnapshot_andTotalAmountMultipliesByQuantity() {
        CartItem item = cartItem("item-1", "user-1", "var-A", 3);
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart("user-1")));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(item));
        when(productClient.getVariantSnapshot("var-A")).thenReturn(
                displayVariant("var-A", "p-1", "Silk Shirt", "M", "Black", "Silk", new BigDecimal("100000"), null));

        CartResponse cart = cartService.getCart("user-1", null);

        // unitPrice (100000) * quantity (3) = 300000
        assertThat(cart.getTotalAmount()).isEqualByComparingTo("300000");
    }

    @Test
    void getCart_variantSnapshotMissing_marksItemUnavailable_withZeroContributionToTotal() {
        CartItem item = cartItem("item-1", "user-1", "var-gone", 1);
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart("user-1")));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(item));
        when(productClient.getVariantSnapshot("var-gone")).thenReturn(ApiResponse.error("VARIANT_NOT_FOUND", "Not found"));

        CartResponse cart = cartService.getCart("user-1", null);

        CartItemResponse response = cart.getItems().get(0);
        assertThat(response.getAvailable()).isFalse();
        assertThat(response.getUnavailableMessage()).isNotBlank();
        assertThat(response.getVariant()).isNull();
        assertThat(cart.getTotalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void getCart_variantInactive_marksItemUnavailable() {
        CartItem item = cartItem("item-1", "user-1", "var-A", 1);
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart("user-1")));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(item));
        when(productClient.getVariantSnapshot("var-A")).thenReturn(inactiveVariant("var-A"));

        CartResponse cart = cartService.getCart("user-1", null);

        assertThat(cart.getItems().get(0).getAvailable()).isFalse();
    }

    @Test
    void getCart_productClientThrows_marksItemUnavailable_doesNotCrash() {
        CartItem item = cartItem("item-1", "user-1", "var-A", 1);
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart("user-1")));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(item));
        when(productClient.getVariantSnapshot("var-A")).thenThrow(new RuntimeException("network error"));

        CartResponse cart = cartService.getCart("user-1", null);

        assertThat(cart.getItems().get(0).getAvailable()).isFalse();
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
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));

        cartService.clearCart("user-1", null);

        verify(cartItemRepository).deleteAll(List.of(i1, i2));
        verify(cartRepository).delete(cart);
    }

    @Test
    void clearCart_emptyCart_noDeleteCalled() {
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of());
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        cartService.clearCart("user-1", null);

        verify(cartItemRepository, never()).deleteAll(any());
        verify(cartRepository, never()).delete(any(ShoppingCart.class));
    }

    // ─── mergeCart ────────────────────────────────────────────────────────────

    @Test
    void mergeCart_noGuestCart_returnsUserCart() {
        when(cartRepository.findById("guest_sess-1")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart("user-1")));
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
        ShoppingCart userCart = cart("cart-user-1", "user-1");
        when(cartRepository.findByUserId("user-1"))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(userCart));
        when(cartRepository.insertIfAbsent(any(), org.mockito.ArgumentMatchers.eq("user-1"))).thenReturn(1);
        when(cartItemRepository.findByCartId("guest_sess-1")).thenReturn(List.of(guestItem));
        when(cartItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("cart-user-1")).thenReturn(List.of(guestItem));

        CartMergeRequest req = new CartMergeRequest();
        req.setGuestSessionId("sess-1");

        cartService.mergeCart("user-1", req);

        // The guest item's cartId must be repointed to the user's cart, not left dangling
        assertThat(guestItem.getCartId()).isEqualTo("cart-user-1");
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
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(userCart));
        when(cartItemRepository.findByCartId("guest_sess-1")).thenReturn(List.of(guestItem));
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-A")).thenReturn(Optional.of(userItem));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(userItem));

        CartMergeRequest req = new CartMergeRequest();
        req.setGuestSessionId("sess-1");

        cartService.mergeCart("user-1", req);

        assertThat(userItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).delete(guestItem);
        verify(cartItemRepository, never()).save(argThat(item -> item.getCartId().equals("guest_sess-1")));
    }

    @Test
    void mergeCart_existingUserCart_differentVariant_copiedAsSeparateItemNotMerged() {
        ShoppingCart guestCart = cart("guest_sess-1");
        ShoppingCart userCart = cart("user-1");
        CartItem guestItem = cartItem("g-item-1", "guest_sess-1", "var-B", 2);
        CartItem userItem = cartItem("u-item-1", "user-1", "var-A", 3);

        when(cartRepository.findById("guest_sess-1")).thenReturn(Optional.of(guestCart));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(userCart));
        when(cartItemRepository.findByCartId("guest_sess-1")).thenReturn(List.of(guestItem));
        // No existing item for var-B in the user's cart -> should be copied over, not merged.
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-B")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(userItem, guestItem));

        CartMergeRequest req = new CartMergeRequest();
        req.setGuestSessionId("sess-1");

        cartService.mergeCart("user-1", req);

        // The distinct-variant guest item is repointed to the user's cart as its own row,
        // and the pre-existing user item for a different variant is left untouched.
        assertThat(guestItem.getCartId()).isEqualTo("user-1");
        assertThat(guestItem.getQuantity()).isEqualTo(2);
        assertThat(userItem.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(guestItem);
        verify(cartItemRepository, never()).save(userItem);
        verify(cartItemRepository, never()).deleteAll(List.of(guestItem));
        verify(cartItemRepository, never()).delete(guestItem);
    }

    @Test
    void mergeCart_existingUserCart_mixedVariants_movesDistinctItemAndDeletesOnlyMergedGuestDuplicate() {
        ShoppingCart guestCart = cart("guest_sess-1");
        ShoppingCart userCart = cart("user-1");
        CartItem guestDuplicate = cartItem("g-item-1", "guest_sess-1", "var-A", 2);
        CartItem guestDistinct = cartItem("g-item-2", "guest_sess-1", "var-B", 1);
        CartItem userItem = cartItem("u-item-1", "user-1", "var-A", 3);

        when(cartRepository.findById("guest_sess-1")).thenReturn(Optional.of(guestCart));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(userCart));
        when(cartItemRepository.findByCartId("guest_sess-1")).thenReturn(List.of(guestDuplicate, guestDistinct));
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-A")).thenReturn(Optional.of(userItem));
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-B")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of(userItem, guestDistinct));

        CartMergeRequest req = new CartMergeRequest();
        req.setGuestSessionId("sess-1");

        cartService.mergeCart("user-1", req);

        assertThat(userItem.getQuantity()).isEqualTo(5);
        assertThat(guestDistinct.getCartId()).isEqualTo("user-1");
        verify(cartItemRepository).delete(guestDuplicate);
        verify(cartItemRepository, never()).delete(guestDistinct);
        verify(cartRepository).delete(guestCart);
    }

    @Test
    void addItem_outOfStockVariant_rejectedWithFriendlyMessage() {
        when(productClient.getVariantSnapshot("var-A")).thenReturn(outOfStockVariant("var-A"));

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem("user-1", null, req))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("VARIANT_OUT_OF_STOCK");
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_inactiveVariantFlag_rejectedEvenIfProductStatusActive() {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId("var-A");
        snapshot.setStatus("ACTIVE");
        snapshot.setActive(false);
        snapshot.setStockQuantity(5);
        when(productClient.getVariantSnapshot("var-A")).thenReturn(ApiResponse.success(snapshot));

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem("user-1", null, req))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("VARIANT_OUT_OF_STOCK");
    }

    @Test
    void addItem_inStockActiveVariant_succeeds() {
        ShoppingCart cart = cart("user-1");
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId("var-A");
        snapshot.setStatus("ACTIVE");
        snapshot.setActive(true);
        snapshot.setStockQuantity(5);
        when(productClient.getVariantSnapshot("var-A")).thenReturn(ApiResponse.success(snapshot));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId("user-1", "var-A")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId("user-1")).thenReturn(List.of());

        CartItemRequest req = new CartItemRequest();
        req.setVariantId("var-A");
        req.setQuantity(1);

        cartService.addItem("user-1", null, req);

        verify(cartItemRepository).save(any());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ApiResponse<ProductClient.VariantSnapshot> activeVariant(String variantId) {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId(variantId);
        snapshot.setStatus("ACTIVE");
        return ApiResponse.success(snapshot);
    }

    private ApiResponse<ProductClient.VariantSnapshot> outOfStockVariant(String variantId) {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId(variantId);
        snapshot.setStatus("ACTIVE");
        snapshot.setActive(true);
        snapshot.setStockQuantity(0);
        return ApiResponse.success(snapshot);
    }

    private ApiResponse<ProductClient.VariantSnapshot> inactiveVariant(String variantId) {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId(variantId);
        snapshot.setStatus("INACTIVE");
        return ApiResponse.success(snapshot);
    }

    private ApiResponse<ProductClient.VariantSnapshot> displayVariant(
            String variantId, String productId, String productName, String size, String color,
            String material, BigDecimal effectivePrice, String primaryImageUrl) {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId(variantId);
        snapshot.setProductId(productId);
        snapshot.setProductName(productName);
        snapshot.setSize(size);
        snapshot.setColor(color);
        snapshot.setMaterial(material);
        snapshot.setEffectivePrice(effectivePrice);
        snapshot.setPrimaryImageUrl(primaryImageUrl);
        snapshot.setStatus("ACTIVE");
        return ApiResponse.success(snapshot);
    }

    private ShoppingCart cart(String id) {
        return cart(id, id);
    }

    private ShoppingCart cart(String id, String userId) {
        ShoppingCart c = new ShoppingCart();
        c.setId(id);
        c.setUserId(userId);
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
