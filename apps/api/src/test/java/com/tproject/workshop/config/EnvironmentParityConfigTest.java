package com.tproject.workshop.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.config.security.CorsProperties;
import com.tproject.workshop.integration.support.TestcontainersConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

/** Guards environment parity across Spring profiles, Compose files, and Postgres versions. */
class EnvironmentParityConfigTest {

    private static final Pattern PGBACKREST_BASE_IMAGE =
            Pattern.compile("^FROM postgres:(\\d+\\.\\d+)-.+@sha256:[0-9a-f]{64}$");

    private static final Path GATEWAY_CADDY_DIR =
            Path.of("..", "..", "platform", "gateway", "caddy");

    /** Documented QA x PRD differences in the gateway Caddyfiles (host names and upstreams). */
    private static final Map<String, String> CADDYFILE_PRD_TO_QA =
            Map.of(
                    "api.eletroluk.com", "api-qa.eletroluk.com",
                    "workshop-api:8080", "workshop-api-qa:8080",
                    "@api_prod", "@api_qa",
                    "\"Gateway: Invalid Host Header\"", "\"Gateway QA: Invalid Host Header\"");

    @Test
    void shouldFailClosedOnCorsInQaAndProduction() throws IOException {
        for (String profileConfig : List.of("application-qa.yml", "application-production.yml")) {
            CorsProperties cors = bindCors(profileConfig);

            assertThat(cors.getAllowedOrigins()).as("cors origins in %s", profileConfig).isEmpty();
            assertThat(cors.isAllowCredentials())
                    .as("cors credentials in %s", profileConfig)
                    .isFalse();
            assertThat(cors.getAllowedHeaders())
                    .as("cors headers in %s", profileConfig)
                    .contains("X-API-Key")
                    .doesNotContain("*");
        }
    }

    @Test
    void shouldKeepExplicitCorsOriginsInLocalAndTest() throws IOException {
        assertThat(bindCors("application-local.yml").getAllowedOrigins())
                .contains("http://localhost:3000");
        assertThat(bindCors("application-test.yml").getAllowedOrigins())
                .contains("http://localhost:8081", "http://127.0.0.1:8081");
    }

    @Test
    void shouldKeepGatewayCaddyfilesInParityExceptDocumentedDifferences() throws IOException {
        String qa =
                normalizeCaddyfile(
                        Files.readString(GATEWAY_CADDY_DIR.resolve("Caddyfile-gateway-qa")));
        String prd =
                normalizeCaddyfile(
                        Files.readString(GATEWAY_CADDY_DIR.resolve("Caddyfile-gateway")));
        for (Map.Entry<String, String> difference : CADDYFILE_PRD_TO_QA.entrySet()) {
            prd = prd.replace(difference.getKey(), difference.getValue());
        }

        assertThat(prd).isEqualTo(qa);
    }

    @Test
    void shouldHardenGatewayCaddyfiles() throws IOException {
        for (String caddyfile : List.of("Caddyfile-gateway-qa", "Caddyfile-gateway")) {
            String config =
                    normalizeCaddyfile(Files.readString(GATEWAY_CADDY_DIR.resolve(caddyfile)));

            // Only the Docker ingress network (cloudflared) is a trusted proxy: never the LAN.
            assertThat(config)
                    .as(caddyfile)
                    .contains(
                            "trusted_proxies static 172.16.0.0/12\n",
                            "trusted_proxies_strict\n",
                            "client_ip_headers Cf-Connecting-Ip X-Forwarded-For\n")
                    .doesNotContain("192.168.0.0/16", "10.0.0.0/8");
            assertThat(config)
                    .as(caddyfile)
                    .contains(
                            "Strict-Transport-Security \"max-age=31536000; includeSubDomains\"",
                            "Referrer-Policy \"no-referrer\"",
                            "-Server",
                            "max_size 1MB",
                            "SecRuleEngine DetectionOnly",
                            "SecAuditLogParts AHZ",
                            "SecAuditLogRelevantStatus \"^5\"",
                            "SecRuleUpdateTargetByTag OWASP_CRS \"!REQUEST_HEADERS:Authorization|!REQUEST_HEADERS:X-API-Key\"",
                            "\"status\":429,\"title\":",
                            "\"status\":413,\"title\":");
            // Each site that proxies the API answers Caddy's own errors (429/413) as JSON.
            assertThat(countOccurrences(config, "import error_responses"))
                    .as(caddyfile)
                    .isEqualTo(countOccurrences(config, "import api_proxy"));
            // Within route the written order is the execution order.
            assertThat(config.indexOf("rate_limit {"))
                    .as(caddyfile)
                    .isLessThan(config.indexOf("request_body {"));
            assertThat(config.indexOf("request_body {"))
                    .as(caddyfile)
                    .isLessThan(config.indexOf("coraza_waf {"));
            assertThat(config.indexOf("coraza_waf {"))
                    .as(caddyfile)
                    .isLessThan(config.indexOf("reverse_proxy {args[0]}"));
        }
    }

