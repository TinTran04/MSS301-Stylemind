package com.stylemind.order.entity;

public enum ReturnStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    RETURN_IN_TRANSIT,
    RECEIVED,
    QC_PASSED,
    QC_FAILED,
    CLOSED,
    CANCELLED
}
