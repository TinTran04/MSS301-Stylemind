package com.stylemind.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private Error error;
    private String requestId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .requestId(resolveRequestId())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Thao tác thành công", data);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .error(new Error(errorCode, message))
                .message(message)
                .requestId(resolveRequestId())
                .build();
    }

    private static String resolveRequestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String requestId = request.getHeader("X-Request-Id");
            if (requestId != null && !requestId.isBlank()) {
                return requestId;
            }
        }
        return UUID.randomUUID().toString();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private String code;
        private String message;
    }
}
