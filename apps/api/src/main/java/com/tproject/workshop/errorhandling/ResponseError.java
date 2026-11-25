package com.tproject.workshop.errorhandling;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class ResponseError {

    private int status;
    private String title;

    @JsonProperty("error")
    private ErrorMetadata.Error errors;

    public ResponseError(int status, String title) {
        super();
        this.status = status;
        this.title = title;
    }

    public static ResponseError of(ErrorCode errorCode, String message) {
        return new ResponseError(
                errorCode.status().value(),
                errorCode.title(),
                new ErrorMetadata.Error(errorCode.code(), message));
    }
}
