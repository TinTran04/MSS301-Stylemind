package com.stylemind.product.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_audit_log")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAuditLog extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "actor_id", length = 50, nullable = false)
    private String actorId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "product_id", length = 50, nullable = false)
    private String productId;

    @Column(name = "detail", length = 500)
    private String detail;
}
