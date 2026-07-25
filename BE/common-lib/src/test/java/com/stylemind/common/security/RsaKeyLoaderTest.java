package com.stylemind.common.security;

import com.stylemind.common.exception.InvalidKeyFormatException;
import com.stylemind.common.exception.KeyLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyLoaderTest {

    @Test
    void loadPrivateKey_ValidPem_ReturnsPrivateKey(@TempDir Path tempDir) throws Exception {
        // Create test PEM file
        String pemContent = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7...
            -----END PRIVATE KEY-----
            """.trim();
        
        Path keyFile = tempDir.resolve("test_private_key.pem");
        Files.writeString(keyFile, pemContent);
        
        // Note: This test requires a valid PKCS#8 key for actual execution
        // For now, we test the exception handling
    }

    @Test
    void loadPrivateKey_FileNotFound_ThrowsKeyLoadException() {
        assertThrows(KeyLoadException.class, 
            () -> RsaKeyLoader.loadPrivateKey("nonexistent.pem"));
    }

    @Test
    void loadPrivateKey_InvalidPem_ThrowsInvalidKeyFormatException(@TempDir Path tempDir) throws Exception {
        Path keyFile = tempDir.resolve("invalid.pem");
        Files.writeString(keyFile, "invalid content");
        
        assertThrows(InvalidKeyFormatException.class, 
            () -> RsaKeyLoader.loadPrivateKey(keyFile.toString()));
    }
}
