package com.souhajaidi.careertwin.identity.service;

import com.souhajaidi.careertwin.security.BusinessException;
import org.springframework.http.HttpStatus;

/** Single failure for unknown email, wrong password and disabled account: callers must not tell them apart. */
public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException() {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid credentials.");
    }
}
