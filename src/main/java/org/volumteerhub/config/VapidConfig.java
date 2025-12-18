package org.volumteerhub.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class VapidConfig {

    @Value("${vapid.public.key:}")
    private String publicKey;

    @Value("${vapid.private.key:}")
    private String privateKey;

    @Value("${vapid.subject:}")
    private String subject;

    @Getter
    @Value("${app.push-notification.enable:false}")
    private boolean isEnable;

    @PostConstruct
    public void validateAndCrashIfInvalid() {
        if (!isEnable) {
            log.info("Push notifications are disabled. VAPID validation skipped.");
            return;
        }

        log.info("Push notifications enabled. Validating VAPID credentials...");

        try {
            // 1. Check if null or empty
            if (isInvalid(publicKey) || isInvalid(privateKey) || isInvalid(subject)) {
                throw new IllegalArgumentException("Missing VAPID properties (publicKey, privateKey, subject).");
            }

            // 2. Check for default placeholder values
            if (publicKey.equals("VAPID_PUBLIC_KEY") || privateKey.equals("VAPID_PRIVATE_KEY")) {
                throw new IllegalArgumentException("VAPID keys are still using default placeholder values.");
            }

            // 3. Check subject format
            Pattern pattern = Pattern.compile("^mailto:[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
            if (!pattern.matcher(subject).matches()) {
                throw new IllegalArgumentException("Invalid subject. Subject must be in format 'mailto:user@example.com'");
            }

            // 4. Validate Key Pair Math
            try {
                validateKeyPair(publicKey, privateKey);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid VAPID key pair.");
            }

            log.info("VAPID configuration validated successfully.");

        } catch (Exception e) {
            log.error("VAPID configuration is invalid. Application shutting down.");
            log.error("Reason: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getPublicKey() {
        ensureEnabled();
        return publicKey;
    }

    public String getPrivateKey() {
        ensureEnabled();
        return privateKey;
    }

    public String getSubject() {
        ensureEnabled();
        return subject;
    }

    public boolean isEnabled() {
        return isEnable;
    }

    /**
     * Validates raw VAPID keys (Uncompressed EC points).
     * Public key: 65 bytes (starts with 0x04)
     * Private key: 32 bytes
     */
    private void validateKeyPair(String publicKey, String privateKey) {
        try {
            // VAPID uses URL-safe Base64 without padding
            byte[] publicBytes = Base64.getUrlDecoder().decode(publicKey);
            byte[] privateBytes = Base64.getUrlDecoder().decode(privateKey);

            // VAPID Public Key: 65 bytes starting with 0x04 (uncompressed)
            if (publicBytes.length != 65 || publicBytes[0] != 0x04) {
                throw new IllegalArgumentException("Public key must be a valid 65-byte uncompressed EC point.");
            }

            // VAPID Private Key: 32 bytes scalar
            if (privateBytes.length != 32) {
                throw new IllegalArgumentException("Private key must be exactly 32 bytes.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode VAPID keys. Ensure they are URL-safe Base64.", e);
        }
    }

    private boolean isInvalid(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void ensureEnabled() {
        if (!isEnable) {
            log.error("Access denied: Push notification fields requested while isEnable is false.");
            throw new IllegalStateException("Push notifications are disabled in configuration.");
        }
    }

}
