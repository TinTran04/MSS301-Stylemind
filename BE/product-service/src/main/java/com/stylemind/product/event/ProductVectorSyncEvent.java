package com.stylemind.product.event;

public record ProductVectorSyncEvent(String productId, Action action) {

    public enum Action {
        UPSERT,
        DELETE
    }
}
