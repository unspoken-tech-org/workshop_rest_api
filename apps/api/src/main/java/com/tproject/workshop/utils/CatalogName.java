package com.tproject.workshop.utils;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Shared normalization and concurrency-safe find-or-create helper for catalog entities. */
public final class CatalogName {

    private CatalogName() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    public static <T> T findOrCreate(
            String rawName,
            Function<String, Optional<T>> finder,
            ToIntFunction<String> insertIgnore,
            Runnable onCreated,
            Supplier<? extends RuntimeException> notFoundSupplier) {
        String normalized = normalize(rawName);
        return finder.apply(normalized)
                .orElseGet(() -> {
                    int inserted = insertIgnore.applyAsInt(normalized);
                    if (inserted > 0 && onCreated != null) {
                        onCreated.run();
                    }
                    return finder.apply(normalized).orElseThrow(notFoundSupplier);
                });
    }
}
