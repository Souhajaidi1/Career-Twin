package com.souhajaidi.careertwin.security;

import org.springframework.http.HttpStatus;

/**
 * An error that is part of the API contract: stable machine-readable code, a status, and a message
 * that is safe to show the caller. Anything not derived from this class is treated as an internal
 * fault and never leaks its message.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
