package com.tproject.workshop.errorhandling;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Getter
@Setter
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@SuppressWarnings("squid:S3398")
public class ErrorMetadata implements Serializable {

    private static final long serialVersionUID = -4204013823809613543L;
    private final transient List<Error> globalErrors = new ArrayList<>();
    private final transient ConcurrentHashMap<String, Object> fieldErrors = new ConcurrentHashMap<>();

    public static ErrorsBuilder builder() {
        return new ErrorsBuilder();
    }

    public List<Error> getGlobalErrors() {
        return globalErrors;
    }

    public Map<String, Object> getFieldErrors() {
        return this.fieldErrors;
    }

    public boolean hasErrors() {
        return !this.globalErrors.isEmpty() || !this.fieldErrors.isEmpty();
    }

    private ErrorMetadata addError(final String code, final String error) {
        this.globalErrors.add(new Error(code, error));
        return this;
    }

    @SuppressWarnings("unchecked")
    private ErrorMetadata addFieldError(final String field, final String error, final String template) {
        Map<String, Object> errorMap = fieldErrors;
        List<String> fieldChar = Arrays.asList(field.split(Pattern.quote(".")));
        String value = fieldChar.get(fieldChar.size() - 1);
        errorMap.put(value, new Error(template, error));
        return this;
    }

    @Getter
    @Setter
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    @NoArgsConstructor
    @SuppressWarnings("squid:S3398")
    public static final class Error {

        private String code;

        private String message;

        public Error(final String code, final String message) {
            super();
            this.code = code;
            this.message = message;
        }
    }

    public static final class ErrorsBuilder {

        private final ErrorMetadata errors;

        private ErrorsBuilder() {
            super();
            this.errors = new ErrorMetadata();
        }

        public ErrorsBuilder addError(final String code, final String error) {
            this.errors.addError(code, error);
            return this;
        }

        public ErrorsBuilder addFieldError(final String field, final String error, final String template) {
            this.errors.addFieldError(field, error, template);
            return this;
        }

        public ErrorMetadata build() {
            return this.errors;
        }
    }
}

