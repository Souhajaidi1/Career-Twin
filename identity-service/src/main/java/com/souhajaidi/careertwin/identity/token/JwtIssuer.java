package com.souhajaidi.careertwin.identity.token;

import com.souhajaidi.careertwin.identity.domain.Role;
import com.souhajaidi.careertwin.security.JwtProperties;
import com.souhajaidi.careertwin.security.JwtVerifier;
import com.souhajaidi.careertwin.security.KeyCodec;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Signs access tokens with RS256. The issuer claim comes from the same property every service
 * verifies against, so the two sides cannot drift apart. The key is decoded once at startup, so a
 * malformed key fails the boot rather than the first login.
 */
@Component
public class JwtIssuer {

    private final PrivateKey key;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    public JwtIssuer(TokenProperties tokenProperties, JwtProperties jwtProperties, Clock clock) {
        this.key = KeyCodec.decodePrivate(tokenProperties.privateKey());
        this.issuer = jwtProperties.issuer();
        this.ttl = tokenProperties.ttl();
        this.clock = clock;
    }

    public String issue(UUID userId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim(JwtVerifier.CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.RS256)
                .compact();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}
