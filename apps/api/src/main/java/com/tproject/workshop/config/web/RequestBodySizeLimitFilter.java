package com.tproject.workshop.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.config.logging.RequestLogSanitizer;
import com.tproject.workshop.errorhandling.GlobalExceptionHandler;
import com.tproject.workshop.exception.PayloadTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps request bodies (JSON included) even when the API is reached without the gateway. A declared
 * {@code Content-Length} above the limit is rejected before the body is read; a body of unknown
 * length (chunked) is counted while it is read and fails with {@link PayloadTooLargeException},
 * mapped to 413 by {@link GlobalExceptionHandler}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestBodySizeLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestBodySizeLimitFilter.class);

    private final long maxBytes;
    private final ObjectMapper objectMapper;

    public RequestBodySizeLimitFilter(
            @Value("${app.request.max-body-size:1MB}") DataSize maxBodySize,
            ObjectMapper objectMapper) {
        this.maxBytes = maxBodySize.toBytes();
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (request.getContentLengthLong() > maxBytes) {
            PayloadTooLargeException ex = new PayloadTooLargeException(maxBytes);
            LOG.warn(
                    "Client error for request: {} | Code: {} | ExceptionType: {}",
                    RequestLogSanitizer.route(request),
                    GlobalExceptionHandler.PAYLOAD_TOO_LARGE_CODE,
                    ex.getClass().getSimpleName());
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(
                    response.getOutputStream(), GlobalExceptionHandler.payloadTooLarge(ex));
            return;
        }

        chain.doFilter(new SizeLimitedRequest(request, maxBytes), response);
    }

    /** Counts body bytes from both {@link #getInputStream()} and {@link #getReader()}. */
    static final class SizeLimitedRequest extends HttpServletRequestWrapper {

        private final long maxBytes;
        private ServletInputStream inputStream;
        private BufferedReader reader;

        SizeLimitedRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (inputStream == null) {
                inputStream = new CountingInputStream(super.getInputStream(), maxBytes);
            }
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (reader == null) {
                Charset charset =
                        Optional.ofNullable(getCharacterEncoding())
                                .map(Charset::forName)
                                .orElse(StandardCharsets.ISO_8859_1);
                reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
            }
            return reader;
        }
    }

    static final class CountingInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxBytes;
        private long count;

        CountingInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1) {
                add(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = delegate.read(buffer, offset, length);
            if (n > 0) {
                add(n);
            }
            return n;
        }

        private void add(long n) throws PayloadTooLargeException {
            count += n;
            if (count > maxBytes) {
                throw new PayloadTooLargeException(maxBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
