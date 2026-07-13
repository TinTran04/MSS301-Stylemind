package com.stylemind.common.security;

import com.stylemind.common.exception.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyLoader {

    private RsaKeyLoader() {
        // Utility class - prevent instantiation
    }

    public static PrivateKey loadPrivateKey(String keyPath) {
        try {
            String pemContent = Files.readString(Paths.get(keyPath));
            String base64Content = stripPemHeaders(pemContent, "PRIVATE KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64Content);
            
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePrivate(keySpec);
            
        } catch (IOException e) {
            throw new KeyLoadException("Failed to read private key file: " + keyPath, e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyFormatException("Invalid PKCS#8 private key format", e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyDecodingException("RSA algorithm not available", e);
        }
    }

    public static PublicKey loadPublicKey(String keyPath) {
        try {
            String pemContent = Files.readString(Paths.get(keyPath));
            String base64Content = stripPemHeaders(pemContent, "PUBLIC KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64Content);
            
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePublic(keySpec);
            
        } catch (IOException e) {
            throw new KeyLoadException("Failed to read public key file: " + keyPath, e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyFormatException("Invalid X.509 public key format", e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyDecodingException("RSA algorithm not available", e);
        }
    }

    private static String stripPemHeaders(String pemContent, String keyType) {
        String beginMarker = "-----BEGIN " + keyType + "-----";
        String endMarker = "-----END " + keyType + "-----";
        
        return pemContent
            .replace(beginMarker, "")
            .replace(endMarker, "")
            .replaceAll("\\s", "");
    }
}