    @Test
    void shouldInheritDdlAutoValidateAcrossProfiles() {
        assertThat(
                        loadYamlProperties("application.yml")
                                .getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");
        assertThat(
                        loadYamlProperties("application-test.yml")
                                .getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");

        for (String profileConfig :
                List.of(
                        "application-local.yml",
                        "application-qa.yml",
                        "application-production.yml",
                        "application-production-local.yml")) {
            assertThat(
                            loadYamlProperties(profileConfig)
                                    .getProperty("spring.jpa.hibernate.ddl-auto"))
                    .as("ddl-auto override in %s", profileConfig)
                    .isNull();
        }
    }

    @Test
    void shouldKeepTestOnlyConfigAndDevKeysOutOfPackagedJar() throws IOException {
        // application-test.yml carries a test bcrypt hash: it must live only in test resources.
        assertThat(Path.of("src", "main", "resources", "application-test.yml")).doesNotExist();
        assertThat(Path.of("src", "test", "resources", "application-test.yml")).isRegularFile();
        // Only the test resources copy is on the classpath (no stale copy from main).
        assertThat(
                        Collections.list(
                                Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResources("application-test.yml")))
                .singleElement()
                .satisfies(url -> assertThat(url.getPath()).contains("/test/"));

        String buildGradle = Files.readString(Path.of("build.gradle"));
        assertThat(buildGradle)
                .containsPattern("tasks\\.named\\('bootJar'\\)\\s*\\{\\s*exclude 'keys/\\*\\*'")
                .containsPattern("tasks\\.named\\('jar'\\)\\s*\\{\\s*exclude 'keys/\\*\\*'");
    }

    @Test
    void shouldNotDuplicateJavaEntrypointCommandInComposeFiles() throws IOException {
        String dockerfile = Files.readString(Path.of("docker", "Dockerfile"));
        assertThat(dockerfile).contains("ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]");

        assertComposeServiceHasNoCommand(
                Path.of("compose", "production.yml"), "workshop_spring_app", "production");
        assertComposeServiceHasNoCommand(
                Path.of("compose", "qa.yml"), "workshop_spring_app_qa", "qa");
    }

    @Test
    void shouldKeepApiOffTheIngressNetworkAndHardenedInQaAndProduction() throws IOException {
        Map<Object, Object> production = loadYamlMap(Path.of("compose", "production.yml"));
        Map<Object, Object> qa = loadYamlMap(Path.of("compose", "qa.yml"));
        Map<Object, Object> productionApi = service(production, "workshop_spring_app");
        Map<Object, Object> qaApi = service(qa, "workshop_spring_app_qa");

        for (Map<Object, Object> api : List.of(productionApi, qaApi)) {
            // Only the Caddy reaches the API (workshop-app); cloudflared stays on workshop-ingress.
            assertThat(api.get("networks"))
                    .isEqualTo(List.of("workshop-network", "workshop-app", "workshop-metrics"));
            assertHardened(api);
        }
        assertThat(productionApi.get("deploy")).isEqualTo(qaApi.get("deploy"));
        assertThat(productionApi.get("tmpfs")).isEqualTo(qaApi.get("tmpfs"));

        for (Map<Object, Object> compose : List.of(production, qa)) {
            Map<Object, Object> networks = asMap(compose.get("networks"));
            assertThat(networks).doesNotContainKey("workshop-ingress");
            assertInternalAppNetwork(networks);
        }
    }

    @Test
    void shouldHardenGatewayContainersEquallyInQaAndProduction() throws IOException {
        Path gatewayCompose = Path.of("..", "..", "platform", "gateway", "compose");
        Map<Object, Object> production = loadYamlMap(gatewayCompose.resolve("production.yml"));
        Map<Object, Object> qa = loadYamlMap(gatewayCompose.resolve("qa.yml"));
        Map<Object, Object> productionCaddy = service(production, "caddy-gateway");
        Map<Object, Object> qaCaddy = service(qa, "caddy-gateway-qa");
        Map<Object, Object> productionTunnel = service(production, "cloudflared-prod");
        Map<Object, Object> qaTunnel = service(qa, "cloudflared-qa");

        for (Map<Object, Object> caddy : List.of(productionCaddy, qaCaddy)) {
            assertHardened(caddy);
            assertThat(caddy.get("cap_add")).isEqualTo(List.of("NET_BIND_SERVICE"));
            assertThat(caddy.get("networks"))
                    .isEqualTo(List.of("workshop-ingress", "workshop-app", "workshop-metrics"));
            assertThat(caddy.get("ports"))
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .contains("127.0.0.1:80:80");
            assertThat(caddy.get("volumes"))
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .filteredOn(volume -> volume.toString().contains("/etc/caddy/Caddyfile"))
                    .singleElement()
                    .asString()
                    .endsWith(":ro");
        }
        for (Map<Object, Object> tunnel : List.of(productionTunnel, qaTunnel)) {
            assertHardened(tunnel);
            assertThat(tunnel.get("networks")).isEqualTo(List.of("workshop-ingress"));
        }
        assertThat(productionCaddy.get("deploy")).isEqualTo(qaCaddy.get("deploy"));
        assertThat(productionCaddy.get("tmpfs")).isEqualTo(qaCaddy.get("tmpfs"));
        assertThat(productionTunnel.get("deploy")).isEqualTo(qaTunnel.get("deploy"));

        for (Map<Object, Object> compose : List.of(production, qa)) {
            assertInternalAppNetwork(asMap(compose.get("networks")));
        }
    }

    @Test
    void shouldAlignLocalAndTestPostgresImageWithProductionMinorVersion() throws IOException {
        String baseLine =
                Files.readAllLines(Path.of("docker", "Dockerfile.pgbackrest")).stream()
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
        assertThat(TestcontainersConfig.POSTGRES_IMAGE).isEqualTo(localImage);
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

    private static Map<Object, Object> service(Map<Object, Object> compose, String name) {
        return asMap(asMap(compose.get("services")).get(name));
    }

    /** Runtime hardening shared by the API, Caddy and cloudflared containers. */
    private static void assertHardened(Map<Object, Object> service) {
        assertThat(service.get("read_only")).isEqualTo(true);
        assertThat(service.get("cap_drop")).isEqualTo(List.of("ALL"));
        assertThat(service.get("security_opt"))
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .contains("no-new-privileges:true");
        Map<Object, Object> limits =
                asMap(asMap(asMap(service.get("deploy")).get("resources")).get("limits"));
        assertThat(limits).containsKeys("memory", "pids");
    }

    private static void assertInternalAppNetwork(Map<Object, Object> networks) {
        Map<Object, Object> app = asMap(networks.get("workshop-app"));
        assertThat(app.get("name")).isEqualTo("workshop-app");
        assertThat(app.get("internal")).isEqualTo(true);
        assertThat(app).doesNotContainKey("external");
    }

    private static CorsProperties bindCors(String classpathResource) throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader()
                        .load(classpathResource, new ClassPathResource(classpathResource));
        return new Binder(ConfigurationPropertySources.from(sources))
                .bindOrCreate("cors", CorsProperties.class);
    }

    /** Drops comments, blank lines and indentation so only directives are compared. */
    private static String normalizeCaddyfile(String content) {
        return content.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private static int countOccurrences(String content, String token) {
        return content.split(Pattern.quote(token), -1).length - 1;
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
