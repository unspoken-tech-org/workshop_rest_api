package com.tproject.workshop.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tproject.workshop.errorhandling.ErrorCode;
import com.tproject.workshop.errorhandling.GlobalExceptionHandler;
import com.tproject.workshop.exception.DomainException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Generic {@code Idempotency-Key} support for POST endpoints. It is not a {@code @Component}: it is
 * registered by {@link IdempotencyConfig} for an explicit list of paths, after Spring Security so
 * the authenticated principal is known.
 *
 * <p>Without the header the request passes through untouched. With it, only 2xx responses are
 * stored and replayed; any other outcome releases the key so the client can retry.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private static final Pattern VALID_KEY = Pattern.compile("[\\x21-\\x7E]{1,255}");

    private final IdempotencyService service;
    private final ObjectMapper objectMapper;
    private final ObjectMapper canonicalMapper;

    public IdempotencyFilter(IdempotencyService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.canonicalMapper =
                objectMapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (key == null
                || !HttpMethod.POST.matches(request.getMethod())
                || auth == null
                || auth instanceof AnonymousAuthenticationToken
                || !auth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        if (!VALID_KEY.matcher(key).matches()) {
            writeProblem(
                    response,
                    ErrorCode.BAD_REQUEST,
                    "Idempotency-Key inválida: use de 1 a 255 caracteres ASCII visíveis.");
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        String scope = auth.getName() + "|POST " + request.getRequestURI();

        IdempotencyService.Outcome outcome;
        try {
            outcome = service.begin(scope, key, hash(body));
        } catch (DomainException ex) {
            writeProblem(response, ex.getErrorCode(), ex.getMessage());
            return;
        }

        if (outcome instanceof IdempotencyService.Replay replay) {
            writeReplay(response, replay.response());
            return;
        }

        long id = ((IdempotencyService.Proceed) outcome).id();
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        boolean stored = false;
        try {
            chain.doFilter(new CachedBodyRequest(request, body), wrapper);
            int status = wrapper.getStatus();
            if (status >= 200 && status < 300) {
                service.complete(
                        id,
                        new StoredResponse(
                                status,
                                wrapper.getContentType(),
                                new String(
                                        wrapper.getContentAsByteArray(), StandardCharsets.UTF_8)));
                stored = true;
            }
        } finally {
            if (!stored) {
                service.release(id);
            }
        }
        wrapper.copyBodyToResponse();
    }

    private void writeReplay(HttpServletResponse response, StoredResponse stored)
            throws IOException {
        response.setStatus(stored.status());
        if (stored.contentType() != null) {
            response.setContentType(stored.contentType());
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(REPLAYED_HEADER, "true");
        response.getOutputStream().write(stored.body().getBytes(StandardCharsets.UTF_8));
    }

    private void writeProblem(HttpServletResponse response, ErrorCode code, String message)
            throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(), GlobalExceptionHandler.toProblemDetail(code, message));
    }

    /** SHA-256 of the JSON with sorted keys, so formatting and key order do not matter. */
    private String hash(byte[] body) {
        byte[] canonical = body;
        try {
            Object tree = canonicalMapper.readValue(body, Object.class);
            canonical = canonicalMapper.writeValueAsBytes(tree);
        } catch (IOException e) {
            // Not JSON: hash the raw bytes; the controller will reject it anyway.
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return in.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            Charset charset =
                    Optional.ofNullable(getCharacterEncoding())
                            .map(Charset::forName)
                            .orElse(StandardCharsets.UTF_8);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
