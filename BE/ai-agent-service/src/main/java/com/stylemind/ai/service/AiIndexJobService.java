package com.stylemind.ai.service;

import com.stylemind.ai.dto.*;
import com.stylemind.ai.entity.*;
import com.stylemind.ai.repository.*;
import com.stylemind.ai.feign.*;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiIndexJobService {

    private final AiIndexJobRepository indexJobRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public void triggerReindex(String targetType, String targetId) {
        AiIndexJob job = AiIndexJob.builder()
                .id(StringUtil.generateUniqueId())
                .targetType(targetType)
                .targetId(targetId)
                .operationType("UPDATE")
                .status("PENDING")
                .build();
        indexJobRepository.save(job);
    }

    public void triggerFullReindex() {
        var response = productClient.getProductsBySkus(""); // Get all products
        if (response.isSuccess() && response.getData() != null) {
            for (ProductClient.ProductDetail product : response.getData()) {
                triggerReindex("PRODUCT", product.getId());
            }
        }
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void processPendingJobs() {
        List<AiIndexJob> pendingJobs = indexJobRepository.findByStatus("PENDING");
        
        for (AiIndexJob job : pendingJobs) {
            processJob(job);
        }
    }

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void retryFailedJobs() {
        List<AiIndexJob> failedJobs = indexJobRepository.findByStatusAndRetryCountLessThan("FAILED", 3);
        
        for (AiIndexJob job : failedJobs) {
            job.setStatus("PENDING");
            indexJobRepository.save(job);
        }
    }

    public void processJob(AiIndexJob job) {
        job.setStatus("PROCESSING");
        indexJobRepository.save(job);

        try {
            switch (job.getTargetType()) {
                case "PRODUCT":
                    indexProduct(job.getTargetId());
                    break;
                case "INVENTORY":
                    indexInventory(job.getTargetId());
                    break;
                case "RULE":
                    indexRule(job.getTargetId());
                    break;
            }
            job.setStatus("COMPLETED");
        } catch (Exception ex) {
            log.error("Failed to process index job: {}", job.getId(), ex);
            job.setStatus("FAILED");
            job.setRetryCount(job.getRetryCount() + 1);
            job.setLastErrorMessage(ex.getMessage());
        }
        
        job.setUpdatedAt(LocalDateTime.now());
        indexJobRepository.save(job);
    }

    private void indexProduct(String productId) {
        // 1. Get product data
        var productResponse = productClient.getProduct(productId);
        if (!productResponse.isSuccess() || productResponse.getData() == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm: " + productId);
        }

        ProductClient.ProductDetail product = productResponse.getData();
        
        // 2. Generate vector embeddings from product description, style, etc.
        // TODO: Call embedding model (OpenAI text-embedding-ada-002 or similar)
        String embeddingText = buildEmbeddingText(product);
        float[] vector = generateEmbedding(embeddingText);
        
        // 3. Store in Qdrant
        // TODO: Implement QdrantClient.upsert(productId, vector, payload)
        
        // 4. Update Neo4j Graph
        // TODO: Create/update Product node with properties and relationships
        
        log.info("Indexed product: {}", productId);
    }

    private void indexInventory(String sku) {
        var inventoryResponse = inventoryClient.getInventory(sku);
        if (!inventoryResponse.isSuccess() || inventoryResponse.getData() == null) {
            throw new BusinessException("INVENTORY_NOT_FOUND", "Không tìm thấy tồn kho: " + sku);
        }

        // Update availability in Qdrant/Graph for filtering
        log.info("Indexed inventory: {}", sku);
    }

    private void indexRule(String ruleId) {
        // Index fashion rules to Neo4j
        log.info("Indexed rule: {}", ruleId);
    }

    private String buildEmbeddingText(ProductClient.ProductDetail product) {
        StringBuilder sb = new StringBuilder();
        sb.append(product.getName()).append(" ");
        if (product.getDescription() != null) sb.append(product.getDescription()).append(" ");
        if (product.getAestheticStyle() != null) sb.append("Phong cách: ").append(product.getAestheticStyle()).append(" ");
        if (product.getTargetDemographic() != null) sb.append("Đối tượng: ").append(product.getTargetDemographic()).append(" ");
        if (product.getSeasonalProperty() != null) sb.append("Mùa: ").append(product.getSeasonalProperty()).append(" ");
        if (product.getVariants() != null) {
            for (var v : product.getVariants()) {
                sb.append("Màu: ").append(v.getColor()).append(" ");
                sb.append("Size: ").append(v.getSize()).append(" ");
                if (v.getMaterial() != null) sb.append("Chất liệu: ").append(v.getMaterial()).append(" ");
            }
        }
        return sb.toString();
    }

    private float[] generateEmbedding(String text) {
        // TODO: Call external embedding API (OpenAI, Vertex AI, etc.)
        // For now, return dummy vector
        return new float[768]; // text-embedding-ada-002 dimension
    }

    public List<AiIndexJob> getIndexJobs(String status) {
        if (status != null && !status.isBlank()) {
            return indexJobRepository.findByStatus(status);
        }
        return indexJobRepository.findAll();
    }

    public void retryJob(String jobId) {
        AiIndexJob job = indexJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("JOB_NOT_FOUND", "Không tìm thấy job"));
        
        job.setStatus("PENDING");
        job.setRetryCount(0);
        job.setLastErrorMessage(null);
        indexJobRepository.save(job);
    }
}