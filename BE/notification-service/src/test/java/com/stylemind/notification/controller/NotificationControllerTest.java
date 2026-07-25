package com.stylemind.notification.controller;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.notification.dto.NotificationReadAllResponse;
import com.stylemind.notification.dto.NotificationResponse;
import com.stylemind.notification.dto.NotificationUnreadCountResponse;
import com.stylemind.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    NotificationService notificationService;

    @Test
    void getMyNotificationsBuildsBoundedPageableAndScopesToAuthenticatedUser() {
        NotificationController controller = new NotificationController(notificationService);
        UserPrincipal principal = principal("user-1");
        PageResponse<NotificationResponse> page = PageResponse.<NotificationResponse>builder()
                .content(List.of(NotificationResponse.builder().id(1L).build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .numberOfElements(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationService.getCustomerNotifications(eq("user-1"), eq(false), pageableCaptor.capture()))
                .thenReturn(page);

        var response = controller.getMyNotifications(principal, 0, 10, "createdAt,desc", false);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void getMyNotificationsRejectsInvalidPaginationAndUnsupportedSort() {
        NotificationController controller = new NotificationController(notificationService);
        UserPrincipal principal = principal("user-1");

        assertThatThrownBy(() -> controller.getMyNotifications(principal, -1, 10, "createdAt,desc", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "VALIDATION_ERROR");
        assertThatThrownBy(() -> controller.getMyNotifications(principal, 0, 51, "createdAt,desc", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "VALIDATION_ERROR");
        assertThatThrownBy(() -> controller.getMyNotifications(principal, 0, 10, "status,desc", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "VALIDATION_ERROR");
    }

    @Test
    void unreadCountUsesAuthenticatedUserOnly() {
        NotificationController controller = new NotificationController(notificationService);
        when(notificationService.getUnreadCount("user-1"))
                .thenReturn(NotificationUnreadCountResponse.builder().unreadCount(4L).build());

        var response = controller.getUnreadCount(principal("user-1"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getUnreadCount()).isEqualTo(4L);
        verify(notificationService).getUnreadCount("user-1");
    }

    @Test
    void markOneReadUsesAuthenticatedUserOnly() {
        NotificationController controller = new NotificationController(notificationService);
        when(notificationService.markNotificationRead("user-1", 9L))
                .thenReturn(NotificationResponse.builder().id(9L).read(true).build());

        var response = controller.markNotificationRead(principal("user-1"), 9L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().isRead()).isTrue();
        verify(notificationService).markNotificationRead("user-1", 9L);
    }

    @Test
    void markAllReadUsesAuthenticatedUserOnly() {
        NotificationController controller = new NotificationController(notificationService);
        when(notificationService.markAllNotificationsRead("user-1"))
                .thenReturn(NotificationReadAllResponse.builder().updatedCount(3).build());

        var response = controller.markAllNotificationsRead(principal("user-1"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getUpdatedCount()).isEqualTo(3);
        verify(notificationService).markAllNotificationsRead("user-1");
    }

    private UserPrincipal principal(String userId) {
        return new UserPrincipal(userId, "user@example.com", null, "CUSTOMER", null, true);
    }
}
