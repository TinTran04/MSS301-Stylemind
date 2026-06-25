package com.stylemind.auth.controller;

import com.stylemind.auth.dto.AdminUserResponse;
import com.stylemind.auth.dto.ChangeEnabledRequest;
import com.stylemind.auth.dto.ChangeRoleRequest;
import com.stylemind.auth.service.AuthService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        PageResponse<AdminUserResponse> result = authService.listUsers(page, size, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable String userId) {
        AdminUserResponse user = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeRole(
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AdminUserResponse user = authService.changeUserRole(userId, principal.getUserId(), request.getRole());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật role thành công", user));
    }

    @PutMapping("/{userId}/enabled")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeEnabled(
            @PathVariable String userId,
            @Valid @RequestBody ChangeEnabledRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AdminUserResponse user = authService.changeUserEnabled(userId, principal.getUserId(), request.getEnabled());
        String msg = Boolean.TRUE.equals(request.getEnabled()) ? "Mở khóa tài khoản thành công" : "Khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, user));
    }
}
