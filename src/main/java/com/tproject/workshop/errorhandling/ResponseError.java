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
    @JsonProperty("meta")
    private ErrorMetadata errorMetadata;

    public ResponseError(int status, String title) {
        super();
        this.status = status;
        this.title = title;
    }
}
