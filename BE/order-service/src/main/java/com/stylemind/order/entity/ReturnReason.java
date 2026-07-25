package com.stylemind.order.entity;

public enum ReturnReason {
    DEFECTIVE,
    DAMAGED,
    WRONG_ITEM,
    MISSING_ITEM,
    SIZE_NOT_FIT,
    CHANGED_MIND,
    OTHER;

    public boolean isEvidenceRequired() {
        return this == DEFECTIVE || this == DAMAGED || this == WRONG_ITEM || this == MISSING_ITEM;
    }
}
