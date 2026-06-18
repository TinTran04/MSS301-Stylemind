package com.stylemind.common.constant;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Auth errors
    AUTH_INVALID_CREDENTIALS("Email hoặc mật khẩu không đúng", 401),
    AUTH_TOKEN_EXPIRED("JWT token hết hạn", 401),
    AUTH_TOKEN_INVALID("JWT token không hợp lệ", 401),
    AUTH_ACCESS_DENIED("Không có quyền truy cập tài nguyên này", 403),
    AUTH_USER_NOT_FOUND("Không tìm thấy người dùng", 404),
    AUTH_EMAIL_EXISTS("Email đã được sử dụng", 400),
    AUTH_ACCOUNT_DISABLED("Tài khoản đã bị khóa", 403),
    
    // Resource not found
    PRODUCT_NOT_FOUND("Không tìm thấy sản phẩm", 404),
    VARIANT_NOT_FOUND("Không tìm thấy biến thể sản phẩm", 404),
    CATEGORY_NOT_FOUND("Không tìm thấy danh mục", 404),
    CART_ITEM_NOT_FOUND("Không tìm thấy sản phẩm trong giỏ hàng", 404),
    ORDER_NOT_FOUND("Không tìm thấy đơn hàng", 404),
    USER_NOT_FOUND("Không tìm thấy người dùng", 404),
    ADDRESS_NOT_FOUND("Không tìm thấy địa chỉ", 404),
    STYLE_PROFILE_NOT_FOUND("Không tìm thấy hồ sơ phong cách", 404),
    INVENTORY_NOT_FOUND("Không tìm thấy tồn kho", 404),
    BUNDLE_NOT_FOUND("Không tìm thấy bộ outfit", 404),
    CONVERSATION_NOT_FOUND("Không tìm thấy hội thoại", 404),
    NOTIFICATION_NOT_FOUND("Không tìm thấy thông báo", 404),
    TRANSACTION_NOT_FOUND("Không tìm thấy giao dịch", 404),
    JOB_NOT_FOUND("Không tìm thấy job", 404),
    
    // Business logic errors
    ORDER_OUT_OF_STOCK("Số lượng tồn kho không đủ", 400),
    PAYMENT_DECLINED("Thanh toán bị từ chối", 400),
    INSUFFICIENT_INVENTORY("Không đủ hàng trong kho", 400),
    CART_EMPTY("Giỏ hàng trống", 400),
    INVALID_ORDER_STATUS("Trạng thái đơn hàng không hợp lệ", 400),
    OWNERSHIP_MISMATCH("Không khớp quyền sở hữu", 403),
    EMAIL_ALREADY_EXISTS("Email đã tồn tại", 400),
    ADDRESS_MISMATCH("Địa chỉ không khớp", 400),
    
    // AI errors
    AI_RATE_LIMIT_EXCEEDED("Vượt quá giới hạn yêu cầu AI", 429),
    AI_PROVIDER_ERROR("Lỗi nhà cung cấp AI", 502),
    AI_INDEX_JOB_FAILED("Lỗi index job AI", 500),
    
    // General errors
    VALIDATION_ERROR("Dữ liệu đầu vào không hợp lệ", 400),
    INTERNAL_ERROR("Lỗi hệ thống", 500),
    SERVICE_UNAVAILABLE("Dịch vụ không khả dụng", 503),
    INTERNAL_TOKEN_INVALID("Token nội bộ không hợp lệ", 401),
    INVALID_REQUEST("Yêu cầu không hợp lệ", 400);

    private final String message;
    private final int httpStatus;

    ErrorCode(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }
}