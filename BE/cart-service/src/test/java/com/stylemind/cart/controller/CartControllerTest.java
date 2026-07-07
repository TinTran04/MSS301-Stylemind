package com.stylemind.cart.controller;

import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.cart.service.CartService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock CartService cartService;

    @Test
    void mergeCart_noAuthenticatedPrincipal_returnsBadRequestWithoutCallingService() {
        CartController controller = new CartController(cartService);
        CartMergeRequest request = new CartMergeRequest();
        request.setGuestSessionId("guest_abc-123");

        ResponseEntity<ApiResponse<CartResponse>> response = controller.mergeCart(null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH_REQUIRED");
        verify(cartService, never()).mergeCart(anyString(), any());
    }

    @Test
    void mergeCart_authenticatedPrincipal_delegatesToServiceWithUserId() {
        CartController controller = new CartController(cartService);
        UserPrincipal principal = new UserPrincipal("user-1", "user@test.com", "hash", "CUSTOMER", "LOCAL", true);
        CartMergeRequest request = new CartMergeRequest();
        request.setGuestSessionId("guest_abc-123");
        CartResponse mergedCart = CartResponse.builder().cartId("user-1").build();
        when(cartService.mergeCart("user-1", request)).thenReturn(mergedCart);

        ResponseEntity<ApiResponse<CartResponse>> response = controller.mergeCart(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(mergedCart);
    }
}
