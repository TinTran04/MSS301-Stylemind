package com.stylemind.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "restock_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestockLog {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "operation_key", nullable = false, unique = true, length = 128)
    private String operationKey;

    @Column(name = "variant_id", nullable = false, length = 64)
    private String variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reason", length = 64)
    private String reason;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
