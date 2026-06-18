package com.stylemind.ai.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_curated_bundles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCuratedBundle extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "message_id", length = 50, nullable = false)
    private String messageId;

    @Column(name = "justification_summary", columnDefinition = "TEXT")
    private String justificationSummary;
}