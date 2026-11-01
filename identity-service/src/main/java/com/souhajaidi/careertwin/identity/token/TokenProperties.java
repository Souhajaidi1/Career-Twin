package com.souhajaidi.careertwin.identity.token;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param privateKey base64 of the DER (PKCS#8) RSA private key; only this service may hold it
 * @param ttl lifetime of an issued access token
 */
@Validated
@ConfigurationProperties(prefix = "careertwin.identity.token")
public record TokenProperties(@NotBlank String privateKey, Duration ttl) {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    public TokenProperties {
        ttl = ttl == null ? DEFAULT_TTL : ttl;
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("careertwin.identity.token.ttl must be positive");
        }
    }
}
