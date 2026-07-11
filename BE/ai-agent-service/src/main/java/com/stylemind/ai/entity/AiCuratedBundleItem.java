package com.stylemind.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "ai_curated_bundle_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AiCuratedBundleItem.AiCuratedBundleItemId.class)
public class AiCuratedBundleItem {

    @Id
    @Column(name = "bundle_id", length = 50, nullable = false)
    private String bundleId;

    @Id
    @Column(name = "product_id", length = 50, nullable = false)
    private String productId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiCuratedBundleItemId implements Serializable {
        private String bundleId;
        private String productId;
    }
}
