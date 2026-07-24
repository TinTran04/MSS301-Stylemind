package com.stylemind.order.entity;

public enum OrderReturnStatus {
    REQUESTED,
    AWAITING_BANK_INFO,
    BANK_INFO_SUBMITTED,
    REFUNDED,
    REJECTED
}
