package com.souhajaidi.careertwin.security;

import java.util.UUID;

/** The identity carried by a verified token. */
public record AuthenticatedUser(UUID userId, String role) {}
