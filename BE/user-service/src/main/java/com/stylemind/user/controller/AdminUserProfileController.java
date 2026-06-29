package com.stylemind.user.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.user.dto.AdminUserDetailResponse;
import com.stylemind.user.dto.AdminUserPageResponse;
import com.stylemind.user.service.UserProfileService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminUserPageResponse>> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                "List user profiles successfully",
                userProfileService.listUsersForAdmin(page, size, search)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get user profile successfully",
                userProfileService.getUserForAdmin(userId)));
    }
}
