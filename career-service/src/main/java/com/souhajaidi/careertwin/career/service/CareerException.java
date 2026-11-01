package com.souhajaidi.careertwin.career.service;

import com.souhajaidi.careertwin.security.BusinessException;
import org.springframework.http.HttpStatus;

public class CareerException extends BusinessException {

    private CareerException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static CareerException notFound(String what) {
        return new CareerException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " not found.");
    }

    public static CareerException unknownSkill(String id) {
        return new CareerException(HttpStatus.BAD_REQUEST, "UNKNOWN_SKILL", "Unknown skill: " + id + ".");
    }

    public static CareerException unknownRole(String id) {
        return new CareerException(HttpStatus.BAD_REQUEST, "UNKNOWN_ROLE", "Unknown role: " + id + ".");
    }

    public static CareerException unknownSeniority(String s) {
        return new CareerException(HttpStatus.BAD_REQUEST, "UNKNOWN_SENIORITY", "Unknown seniority: " + s + ".");
    }

    public static CareerException invalid(String message) {
        return new CareerException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    public static CareerException noGoal() {
        return new CareerException(HttpStatus.CONFLICT, "NO_GOAL", "Choose a target role first.");
    }

    public static CareerException reservedType(String type) {
        return new CareerException(
                HttpStatus.BAD_REQUEST,
                "RESERVED_EVIDENCE_TYPE",
                "'" + type + "' evidence is produced by in-product assessments and cannot be entered by hand.");
    }
}
