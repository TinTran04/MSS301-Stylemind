package com.stylemind.auth.controller;

import com.stylemind.auth.dto.AdminUserResponse;
import com.stylemind.auth.dto.AdminCreateUserRequest;
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
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AdminUserResponse user = authService.createUserByAdmin(request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản thành công và đã gửi email thiết lập mật khẩu", user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled) {
        PageResponse<AdminUserResponse> result = authService.listUsers(page, size, search, role, enabled);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
    }


    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable String userId) {
        AdminUserResponse user = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeRole(
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AdminUserResponse user = authService.changeUserRole(userId, principal.getUserId(), request.getRole());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật role thành công", user));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeEnabled(
            @PathVariable String userId,
            @Valid @RequestBody ChangeEnabledRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AdminUserResponse user = authService.changeUserEnabled(userId, principal.getUserId(), request.getEnabled());
        String msg = Boolean.TRUE.equals(request.getEnabled()) ? "Mở khóa tài khoản thành công" : "Khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, user));
    }
}
