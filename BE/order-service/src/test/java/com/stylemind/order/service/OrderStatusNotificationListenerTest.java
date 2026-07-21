package com.stylemind.order.service;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.event.OrderStatusChangedEvent;
import com.stylemind.order.feign.NotificationClient;
import com.stylemind.order.feign.UserClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusNotificationListenerTest {

    @Mock UserClient userClient;
    @Mock NotificationClient notificationClient;

    @InjectMocks OrderStatusNotificationListener listener;

    @Test
    void completedStatus_sendsDeliveredNotificationToUser() {
        UserClient.UserEmail userEmail = new UserClient.UserEmail();
        userEmail.setEmail("buyer@example.com");
        when(userClient.getUserEmail("user-1")).thenReturn(ApiResponse.success("ok", userEmail));

        listener.handleOrderStatusChanged(new OrderStatusChangedEvent(
                "order-1", "user-1", OrderStatus.SHIPPED, OrderStatus.COMPLETED));

        ArgumentCaptor<NotificationClient.EmailRequest> captor =
                ArgumentCaptor.forClass(NotificationClient.EmailRequest.class);
        verify(notificationClient).sendEmail(captor.capture());
        NotificationClient.EmailRequest request = captor.getValue();
        assertThat(request.getRecipientEmail()).isEqualTo("buyer@example.com");
        assertThat(request.getType()).isEqualTo("ORDER_COMPLETED");
        assertThat(request.getTitle()).isEqualTo("Đơn hàng đã giao thành công");
        assertThat(request.getContent()).contains("#order-1", "giao thành công");
    }

    @Test
    void missingEmail_skipsNotification() {
        when(userClient.getUserEmail("user-1")).thenReturn(ApiResponse.success("ok", null));

        listener.handleOrderStatusChanged(new OrderStatusChangedEvent(
                "order-1", "user-1", OrderStatus.PROCESSING, OrderStatus.SHIPPED));

        verify(notificationClient, never()).sendEmail(any());
    }
}
