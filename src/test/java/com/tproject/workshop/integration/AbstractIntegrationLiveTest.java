package com.tproject.workshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.utils.TestFileUtils;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Tag;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.platform.commons.function.Try.success;

@ActiveProfiles("test")
@Tag("integration")
@TestPropertySource(locations = "classpath:application-test.yml")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AbstractIntegrationLiveTest {

    protected static final String TESTS_HOST = "localhost";
    protected static final String PROTOCOL_HTTP = "http://";

    protected static final int API_PORT = 8081;
    protected static final String API_KEY = TestAuthHelper.MOBILE_API_KEY;


    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    protected static final RequestSpecification SPEC = createRequestSpecification(API_PORT);

    // Lazy-initialized authenticated spec (cached after first use)
    private static RequestSpecification cachedAuthenticatedSpec;
    private static RequestSpecification cachedServiceAuthenticatedSpec;

    protected static RequestSpecification createRequestSpecification(int apiPort) {
        return new RequestSpecBuilder().setContentType(ContentType.JSON)
                .setBaseUri(PROTOCOL_HTTP + TESTS_HOST + ":" + apiPort)
                .addFilter(new ResponseLoggingFilter())
                .addFilter(new RequestLoggingFilter()).build();
    }

    /**
     * Returns a RequestSpecification with JWT authentication using the default API Key (ADMIN).
     *
     * @return RequestSpecification with JWT authentication
     */
    protected static RequestSpecification getAuthenticatedSpec() {
        if (cachedAuthenticatedSpec == null) {
            String token = TestAuthHelper.getAccessToken(SPEC, API_KEY);
            cachedAuthenticatedSpec = buildAuthenticatedSpec(token);
        }
        return cachedAuthenticatedSpec;
    }

    /**
     * Returns a RequestSpecification with JWT authentication using a SERVICE role API Key.
     *
     * @return RequestSpecification with JWT authentication
     */
    protected static RequestSpecification getServiceAuthenticatedSpec() {
        if (cachedServiceAuthenticatedSpec == null) {
            String token = TestAuthHelper.getAccessToken(SPEC, TestAuthHelper.SERVICE_API_KEY);
            cachedServiceAuthenticatedSpec = buildAuthenticatedSpec(token);
        }
        return cachedServiceAuthenticatedSpec;
    }

    private static RequestSpecification buildAuthenticatedSpec(String token) {
        return new RequestSpecBuilder().setContentType(ContentType.JSON)
                .setBaseUri(PROTOCOL_HTTP + TESTS_HOST + ":" + API_PORT)
                .addFilter(new ResponseLoggingFilter())
                .addFilter(new RequestLoggingFilter())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * Alias for getAuthenticatedSpec() to maintain compatibility with existing tests.
     *
     * @deprecated Use getAuthenticatedSpec() for clarity
     */
    protected static RequestSpecification AUTHENTICATED_SPEC() {
        return getAuthenticatedSpec();
    }

    public void validateResponse(Object index, Object response) {
        validateResponse(index, response, new DefaultComparator(JSONCompareMode.STRICT));
    }

    public void validateResponseIgnoreAttributes(Object index, Object response, List<String> ignoredAttributes) {

        validateResponse(index, response, new CustomComparator(JSONCompareMode.STRICT,
                ignoredAttributes.stream().map(attr -> new Customization(attr, (o1, o2) -> true))
                        .toArray(Customization[]::new)));
    }

    public void validateResponse(Object index, Object response, DefaultComparator comparator) {
        var className = TestFileUtils.getClassName("com.tproject.workshop.integration");
        var methodName = TestFileUtils.getMethodName("com.tproject.workshop.integration");

        String filename = String.format("%s/%s_%s.json", className, methodName, index.toString());
        String resource = String.format("src/test/resources/jsons/%s", filename);
        File testFile = new File(resource);

        try {
            String actualPayload = response instanceof Response ? ((Response) response).getBody().prettyPrint() :
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);

            if (testFile.exists()) {

                String expectedPayload =
                        (String) FileUtils.readLines(testFile, StandardCharsets.UTF_8.name()).stream()
                                .collect(Collectors.joining("\n"));

                JSONAssert.assertEquals(String.format("File %s", filename), expectedPayload, actualPayload, comparator);

            } else {
                testFile.getParentFile().mkdirs();
                Files.write(testFile.toPath(), actualPayload.getBytes());
                success("Test file did not exist. File created and test succeeded");
            }
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
