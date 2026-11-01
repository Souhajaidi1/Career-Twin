package com.souhajaidi.careertwin.engine;

import java.time.LocalDate;
import java.util.List;

/**
 * One thing the user can point to. {@code months} is the duration for
 * experience, projects and repositories; {@code score} (0..100) applies to
 * assessments and interviews only. {@code simulated} marks what-if evidence
 * that never touches storage.
 */
public record Evidence(
        String id,
        EvidenceType type,
        String title,
        LocalDate date,
        Double months,
        Integer score,
        boolean simulated,
        List<SkillLink> skills) {

    public Evidence {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
