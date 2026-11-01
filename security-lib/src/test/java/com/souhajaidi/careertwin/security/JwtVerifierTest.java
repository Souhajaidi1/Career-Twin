package com.souhajaidi.careertwin.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtVerifierTest {

    private static final String ISSUER = "careertwin-identity";

    private final String[] pair = KeyCodec.generateBase64Pair();
    private final JwtVerifier verifier = new JwtVerifier(new JwtProperties(pair[1], ISSUER));

    private String token(String privateKey, String issuer, Instant expires, String role) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(UUID.randomUUID().toString())
                .claim(JwtVerifier.CLAIM_ROLE, role)
                .issuedAt(new Date())
                .expiration(Date.from(expires))
                .signWith(KeyCodec.decodePrivate(privateKey), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    @DisplayName("a token signed by the matching private key verifies and carries its role")
    void acceptsValidToken() {
        var user = verifier.verify(token(pair[0], ISSUER, Instant.now().plusSeconds(60), "USER"));
        assertThat(user).isPresent();
        assertThat(user.get().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("a token signed by a different key is rejected")
    void rejectsForeignSignature() {
        String[] other = KeyCodec.generateBase64Pair();
        assertThat(verifier.verify(token(other[0], ISSUER, Instant.now().plusSeconds(60), "USER"))).isEmpty();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void rejectsExpired() {
        assertThat(verifier.verify(token(pair[0], ISSUER, Instant.now().minusSeconds(60), "USER"))).isEmpty();
    }

    @Test
    @DisplayName("a token from another issuer is rejected")
    void rejectsWrongIssuer() {
        assertThat(verifier.verify(token(pair[0], "someone-else", Instant.now().plusSeconds(60), "USER"))).isEmpty();
    }

    @Test
    @DisplayName("garbage is rejected without throwing")
    void rejectsGarbage() {
        assertThat(verifier.verify("not.a.jwt")).isEmpty();
        assertThat(verifier.verify("")).isEmpty();
    }
}
