package com.stylemind.product.service.image.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.stylemind.product.service.image.ProductImageStorage;
import com.stylemind.product.service.image.StoredProductImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryProductImageStorage implements ProductImageStorage {

    private final Cloudinary cloudinary;
    private final String folder;

    public CloudinaryProductImageStorage(
            Cloudinary cloudinary,
            @Value("${cloudinary.folder:stylemind/products}") String folder) {
        this.cloudinary = cloudinary;
        this.folder = folder;
    }

    @Override
    public boolean isConfigured() {
        // Read straight off the client's own config (single source of truth)
        // rather than re-injecting the same three @Value properties here.
        return isNotBlank(cloudinary.config.cloudName)
                && isNotBlank(cloudinary.config.apiKey)
                && isNotBlank(cloudinary.config.apiSecret);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public StoredProductImage upload(String productId, MultipartFile file) throws Exception {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image",
                        "use_filename", true,
                        "unique_filename", true,
                        "overwrite", false));

        Object secureUrl = result.get("secure_url");
        Object publicId = result.get("public_id");
        if (secureUrl == null || publicId == null) {
            throw new IllegalStateException("Cloud image provider returned an incomplete upload response");
        }

        return new StoredProductImage(secureUrl.toString(), publicId.toString());
    }

    @Override
    public void delete(String publicId) throws Exception {
        cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "invalidate", true));
    }
}
