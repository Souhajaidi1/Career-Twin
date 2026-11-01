package com.souhajaidi.careertwin.identity.service;

import com.souhajaidi.careertwin.identity.api.dto.LoginRequest;
import com.souhajaidi.careertwin.identity.api.dto.RegisterRequest;
import com.souhajaidi.careertwin.identity.api.dto.TokenResponse;
import com.souhajaidi.careertwin.identity.domain.Role;
import com.souhajaidi.careertwin.identity.domain.User;
import com.souhajaidi.careertwin.identity.repository.UserRepository;
import com.souhajaidi.careertwin.identity.token.JwtIssuer;
import com.souhajaidi.careertwin.security.BusinessException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    /** Matched against when the email is unknown, so that path costs as much as a real one. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.USER);
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent registrations can both pass the check above; the unique index is the
            // real arbiter, and the loser gets the same answer as a sequential duplicate.
            throw new EmailAlreadyRegisteredException();
        }
    }

    /** Not transactional: the BCrypt comparison is slow and must not hold a database connection. */
    public TokenResponse login(LoginRequest request) {
        Optional<User> found = users.findByEmailIgnoreCase(normalize(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        User user = found.filter(candidate -> passwordMatches && candidate.isEnabled())
                .orElseThrow(AuthenticationFailedException::new);
        return TokenResponse.bearer(jwtIssuer.issue(user.getId(), user.getRole()), jwtIssuer.ttlSeconds());
    }

    @Transactional(readOnly = true)
    public User requireActive(UUID userId) {
        return users.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required."));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
