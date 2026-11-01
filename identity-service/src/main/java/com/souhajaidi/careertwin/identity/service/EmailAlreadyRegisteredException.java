package com.souhajaidi.careertwin.identity.service;

import com.souhajaidi.careertwin.security.BusinessException;
import org.springframework.http.HttpStatus;

/** The message deliberately omits the address so the response cannot be used to harvest it back. */
public class EmailAlreadyRegisteredException extends BusinessException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists.");
    }
}
