package com.tproject.workshop.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "jwt.rsa")
public record RsaKeyProperties(
    RSAPublicKey publicKey,
    RSAPrivateKey privateKey
) {}
