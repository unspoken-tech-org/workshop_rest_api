package com.tproject.workshop.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards environment parity across Spring profiles, Compose files, and Postgres versions. */
class EnvironmentParityConfigTest {

    private static final Pattern PGBACKREST_BASE_IMAGE =
            Pattern.compile("^FROM postgres:(\\d+\\.\\d+)-.+@sha256:[0-9a-f]{64}$");

    @Test
    void shouldInheritDdlAutoValidateAcrossProfiles() {
        assertThat(loadYamlProperties("application.yml").getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");
        assertThat(loadYamlProperties("application-test.yml").getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");

        for (String profileConfig : List.of(
                "application-local.yml",
                "application-qa.yml",
                "application-production.yml",
                "application-production-local.yml")) {
            assertThat(loadYamlProperties(profileConfig).getProperty("spring.jpa.hibernate.ddl-auto"))
                    .as("ddl-auto override in %s", profileConfig)
                    .isNull();
        }
    }

    @Test
    void shouldNotDuplicateJavaEntrypointCommandInComposeFiles() throws IOException {
        String dockerfile = Files.readString(Path.of("docker", "Dockerfile"));
        assertThat(dockerfile).contains("ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]");

        assertComposeServiceHasNoCommand(Path.of("compose", "production.yml"), "workshop_spring_app", "production");
        assertComposeServiceHasNoCommand(Path.of("compose", "qa.yml"), "workshop_spring_app_qa", "qa");
    }

    @Test
    void shouldAlignLocalAndTestPostgresImageWithProductionMinorVersion() throws IOException {
        String baseLine = Files.readAllLines(Path.of("docker", "Dockerfile.pgbackrest")).stream()
                .filter(line -> line.startsWith("FROM postgres:"))
                .findFirst()
                .orElseThrow();

        Matcher matcher = PGBACKREST_BASE_IMAGE.matcher(baseLine);
        assertThat(matcher.matches()).as("Dockerfile.pgbackrest base image: %s", baseLine).isTrue();
        String pgMinorVersion = matcher.group(1);

        Map<Object, Object> localCompose = loadYamlMap(Path.of("compose", "local.yml"));
        Map<Object, Object> services = asMap(localCompose.get("services"));
        Map<Object, Object> dbService = asMap(services.get("workshop-db"));
        Object imageObj = dbService.get("image");
        assertThat(imageObj).isInstanceOf(String.class);

        String localImage = (String) imageObj;
        assertThat(localImage)
                .matches("^postgres:" + Pattern.quote(pgMinorVersion) + "-.+@sha256:[0-9a-f]{64}$");
        assertThat(TestcontainersConfiguration.POSTGRES_IMAGE).isEqualTo(localImage);
    }

    private static void assertComposeServiceHasNoCommand(
            Path composePath, String serviceName, String expectedProfile) throws IOException {
        Map<Object, Object> compose = loadYamlMap(composePath);
        Map<Object, Object> services = asMap(compose.get("services"));
        Map<Object, Object> service = asMap(services.get(serviceName));
        assertThat(service).doesNotContainKey("command");

        Map<Object, Object> environment = asMap(service.get("environment"));
        assertThat(environment.get("SPRING_PROFILES_ACTIVE")).isEqualTo(expectedProfile);
    }

    private static Properties loadYamlProperties(String classpathResource) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(classpathResource));
        Properties props = yaml.getObject();
        assertThat(props).isNotNull();
        return props;
    }

    private static Map<Object, Object> loadYamlMap(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            Object loaded = new Yaml().load(reader);
            return asMap(loaded);
        }
    }

    private static Map<Object, Object> asMap(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        Map<?, ?> raw = (Map<?, ?>) value;
        Map<Object, Object> result = new LinkedHashMap<>(raw.size());
        result.putAll(raw);
        return result;
    }
}
