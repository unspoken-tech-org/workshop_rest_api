package com.tproject.workshop.exception;

import java.io.IOException;
import java.io.Serial;

/**
 * Request body larger than the configured limit. It is an {@link IOException} because it is thrown
 * from the request input stream; while Jackson reads the body, Spring wraps it in an {@code
 * HttpMessageNotReadableException}.
 */
public class PayloadTooLargeException extends IOException {

    @Serial private static final long serialVersionUID = 2958610394527187163L;

    private final long maxBytes;

    public PayloadTooLargeException(long maxBytes) {
        super("O corpo da requisição excede o limite de " + describe(maxBytes) + ".");
        this.maxBytes = maxBytes;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    private static String describe(long bytes) {
        long kb = 1024L;
        long mb = kb * kb;
        if (bytes >= mb && bytes % mb == 0) {
            return bytes / mb + "MB";
        }
        if (bytes >= kb && bytes % kb == 0) {
            return bytes / kb + "KB";
        }
        return bytes + " bytes";
    }
}
