package com.stylemind.product.service.image;

import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorage {
    // Lets callers fail fast with a clear error when the provider has no
    // credentials, instead of attempting the call and getting an opaque
    // provider exception (e.g. Cloudinary rejecting a blank api_secret).
    boolean isConfigured();

    StoredProductImage upload(String productId, MultipartFile file) throws Exception;

    void delete(String publicId) throws Exception;
}
