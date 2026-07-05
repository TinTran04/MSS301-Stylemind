package com.stylemind.product.service.image;

import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorage {
    StoredProductImage upload(String productId, MultipartFile file) throws Exception;

    void delete(String publicId) throws Exception;
}
