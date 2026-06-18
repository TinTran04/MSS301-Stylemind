package com.stylemind.ai.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_analytics_logs")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalyticsLog extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "bundle_id", length = 50, nullable = false)
    private String bundleId;

    @Column(name = "interaction_type", length = 30, nullable = false)
    private String interactionType; // IMPRESSION, CLICK, ADD_TO_CART
}