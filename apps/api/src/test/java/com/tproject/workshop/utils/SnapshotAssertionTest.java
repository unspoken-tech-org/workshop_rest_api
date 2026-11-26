package com.tproject.workshop.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        AssertionFailedError error =
                assertThrows(
                        AssertionFailedError.class,
                        () ->
                                AbstractIntegrationTest.assertMatchesSnapshot(
                                        missingSnapshot,
                                        "nested/example_1.json",
                                        "{\"status\":\"ok\"}",
                                        STRICT_COMPARATOR,
                                        false));

        assertTrue(error.getMessage().contains("Missing JSON snapshot"));
        assertFalse(missingSnapshot.exists());
    }

    @Test
    void shouldWriteSnapshotWhenMissingAndUpdateIsEnabled(@TempDir Path tempDir) throws Exception {
        File newSnapshot = tempDir.resolve("nested/example_1.json").toFile();
        String payload = "{\n  \"status\" : \"ok\"\n}";

        AbstractIntegrationTest.assertMatchesSnapshot(
                newSnapshot, "nested/example_1.json", payload, STRICT_COMPARATOR, true);

        assertTrue(newSnapshot.exists());
        assertEquals(payload, Files.readString(newSnapshot.toPath(), StandardCharsets.UTF_8));
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

        assertThrows(
                AssertionError.class,
                () ->
                        AbstractIntegrationTest.assertMatchesSnapshot(
                                existingSnapshot,
                                "example_1.json",
                                "{\"status\":\"changed\"}",
                                STRICT_COMPARATOR,
                                true));
    }

    @Test
    void shouldRespectUpdateSnapshotsSystemProperty() {
        String previous = System.getProperty("updateSnapshots");
        try {
            System.setProperty("updateSnapshots", "true");
            assertTrue(AbstractIntegrationTest.isUpdateSnapshotsEnabled());

            System.setProperty("updateSnapshots", "false");
            assertFalse(AbstractIntegrationTest.isUpdateSnapshotsEnabled());
        } finally {
            if (previous == null) {
                System.clearProperty("updateSnapshots");
            } else {
                System.setProperty("updateSnapshots", previous);
            }
        }
    }
}
