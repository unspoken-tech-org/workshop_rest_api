package com.tproject.workshop.integration.support;

import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.utils.TestFileUtils;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestExecutionListeners(
        listeners = DatabaseCleanerTestExecutionListener.class,
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public abstract class AbstractIntegrationTest {

    protected static final String TESTS_HOST = "localhost";
    protected static final String PROTOCOL_HTTP = "http://";

    protected static final String API_KEY = TestAuthClient.MOBILE_API_KEY;

    @LocalServerPort protected int port;

    @Autowired protected JdbcTemplate jdbcTemplate;

    @Autowired private ObjectMapper objectMapper;

    protected RequestSpecification SPEC;
    protected TestAuthClient authClient;

    // Lazy-initialized authenticated spec (cached after first use)
    private RequestSpecification cachedAuthenticatedSpec;
    private RequestSpecification cachedServiceAuthenticatedSpec;

    @BeforeEach
    void setUpBaseIntegrationTest() {
        SPEC = createRequestSpecification(port);
        authClient = new TestAuthClient(SPEC);
        cachedAuthenticatedSpec = null;
        cachedServiceAuthenticatedSpec = null;
    }

    protected URI uri(String path) {
        return URI.create(PROTOCOL_HTTP + TESTS_HOST + ":" + port + path);
    }

    protected static RequestSpecification createRequestSpecification(int apiPort) {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBaseUri(PROTOCOL_HTTP + TESTS_HOST + ":" + apiPort)
                .setConfig(loggingOnFailureConfig())
                .build();
    }

    /**
     * Returns a RequestSpecification with JWT authentication using the default API Key (ADMIN).
     *
     * @return RequestSpecification with JWT authentication
     */
    protected RequestSpecification getAuthenticatedSpec() {
        if (cachedAuthenticatedSpec == null) {
            String token = authClient.getAccessToken(API_KEY);
            cachedAuthenticatedSpec = buildAuthenticatedSpec(token);
        }
        return cachedAuthenticatedSpec;
    }

    /**
     * Returns a RequestSpecification with JWT authentication using a SERVICE role API Key.
     *
     * @return RequestSpecification with JWT authentication
     */
    protected RequestSpecification getServiceAuthenticatedSpec() {
        if (cachedServiceAuthenticatedSpec == null) {
            String token = authClient.getAccessToken(TestAuthClient.SERVICE_API_KEY);
            cachedServiceAuthenticatedSpec = buildAuthenticatedSpec(token);
        }
        return cachedServiceAuthenticatedSpec;
    }

    private RequestSpecification buildAuthenticatedSpec(String token) {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBaseUri(PROTOCOL_HTTP + TESTS_HOST + ":" + port)
                .setConfig(loggingOnFailureConfig())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    private static RestAssuredConfig loggingOnFailureConfig() {
        return RestAssuredConfig.config()
                .logConfig(
                        LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
    }

    public void validateResponse(Object index, Object response) {
        validateResponse(index, response, new DefaultComparator(JSONCompareMode.STRICT));
    }

    public void validateResponseIgnoreAttributes(
            Object index, Object response, List<String> ignoredAttributes) {

        validateResponse(
                index,
                response,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        ignoredAttributes.stream()
                                .map(attr -> new Customization(attr, (o1, o2) -> true))
                                .toArray(Customization[]::new)));
    }

    public void validateResponse(Object index, Object response, DefaultComparator comparator) {
        var className = TestFileUtils.getClassName("com.tproject.workshop.integration");
        var methodName = TestFileUtils.getMethodName("com.tproject.workshop.integration");

        String filename = String.format("%s/%s_%s.json", className, methodName, index.toString());
        String resource = String.format("src/test/resources/jsons/%s", filename);
        File testFile = new File(resource);

        try {
            String actualPayload =
                    response instanceof Response
                            ? ((Response) response).getBody().prettyPrint()
                            : objectMapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(response);

            assertMatchesSnapshot(
                    testFile, filename, actualPayload, comparator, isUpdateSnapshotsEnabled());
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isUpdateSnapshotsEnabled() {
        String property = System.getProperty("updateSnapshots");
        if (property != null) {
            return Boolean.parseBoolean(property);
        }
        return Boolean.parseBoolean(System.getenv().getOrDefault("UPDATE_SNAPSHOTS", "false"));
    }

    public static void assertMatchesSnapshot(
            File testFile,
            String filename,
            String actualPayload,
            DefaultComparator comparator,
            boolean updateSnapshots)
            throws IOException, JSONException {
        if (testFile.exists()) {
            String expectedPayload =
                    FileUtils.readLines(testFile, StandardCharsets.UTF_8.name()).stream()
                            .collect(Collectors.joining("\n"));

            JSONAssert.assertEquals(
                    String.format("File %s", filename), expectedPayload, actualPayload, comparator);
            return;
        }

        if (!updateSnapshots) {
            fail(
                    String.format(
                            "Missing JSON snapshot: %s. Re-run with -DupdateSnapshots=true or UPDATE_SNAPSHOTS=true to generate it.",
                            testFile.getPath()));
        }

        File parentDir = testFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        Files.writeString(testFile.toPath(), actualPayload, StandardCharsets.UTF_8);
    }
}
