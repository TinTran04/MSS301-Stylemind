package com.stylemind.auth.controller;

import com.stylemind.auth.dto.AccountStatusResponse;
import com.stylemind.auth.dto.RoleResponse;
import com.stylemind.auth.dto.UpdateAccountStatusRequest;
import com.stylemind.auth.dto.UpdateRoleRequest;
import com.stylemind.auth.service.AuthService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> updateAccountStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update account status successfully",
                authService.updateAccountStatus(userId, request)));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<RoleResponse>> updateAccountRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update account role successfully",
                authService.updateAccountRole(userId, request)));
    }
}
