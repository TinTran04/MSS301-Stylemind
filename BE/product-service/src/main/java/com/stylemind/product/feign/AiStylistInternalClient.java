package com.stylemind.product.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "ai-stylist-service", url = "${ai-stylist.service.url:http://ai-stylist-service:8000}")
public interface AiStylistInternalClient {

    @PostMapping("/internal/v1/products/{productId}/sync")
    void syncProduct(@PathVariable("productId") String productId);

    @DeleteMapping("/internal/v1/products/{productId}")
    void deleteProduct(@PathVariable("productId") String productId);
}
