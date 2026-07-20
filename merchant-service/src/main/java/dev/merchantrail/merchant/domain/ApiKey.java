package dev.merchantrail.merchant.domain;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for generating secure API keys.
 */
public class ApiKey {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int KEY_LENGTH = 32; // 32 bytes = 256 bits
    
    public static String generate() {
        byte[] keyBytes = new byte[KEY_LENGTH];
        RANDOM.nextBytes(keyBytes);
        return "mrk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }
}
