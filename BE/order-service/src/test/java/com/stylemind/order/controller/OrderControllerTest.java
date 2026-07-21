package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.order.dto.OrderSummaryResponse;
import com.stylemind.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final OrderController controller = new OrderController(orderService);

    @Test
    void getOrders_usesPrincipalUserAndDefaultPagination() {
        UserPrincipal principal = new UserPrincipal(
                "user-1",
                "customer@example.com",
                null,
                "CUSTOMER",
                "LOCAL",
                true);
        PageResponse<OrderSummaryResponse> page = PageResponse.<OrderSummaryResponse>builder()
                .content(List.of(OrderSummaryResponse.builder()
                        .id("order-1")
                        .createdAt(Instant.parse("2026-07-22T01:00:00Z"))
                        .orderStatus("PROCESSING")
                        .totalAmount(new BigDecimal("399000"))
                        .itemCount(2)
                        .build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        PageRequest expectedPageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(orderService.getOrdersPage("user-1", null, expectedPageable)).thenReturn(page);

        ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> response =
                controller.getOrders(principal, null, null, null, null);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getPage()).isZero();
        verify(orderService).getOrdersPage("user-1", null, expectedPageable);
    }

    @Test
    void getOrders_usesRequestedPaginationWithinBounds() {
        UserPrincipal principal = new UserPrincipal(
                "user-1",
                "customer@example.com",
                null,
                "CUSTOMER",
                "LOCAL",
                true);
        PageRequest expectedPageable = PageRequest.of(
                2,
                50,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        PageResponse<OrderSummaryResponse> page = PageResponse.<OrderSummaryResponse>builder()
                .content(List.of())
                .page(2)
                .size(50)
                .totalElements(0)
                .totalPages(0)
                .first(false)
                .last(true)
                .empty(true)
                .build();
        when(orderService.getOrdersPage("user-1", "PROCESSING", expectedPageable)).thenReturn(page);

        controller.getOrders(principal, 2, 50, "createdAt,desc", "PROCESSING");

        verify(orderService).getOrdersPage("user-1", "PROCESSING", expectedPageable);
    }
}
