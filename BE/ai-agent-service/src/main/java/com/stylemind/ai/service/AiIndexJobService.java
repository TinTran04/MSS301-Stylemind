package com.stylemind.ai.service;

import com.stylemind.ai.dto.*;
import com.stylemind.ai.entity.*;
import com.stylemind.ai.repository.*;
import com.stylemind.ai.feign.ProductClient;
import com.stylemind.common.constant.ErrorCode;
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
                    indexMockTarget("INVENTORY", job.getTargetId());
                    break;
                case "RULE":
                    indexMockTarget("RULE", job.getTargetId());
                    break;
                default:
                    indexMockTarget(job.getTargetType(), job.getTargetId());
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
        // Mock implementation
    }

    private void indexMockTarget(String targetType, String targetId) {
        log.debug("Mock AI index for targetType={}, targetId={}", targetType, targetId);
    }

    private String buildEmbeddingText(ProductClient.ProductDetail product) {
        return "";
    }

    private float[] generateEmbedding(String text) {
        return new float[768];
    }

    public List<AiIndexJob> getIndexJobs(String status) {
        if (status != null && !status.isBlank()) {
            return indexJobRepository.findByStatus(status);
        }
        return indexJobRepository.findAll();
    }

    public void retryJob(String jobId) {
        AiIndexJob job = indexJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        
        job.setStatus("PENDING");
        job.setRetryCount(0);
        job.setLastErrorMessage(null);
        indexJobRepository.save(job);
    }
}
