package com.tproject.workshop.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;

class SnapshotAssertionTest {

    private static final DefaultComparator STRICT_COMPARATOR =
            new DefaultComparator(JSONCompareMode.STRICT);

    @Test
    void shouldFailWhenSnapshotIsMissingAndUpdateIsDisabled(@TempDir Path tempDir) {
        File missingSnapshot = tempDir.resolve("nested/example_1.json").toFile();

        assertThatThrownBy(() ->
                        AbstractIntegrationTest.assertMatchesSnapshot(
                                missingSnapshot,
                                "nested/example_1.json",
                                "{\"status\":\"ok\"}",
                                STRICT_COMPARATOR,
                                false))
                .isInstanceOf(AssertionFailedError.class)
                .hasMessageContaining("Missing JSON snapshot");

        assertThat(missingSnapshot).doesNotExist();
    }

    @Test
    void shouldWriteSnapshotWhenMissingAndUpdateIsEnabled(@TempDir Path tempDir) throws Exception {
        File newSnapshot = tempDir.resolve("nested/example_1.json").toFile();
        String payload = "{\n  \"status\" : \"ok\"\n}";

        AbstractIntegrationTest.assertMatchesSnapshot(
                newSnapshot, "nested/example_1.json", payload, STRICT_COMPARATOR, true);

        assertThat(newSnapshot).exists();
        assertThat(Files.readString(newSnapshot.toPath(), StandardCharsets.UTF_8))
                .isEqualTo(payload);
    }

    @Test
    void shouldCompareAgainstExistingSnapshotRegardlessOfUpdateFlag(@TempDir Path tempDir)
            throws Exception {
        File existingSnapshot = tempDir.resolve("example_1.json").toFile();
        Files.writeString(existingSnapshot.toPath(), "{\"status\":\"ok\"}", StandardCharsets.UTF_8);

        AbstractIntegrationTest.assertMatchesSnapshot(
                existingSnapshot,
                "example_1.json",
                "{\"status\":\"ok\"}",
                STRICT_COMPARATOR,
                false);

        assertThatThrownBy(() ->
                        AbstractIntegrationTest.assertMatchesSnapshot(
                                existingSnapshot,
                                "example_1.json",
                                "{\"status\":\"changed\"}",
                                STRICT_COMPARATOR,
                                true))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldRespectUpdateSnapshotsSystemProperty() {
        String previous = System.getProperty("updateSnapshots");
        try {
            System.setProperty("updateSnapshots", "true");
            assertThat(AbstractIntegrationTest.isUpdateSnapshotsEnabled()).isTrue();

            System.setProperty("updateSnapshots", "false");
            assertThat(AbstractIntegrationTest.isUpdateSnapshotsEnabled()).isFalse();
        } finally {
            if (previous == null) {
                System.clearProperty("updateSnapshots");
            } else {
                System.setProperty("updateSnapshots", previous);
            }
        }
    }
}
