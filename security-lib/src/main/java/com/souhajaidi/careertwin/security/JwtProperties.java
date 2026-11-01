package com.souhajaidi.careertwin.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * What a service needs to <em>verify</em> a token: the identity service's public key and the
 * expected issuer. There is deliberately no secret here. Tokens are signed with RS256, so a
 * service that can verify them still cannot mint one.
 *
 * @param publicKey base64 of the DER (X.509 SubjectPublicKeyInfo) encoded RSA public key
 */
@Validated
@ConfigurationProperties(prefix = "careertwin.jwt")
public record JwtProperties(@NotBlank String publicKey, @NotBlank String issuer) {}
