package com.stylemind.auth.controller;

import com.stylemind.auth.dto.InternalUserEmailResponse;
import com.stylemind.auth.entity.User;
import com.stylemind.auth.repository.UserRepository;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}/email")
    public ResponseEntity<ApiResponse<InternalUserEmailResponse>> getUserEmail(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found", 404));
        InternalUserEmailResponse response = InternalUserEmailResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .build();
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }
}
