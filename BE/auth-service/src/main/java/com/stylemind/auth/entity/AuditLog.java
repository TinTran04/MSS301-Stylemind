package com.stylemind.auth.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_log")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "actor_user_id", length = 50, nullable = false)
    private String actorUserId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "target_user_id", length = 50, nullable = false)
    private String targetUserId;

    @Column(name = "detail", length = 500)
    private String detail;
}
