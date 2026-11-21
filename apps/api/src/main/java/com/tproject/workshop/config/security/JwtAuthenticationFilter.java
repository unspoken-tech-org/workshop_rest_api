package com.tproject.workshop.config.security;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.repository.ApiKeyRepository;
import com.tproject.workshop.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ApiKeyRepository apiKeyRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = jwtService.validateAndGetClaims(token);
            String subject = claims.getSubject();

            Number apiKeyIdClaim = claims.get("apiKeyId", Number.class);
            if (apiKeyIdClaim == null) {
                log.warn("JWT authentication failed: missing apiKeyId claim");
                filterChain.doFilter(request, response);
                return;
            }

            Long apiKeyId = apiKeyIdClaim.longValue();
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(apiKeyId);
            if (apiKeyOpt.isEmpty()) {
                log.warn("JWT authentication failed: API key not found (apiKeyId={})", apiKeyId);
                filterChain.doFilter(request, response);
                return;
            }

            ApiKey apiKey = apiKeyOpt.get();
            if (!apiKey.isActive()) {
                log.warn("JWT authentication failed: API key inactive (apiKeyId={})", apiKeyId);
                filterChain.doFilter(request, response);
                return;
            }

            if (apiKey.getExpiresAt() != null && !apiKey.getExpiresAt().isAfter(Instant.now())) {
                log.warn("JWT authentication failed: API key expired (apiKeyId={})", apiKeyId);
                filterChain.doFilter(request, response);
                return;
            }

            String tokenBoundDeviceId = claims.get("boundDeviceId", String.class);
            if (apiKey.getBoundDeviceId() == null
                    || !apiKey.getBoundDeviceId().equals(tokenBoundDeviceId)) {
                log.warn(
                        "JWT authentication failed: API key device binding mismatch or reset (apiKeyId={})",
                        apiKeyId);
                filterChain.doFilter(request, response);
                return;
            }

            // Extract roles from claims
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities =
                    roles == null
                            ? Collections.emptyList()
                            : roles.stream().map(SimpleGrantedAuthority::new).toList();

            // Creates authentication in the Spring Security context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(subject, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Subject and boundDeviceId originate from credentials/device data;
            // only the bounded role set is useful in a diagnostic log.
            log.debug("JWT authentication succeeded with roles: {}", roles);

        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            // Do not throw exception, let Spring Security return 401
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.equals("/actuator/health")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }
}
