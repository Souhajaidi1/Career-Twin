package com.souhajaidi.careertwin.identity.repository;

import com.souhajaidi.careertwin.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Both derive "lower(email) = lower(?)", which is the expression the unique index covers.
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
