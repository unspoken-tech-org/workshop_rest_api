package com.tproject.workshop.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported platforms for API Keys.
 * Each platform has a unique prefix for easy identification.
 */
@Getter
@RequiredArgsConstructor
public enum Platform {
    
    MOBILE("sk_mobile_", "Mobile application (Android/iOS)"),
    WEB("sk_web_", "Web application (browser)"),
    DESKTOP("sk_desktop_", "Desktop application (Windows/Mac/Linux)"),
    SERVER("sk_server_", "Server/Backend (service-to-service communication)");
    
    private final String prefix;
    private final String description;
    
    /**
     * Identifies the platform based on the key prefix.
     *
     * @param keyValue the API key
     * @return the corresponding platform or null if not found
     */
    public static Platform fromKeyValue(String keyValue) {
        if (keyValue == null) return null;
        
        for (Platform platform : values()) {
            if (keyValue.startsWith(platform.getPrefix())) {
                return platform;
            }
        }
        return null;
    }
}
