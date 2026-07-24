package com.stylemind.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "return_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @Column(name = "order_item_id", nullable = false, length = 64)
    private String orderItemId;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "variant_id", length = 64)
    private String variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "restock_status", length = 32)
    private String restockStatus; // PENDING, RESTOCKED, SKIPPED, FAILED
}
