package com.stylemind.product.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Slf4j
@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            Environment environment,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:stylemind/products}") String folder) {
        // Presence-only: never log the actual key/secret values. profiles/userDir
        // are diagnostic aids for the "why isn't BE/.env loading locally" case —
        // they reveal whether the local profile is active and which working
        // directory spring.config.import's relative paths were resolved against.
        log.info("Cloudinary config loaded: profiles={}, userDir={}, cloudNamePresent={}, apiKeyPresent={}, apiSecretPresent={}, folder={}",
                Arrays.toString(environment.getActiveProfiles()), System.getProperty("user.dir"),
                isPresent(cloudName), isPresent(apiKey), isPresent(apiSecret), folder);
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
