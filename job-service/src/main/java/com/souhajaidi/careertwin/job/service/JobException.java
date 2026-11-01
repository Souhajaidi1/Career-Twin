package com.souhajaidi.careertwin.job.service;

import com.souhajaidi.careertwin.security.BusinessException;
import org.springframework.http.HttpStatus;

public class JobException extends BusinessException {

    private JobException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static JobException notFound() {
        return new JobException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Job not found.");
    }

    public static JobException analysisUnavailable() {
        return new JobException(
                HttpStatus.BAD_GATEWAY, "ANALYSIS_UNAVAILABLE", "Match scoring is temporarily unavailable. Try again shortly.");
    }
}
