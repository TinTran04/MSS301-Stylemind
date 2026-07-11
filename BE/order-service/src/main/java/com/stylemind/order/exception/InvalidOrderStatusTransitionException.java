package com.stylemind.order.exception;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.entity.OrderStatus;

// Extends BusinessException so it flows through the shared
// GlobalExceptionHandler (@RestControllerAdvice in common-lib) without
// needing a duplicate advice class per service.
public class InvalidOrderStatusTransitionException extends BusinessException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super(
                "INVALID_ORDER_STATUS_TRANSITION",
                String.format("Cannot transition order from %s to %s", from, to),
                409
        );
    }
}
