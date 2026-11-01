package com.souhajaidi.careertwin.identity.api.dto;

import com.souhajaidi.careertwin.identity.domain.Role;
import com.souhajaidi.careertwin.identity.domain.User;
import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
