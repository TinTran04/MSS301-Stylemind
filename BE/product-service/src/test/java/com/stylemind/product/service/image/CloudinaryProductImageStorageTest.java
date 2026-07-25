package com.stylemind.product.service.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.stylemind.product.service.image.impl.CloudinaryProductImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryProductImageStorageTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryProductImageStorage storage;

    @BeforeEach
    void setUp() {
        // lenient: the isConfigured() tests below build their own storage
        // instance from a real (unmocked) Cloudinary and never call uploader().
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        storage = new CloudinaryProductImageStorage(cloudinary, "stylemind/products");
    }

    @Test
    void upload_returnsSecureUrlAndPublicId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shirt.png", "image/png", new byte[]{1, 2, 3});
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/stylemind/image/upload/shirt.png",
                "public_id", "stylemind/products/shirt"));

        StoredProductImage result = storage.upload("p1", file);

        assertEquals("https://res.cloudinary.com/stylemind/image/upload/shirt.png", result.imageUrl());
        assertEquals("stylemind/products/shirt", result.publicId());
        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), options.capture());
        assertEquals("stylemind/products", options.getValue().get("folder"));
        assertEquals("image", options.getValue().get("resource_type"));
    }

    @Test
    void delete_invalidatesCachedAsset() throws Exception {
        storage.delete("stylemind/products/shirt");

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(
                org.mockito.ArgumentMatchers.eq("stylemind/products/shirt"),
                options.capture());
        assertEquals(Boolean.TRUE, options.getValue().get("invalidate"));
        assertEquals("image", options.getValue().get("resource_type"));
    }

    @Test
    void upload_rejectsIncompleteProviderResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shirt.png", "image/png", new byte[]{1, 2, 3});
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("public_id", "stylemind/products/shirt"));

        assertThrows(IllegalStateException.class, () -> storage.upload("p1", file));
    }

    @Test
    void isConfigured_trueWhenCloudNameKeyAndSecretArePresent() {
        Cloudinary configured = new Cloudinary(Map.of(
                "cloud_name", "demo", "api_key", "123", "api_secret", "secret"));

        assertEquals(true, new CloudinaryProductImageStorage(configured, "stylemind/products").isConfigured());
    }

    @Test
    void isConfigured_falseWhenApiSecretIsBlank() {
        Cloudinary missingSecret = new Cloudinary(Map.of(
                "cloud_name", "demo", "api_key", "123", "api_secret", ""));

        assertEquals(false, new CloudinaryProductImageStorage(missingSecret, "stylemind/products").isConfigured());
    }
}
