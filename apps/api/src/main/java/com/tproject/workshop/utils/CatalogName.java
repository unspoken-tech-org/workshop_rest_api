package com.tproject.workshop.utils;

import java.text.Normalizer;
import java.util.Locale;
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

    /**
     * Mirrors the database column {@code name_key}: {@code regexp_replace(lower(unaccent(name)),
     * '[^a-z0-9]', '', 'g')}. "Micro-ondas", "microondas" and "micro ondas" share the same key.
     */
    public static String nameKey(String raw) {
        if (raw == null) {
            return null;
        }
        String folded =
                raw.toLowerCase(Locale.ROOT)
                        .replace("ß", "ss")
                        .replace("æ", "ae")
                        .replace("œ", "oe")
                        .replace("ø", "o")
                        .replace("đ", "d")
                        .replace("ł", "l");
        return Normalizer.normalize(folded, Normalizer.Form.NFD).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Find-or-create keyed by {@link #nameKey}: any spelling of an existing entry returns the
     * existing row; a new entry is stored with the {@link #normalize normalized} name.
     */
    public static <T> T findOrCreateByKey(
            String rawName,
            Function<String, Optional<T>> finderByKey,
            ToIntFunction<String> insertIgnore,
            Runnable onCreated,
            Supplier<? extends RuntimeException> notFoundSupplier) {
        String key = nameKey(rawName);
        String normalized = normalize(rawName);
        return finderByKey
                .apply(key)
                .orElseGet(() -> {
                    int inserted = insertIgnore.applyAsInt(normalized);
                    if (inserted > 0 && onCreated != null) {
                        onCreated.run();
                    }
                    return finderByKey.apply(key).orElseThrow(notFoundSupplier);
                });
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
