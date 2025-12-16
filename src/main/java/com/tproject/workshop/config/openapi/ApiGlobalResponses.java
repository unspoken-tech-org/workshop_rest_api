package com.tproject.workshop.config.openapi;

import com.tproject.workshop.errorhandling.ResponseError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "400",
                description = "Bad Request - Validation Errors",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ResponseError.class)
                )
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = @Content
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = @Content
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Resource Not Found",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ResponseError.class)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ResponseError.class)
                )
        )
})
public @interface ApiGlobalResponses {
}

