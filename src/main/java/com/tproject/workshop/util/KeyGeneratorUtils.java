package com.tproject.workshop.util;

import com.tproject.workshop.model.Platform;
import java.security.SecureRandom;
import java.util.Base64;

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
}