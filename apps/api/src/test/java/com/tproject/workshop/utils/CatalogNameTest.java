package com.tproject.workshop.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CatalogNameTest {

    @Test
    void normalizeShouldTrimCollapseWhitespaceAndLowercase() {
        assertThat(CatalogName.normalize("  Azul   Marinho  ")).isEqualTo("azul marinho");
        assertThat(CatalogName.normalize("IPHONE 15")).isEqualTo("iphone 15");
        assertThat(CatalogName.normalize(null)).isNull();
    }

    @Test
    void findOrCreateShouldReturnExistingWithoutInserting() {
        AtomicBoolean inserted = new AtomicBoolean(false);
        AtomicBoolean callbackRan = new AtomicBoolean(false);

        String result =
                CatalogName.findOrCreate(
                        "  Azul   Claro ",
                        name ->
                                "azul claro".equals(name)
                                        ? Optional.of("azul claro")
                                        : Optional.empty(),
                        name -> {
                            inserted.set(true);
                            return 1;
                        },
                        () -> callbackRan.set(true),
                        IllegalStateException::new);

        assertThat(result).isEqualTo("azul claro");
        assertThat(inserted).isFalse();
        assertThat(callbackRan).isFalse();
    }

    @Test
    void findOrCreateShouldInsertAndRunCallbackWhenMissing() {
        AtomicReference<String> stored = new AtomicReference<>();
        AtomicBoolean callbackRan = new AtomicBoolean(false);

        String result =
                CatalogName.findOrCreate(
                        "  Verde  Limao ",
                        name -> Optional.ofNullable(stored.get()),
                        name -> {
                            stored.set(name);
                            return 1;
                        },
                        () -> callbackRan.set(true),
                        IllegalStateException::new);

        assertThat(result).isEqualTo("verde limao");
        assertThat(callbackRan).isTrue();
    }

    @Test
    void findOrCreateShouldNotRunCallbackWhenConcurrentConflictOccurs() {
        AtomicReference<String> stored = new AtomicReference<>();
        AtomicBoolean firstLookup = new AtomicBoolean(true);
        AtomicBoolean callbackRan = new AtomicBoolean(false);

        String result =
                CatalogName.findOrCreate(
                        "Preto",
                        name -> {
                            if (firstLookup.getAndSet(false)) {
                                return Optional.empty();
                            }
                            return Optional.ofNullable(stored.get());
                        },
                        name -> {
                            // Simulate another concurrent transaction having inserted first
                            stored.set(name);
                            return 0;
                        },
                        () -> callbackRan.set(true),
                        IllegalStateException::new);

        assertThat(result).isEqualTo("preto");
        assertThat(callbackRan).isFalse();
    }

    @Test
    void findOrCreateShouldThrowWhenStillMissingAfterInsert() {
        assertThatThrownBy(() ->
                        CatalogName.findOrCreate(
                                "Inexistente",
                                name -> Optional.empty(),
                                name -> 0,
                                () -> {},
                                () -> new IllegalStateException("missing")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nameKeyShouldIgnoreCaseAccentHyphenAndSpaces() {
        assertThat(CatalogName.nameKey("Micro-ondas"))
                .isEqualTo(CatalogName.nameKey("microondas"))
                .isEqualTo(CatalogName.nameKey("  MICRO   ondas "))
                .isEqualTo("microondas");
        assertThat(CatalogName.nameKey("Forno Elétrico")).isEqualTo("fornoeletrico");
        assertThat(CatalogName.nameKey(null)).isNull();
    }

    @Test
    void findOrCreateByKeyShouldLookUpByKeyAndInsertNormalizedName() {
        AtomicReference<String> storedName = new AtomicReference<>();
        AtomicReference<String> lookedUp = new AtomicReference<>();

        String result =
                CatalogName.findOrCreateByKey(
                        "  Micro-Ondas ",
                        key -> {
                            lookedUp.set(key);
                            return Optional.ofNullable(storedName.get());
                        },
                        name -> {
                            storedName.set(name);
                            return 1;
                        },
                        null,
                        IllegalStateException::new);

        assertThat(lookedUp.get()).isEqualTo("microondas");
        assertThat(result).isEqualTo("micro-ondas");
    }
}
