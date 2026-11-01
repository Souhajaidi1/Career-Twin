package com.souhajaidi.careertwin.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull @Size(min = 12, max = 100, message = "must be between 12 and 100 characters") String password,
        @NotBlank @Size(max = 120) String fullName) {}
