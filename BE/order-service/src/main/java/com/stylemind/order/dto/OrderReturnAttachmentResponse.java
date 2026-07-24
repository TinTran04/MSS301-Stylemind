package com.stylemind.order.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnAttachmentResponse {
    private String id;
    private String returnRequestId;
    private String orderId;
    private String owner;
    private String kind;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String imageDataUrl;
    private Instant uploadedAt;
}
