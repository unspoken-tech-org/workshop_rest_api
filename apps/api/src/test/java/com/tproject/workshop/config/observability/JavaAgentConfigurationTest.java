package com.tproject.workshop.config.observability;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the versioned Java Agent configuration (the agent ignores Spring YAML). */
class JavaAgentConfigurationTest {

    private static final Path AGENT_CONFIG = Path.of("otel", "javaagent.properties");

    @Test
    void shouldExportOnlyTracesThroughTheAgent() throws IOException {
        Properties config = load();

        assertThat(config.getProperty("otel.traces.exporter")).isEqualTo("otlp");
        assertThat(config.getProperty("otel.metrics.exporter")).isEqualTo("none");
        assertThat(config.getProperty("otel.logs.exporter")).isEqualTo("none");
    }

    @Test
    void shouldNotCaptureHttpHeadersOrPinEnvironmentSpecificValues() throws IOException {
        Properties config = load();

        assertThat(config.getProperty("otel.instrumentation.http.server.capture-request-headers")).isEmpty();
        assertThat(config.getProperty("otel.instrumentation.http.client.capture-request-headers")).isEmpty();
        assertThat(config.stringPropertyNames())
                .doesNotContain("otel.exporter.otlp.endpoint", "otel.exporter.otlp.headers", "otel.service.instance.id");
    }

    private static Properties load() throws IOException {
        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(AGENT_CONFIG)) {
            config.load(reader);
        }
        return config;
    }
}
