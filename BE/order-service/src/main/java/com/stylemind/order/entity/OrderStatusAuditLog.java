package com.stylemind.order.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_status_audit_log")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusAuditLog extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_id", length = 50, nullable = false)
    private String orderId;

    @Column(name = "actor_id", length = 50, nullable = false)
    private String actorId;

    @Column(name = "from_status", length = 30)
    @Enumerated(EnumType.STRING)
    private OrderStatus fromStatus;

    @Column(name = "to_status", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus toStatus;
}
