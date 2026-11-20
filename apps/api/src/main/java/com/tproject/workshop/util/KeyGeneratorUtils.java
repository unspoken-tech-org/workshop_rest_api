package com.tproject.workshop.util;

import com.tproject.workshop.model.Platform;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public class KeyGeneratorUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int ENTROPY_BYTES = 32; // 256 bits

    private KeyGeneratorUtils() {
        // Utility class
    }

    public static String generateSecureKey(Platform platform) {
        byte[] bytes = new byte[ENTROPY_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String randomPart = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return platform.getPrefix() + randomPart;
    }

    public static String sha256Hex(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value to hash must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}