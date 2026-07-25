package com.stylemind.auth.controller;

import com.stylemind.auth.dto.InternalUserEmailResponse;
import com.stylemind.auth.service.AuthService;
import com.stylemind.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final AuthService authService;

    @GetMapping("/{userId}/email")
    public ResponseEntity<ApiResponse<InternalUserEmailResponse>> getUserEmail(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("OK", authService.getUserEmail(userId)));
    }
}
