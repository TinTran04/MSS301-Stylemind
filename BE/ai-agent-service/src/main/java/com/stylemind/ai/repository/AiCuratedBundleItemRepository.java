package com.stylemind.ai.repository;

import com.stylemind.ai.entity.AiCuratedBundleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiCuratedBundleItemRepository extends JpaRepository<AiCuratedBundleItem, AiCuratedBundleItem.AiCuratedBundleItemId> {
    List<AiCuratedBundleItem> findByBundleId(String bundleId);
    List<AiCuratedBundleItem> findByProductId(String productId);
}