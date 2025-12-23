package com.tproject.workshop.config.logging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
@Order(1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("http.access");
    
    private static final int MAX_BODY_CHARS = 4000;
    
    // Fields that will be masked in the log (case-insensitive comparison)
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "senha", "secret",
            "token", "accessToken", "refreshToken", "authorization", "apiKey",
            "cpf", "cnpj", "rg",
            "email", "phone", "telefone", "celular",
            "creditCard", "cardNumber", "cvv", "cvc",
            "accountNumber", "routingNumber"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Instant start = Instant.now();

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            logRequest(requestWrapper, responseWrapper, durationMs);
            
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            long durationMs) {

        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = (query == null) ? path : path + "?" + query;
        int status = response.getStatus();

        MDC.put("httpMethod", method);
        MDC.put("httpPath", path);
        MDC.put("httpStatus", String.valueOf(status));
        MDC.put("durationMs", String.valueOf(durationMs));

        try {
            boolean isError = status >= 400;
            boolean shouldLogBody = isError || LOG.isDebugEnabled();

            if (!shouldLogBody) {
                // Log for successful requests
                LOG.info("HTTP {} {} -> {} ({}ms)", method, fullPath, status, durationMs);
            } else {
                // Log detailed for errors or when DEBUG is enabled
                String requestBody = extractBody(
                        request.getContentAsByteArray(),
                        request.getCharacterEncoding(),
                        request.getContentType()
                );
                String sanitizedBody = sanitizeJson(requestBody);

                String bodySuffix = formatBodySuffix(sanitizedBody);

                if (isError) {
                    LOG.warn("HTTP {} {} -> {} ({}ms){}", method, fullPath, status, durationMs, bodySuffix);
                } else {
                    LOG.debug("HTTP {} {} -> {} ({}ms){}", method, fullPath, status, durationMs, bodySuffix);
                }
            }
        } finally {
            // Clear extra MDC
            MDC.remove("httpMethod");
            MDC.remove("httpPath");
            MDC.remove("httpStatus");
            MDC.remove("durationMs");
        }
    }

    private String extractBody(byte[] content, String encoding, String contentType) {
        if (content == null || content.length == 0) {
            return "";
        }

        if (!isTextualContent(contentType)) {
            return "[binary:" + contentType + ", bytes=" + content.length + "]";
        }

        Charset charset = determineCharset(encoding);
        String body = new String(content, charset);
        return truncate(body);
    }

    private boolean isTextualContent(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true;
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.includes(mediaType)
                    || MediaType.APPLICATION_XML.includes(mediaType)
                    || MediaType.TEXT_PLAIN.includes(mediaType)
                    || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)
                    || mediaType.getType().equalsIgnoreCase("text");
        } catch (Exception e) {
            return true;
        }
    }

    private Charset determineCharset(String encoding) {
        if (StringUtils.hasText(encoding)) {
            try {
                return Charset.forName(encoding);
            } catch (Exception ignored) {
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String truncate(String value) {
        if (value == null) return "";
        if (value.length() <= MAX_BODY_CHARS) return value;
        return value.substring(0, MAX_BODY_CHARS) + "...(truncated, total=" + value.length() + ")";
    }

    private String formatBodySuffix(String body) {
        return (body == null || body.isBlank()) ? "" : " | requestBody=" + body;
    }

    /**
     * Masks sensitive fields in JSON.
     * If not valid JSON, returns the original truncated value.
     */
    private String sanitizeJson(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        String trimmed = body.trim();
        
        try {
            if (trimmed.startsWith("{")) {
                Map<String, Object> map = objectMapper.readValue(trimmed, new TypeReference<>() {});
                maskSensitiveFields(map);
                return objectMapper.writeValueAsString(map);
            } else if (trimmed.startsWith("[")) {
                List<Object> list = objectMapper.readValue(trimmed, new TypeReference<>() {});
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        maskSensitiveFields(map);
                    }
                }
                return objectMapper.writeValueAsString(list);
            }
        } catch (Exception e) {}

        return truncate(body);
    }

    @SuppressWarnings("unchecked")
    private void maskSensitiveFields(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Check if the key is sensitive (case-insensitive)
            boolean isSensitive = SENSITIVE_KEYS.stream()
                    .anyMatch(sensitive -> sensitive.equalsIgnoreCase(key));

            if (isSensitive && value != null) {
                entry.setValue("***MASKED***");
                continue;
            }

            // Recursively processes nested objects
            if (value instanceof Map) {
                maskSensitiveFields((Map<String, Object>) value);
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        maskSensitiveFields((Map<String, Object>) item);
                    }
                }
            }
        }
    }
}

