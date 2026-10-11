package com.tproject.workshop.utils;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class LoadResourceUtil {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private static final ResourceLoader LOADER = new DefaultResourceLoader();

    private LoadResourceUtil() {
    }

    public static String getResource(String resourcePath) {
        synchronized (LoadResourceUtil.class) {
            if (!CACHE.containsKey(resourcePath)) {
                try (var rd = new InputStreamReader(LOADER.getResource(resourcePath).getInputStream(), UTF_8)) {
                    CACHE.put(resourcePath, FileCopyUtils.copyToString(rd));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return CACHE.get(resourcePath);
        }
    }
}
