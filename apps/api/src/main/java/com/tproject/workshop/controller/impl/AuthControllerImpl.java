package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.AuthController;
import com.tproject.workshop.dto.auth.RefreshTokenRequest;
import com.tproject.workshop.dto.auth.RefreshTokenResponse;
import com.tproject.workshop.dto.auth.TokenRequest;
import com.tproject.workshop.dto.auth.TokenResponse;
import com.tproject.workshop.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthControllerImpl implements AuthController {

    private final AuthService authService;

    @Override
    public TokenResponse getToken(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody TokenRequest request) {
        return authService.authenticate(apiKey, request);
    }

    @Override
    public RefreshTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @Override
    public void revokeToken(@Valid @RequestBody RefreshTokenRequest request) {
        authService.revokeToken(request);
    }
}
