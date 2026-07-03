package com.stylemind.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexJobResponse {
    private String id;
    private String targetType;
    private String targetId;
    private String operationType;
    private String status;
    private Integer retryCount;
    private String lastErrorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
