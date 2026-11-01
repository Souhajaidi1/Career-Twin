package com.souhajaidi.careertwin.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Known business failures return their own message because the caller needs it to act. Everything
 * else returns a fixed message and is logged with its stack trace, because an unexpected
 * exception's message can carry table names, SQL fragments or file paths.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail business(BusinessException ex, HttpServletRequest request) {
        if (ex.status().is5xxServerError()) {
            log.error("Business failure on {} {}", request.getMethod(), request.getRequestURI(), ex);
        }
        return ProblemFactory.of(ex.status(), ex.code(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> violations = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> violations.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail problem = ProblemFactory.of(
                HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid.", request.getRequestURI());
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ProblemFactory.of(
                HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body could not be read.", request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail illegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ProblemFactory.of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ProblemFactory.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "The request could not be completed.",
                request.getRequestURI());
    }
}
