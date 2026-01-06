package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.ApiKeyController;
import com.tproject.workshop.dto.apikey.ApiKeyCreatedResponse;
import com.tproject.workshop.dto.apikey.ApiKeyResponse;
import com.tproject.workshop.dto.apikey.CreateApiKeyRequest;
import com.tproject.workshop.service.auth.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/api-keys")
public class ApiKeyControllerImpl implements ApiKeyController {

    private final ApiKeyService apiKeyService;

    @Override
    public ApiKeyCreatedResponse create(@Valid @RequestBody CreateApiKeyRequest request) {
        return apiKeyService.createApiKey(
                request.clientName(),
                request.platform(),
                request.description(),
                request.expiresAt()
        );
    }

    @Override
    public List<ApiKeyResponse> listAll() {
        return apiKeyService.findAll();
    }

    @Override
    public ApiKeyResponse findById(Long id) {
        return apiKeyService.findById(id);
    }

    @Override
    public void revoke(Long id) {
        apiKeyService.revokeApiKey(id);
    }
}
