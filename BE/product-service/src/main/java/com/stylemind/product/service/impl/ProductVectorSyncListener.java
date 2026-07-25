package com.stylemind.product.service.impl;

import com.stylemind.product.event.ProductVectorSyncEvent;
import com.stylemind.product.feign.AiStylistInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductVectorSyncListener {

    private static final int MAX_ATTEMPTS = 3;

    private final AiStylistInternalClient aiStylistClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductVectorSync(ProductVectorSyncEvent event) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                switch (event.action()) {
                    case UPSERT -> aiStylistClient.syncProduct(event.productId());
                    case DELETE -> aiStylistClient.deleteProduct(event.productId());
                }
                return;
            } catch (Exception ex) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Failed to sync product {} ({}) to Qdrant after {} attempts: {}",
                            event.productId(), event.action(), attempt, ex.getMessage());
                } else {
                    log.debug("Qdrant sync attempt {} failed for product {} ({}), retrying: {}",
                            attempt, event.productId(), event.action(), ex.getMessage());
                }
            }
        }
    }
}
