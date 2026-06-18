package com.stylemind.ai.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_index_jobs")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiIndexJob extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "target_type", length = 30, nullable = false)
    private String targetType; // PRODUCT, INVENTORY, RULE

    @Column(name = "target_id", length = 50, nullable = false)
    private String targetId;

    @Column(name = "operation_type", length = 10, nullable = false)
    private String operationType; // CREATE, UPDATE, DELETE

    @Column(name = "status", length = 20, nullable = false)
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;
}