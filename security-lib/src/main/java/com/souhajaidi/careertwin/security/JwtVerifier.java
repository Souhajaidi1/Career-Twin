package com.souhajaidi.careertwin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

/**
 * Verifies signature, issuer and expiry. A malformed, expired or foreign-signed token yields an
 * empty result rather than an exception, so the filter can treat it as simply unauthenticated.
 */
public class JwtVerifier {

    public static final String CLAIM_ROLE = "role";

    private final PublicKey key;
    private final String issuer;

    public JwtVerifier(JwtProperties properties) {
        this.key = KeyCodec.decodePublic(properties.publicKey());
        this.issuer = properties.issuer();
    }

    public Optional<AuthenticatedUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get(CLAIM_ROLE, String.class);
            if (role == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(UUID.fromString(claims.getSubject()), role));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
