package com.souhajaidi.careertwin.identity.api;

import com.souhajaidi.careertwin.identity.api.dto.LoginRequest;
import com.souhajaidi.careertwin.identity.api.dto.RegisterRequest;
import com.souhajaidi.careertwin.identity.api.dto.TokenResponse;
import com.souhajaidi.careertwin.identity.api.dto.UserResponse;
import com.souhajaidi.careertwin.identity.config.OpenApiConfig;
import com.souhajaidi.careertwin.identity.service.AuthService;
import com.souhajaidi.careertwin.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Account registration, login and the current user")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an account; new accounts always have the USER role")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for a bearer access token")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The authenticated user", security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    public UserResponse me() {
        return UserResponse.from(authService.requireActive(currentUser.requireId()));
    }
}
