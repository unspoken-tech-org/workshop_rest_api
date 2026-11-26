package com.tproject.workshop.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.errorhandling.ErrorCode;
import com.tproject.workshop.errorhandling.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class DomainExceptionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldBuildConsistentPtBrNotFoundMessages() {
        assertThat(NotFoundException.of("Modelo", 7).getMessage())
                .isEqualTo("Modelo com id 7 não encontrado");
        assertThat(NotFoundException.ofFeminine("Marca", 3).getMessage())
                .isEqualTo("Marca com id 3 não encontrada");
    }

    @Test
    void shouldRouteAllDomainExceptionsThroughSingleHandler() {
        ServletWebRequest req =
                new ServletWebRequest(new MockHttpServletRequest("GET", "/v1/test"));

        assertHandled(NotFoundException.of("Aparelho", 1), ErrorCode.ENTITY_NOT_FOUND, req);
        assertHandled(new BadRequestException("inválido"), ErrorCode.BAD_REQUEST, req);
        assertHandled(
                new EntityAlreadyExistsException("duplicado"), ErrorCode.RESOURCE_CONFLICT, req);
        assertHandled(new InvalidApiKeyException("invalid"), ErrorCode.INVALID_API_KEY, req);
        assertHandled(new InvalidTokenException("invalid"), ErrorCode.INVALID_TOKEN, req);
        assertHandled(new TokenExpiredException("expired"), ErrorCode.TOKEN_EXPIRED, req);
        assertHandled(new ApiKeyDeviceBoundException("bound"), ErrorCode.API_KEY_DEVICE_BOUND, req);
    }

    @Test
    void shouldMapInternalWorkshopExceptionTo500WithoutLeakingDetails() {
        ServletWebRequest req =
                new ServletWebRequest(new MockHttpServletRequest("POST", "/v1/brand"));

        ResponseEntity<ProblemDetail> response =
                handler.handleDomainException(
                        new WorkshopException("Erro ao criar ou buscar marca: test"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties())
                .containsEntry("code", ErrorCode.INTERNAL_SERVER_ERROR.code());
        assertThat(response.getBody().getDetail()).doesNotContain("marca: test");
    }

    private void assertHandled(DomainException ex, ErrorCode expected, ServletWebRequest req) {
        ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(expected.status());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(expected.status().value());
        assertThat(response.getBody().getTitle()).isEqualTo(expected.title());
        assertThat(response.getBody().getProperties()).containsEntry("code", expected.code());
        assertThat(response.getBody().getDetail()).isEqualTo(ex.getMessage());
    }
}
