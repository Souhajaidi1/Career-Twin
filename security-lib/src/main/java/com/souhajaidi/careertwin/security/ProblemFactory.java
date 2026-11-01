package com.souhajaidi.careertwin.security;

import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Builds the one error document shape every service returns (RFC 7807 plus a stable {@code code}). */
public final class ProblemFactory {

    private static final URI TYPE = URI.create("https://github.com/Souhajaidi1/Career-Twin/blob/main/docs/errors.md");

    private ProblemFactory() {}

    public static ProblemDetail of(HttpStatus status, String code, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(TYPE);
        problem.setTitle(status.getReasonPhrase());
        if (instance != null) {
            problem.setInstance(URI.create(instance));
        }
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
