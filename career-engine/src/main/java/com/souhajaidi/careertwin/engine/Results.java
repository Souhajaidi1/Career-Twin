package com.souhajaidi.careertwin.engine;

import java.time.LocalDate;
import java.util.List;

/** Immutable result types returned by {@link ScoringEngine}. */
public final class Results {

    private Results() {}

    /** One evidence record's contribution to one skill, kept so the arithmetic can be replayed. */
    public record Contribution(
            String evidenceId,
            EvidenceType type,
            String title,
            LocalDate date,
            double points,
            double weight,
            double recency,
            boolean simulated,
            String detail) {}

    public record SkillLevel(
            String skillId,
            int level,
            int confidence,
            LocalDate lastAt,
            boolean simulated,
            List<Contribution> contributions) {}

    public record ReadinessPart(String key, String label, int weight, double value, String detail) {}

    public record ScoredRequirement(String skillId, int target, double weight) {}

    public record Readiness(int score, Role role, List<ScoredRequirement> requirements, List<ReadinessPart> parts) {}

    public enum GapStatus {
        MET,
        MINOR,
        MAJOR,
        CRITICAL;

        public String wire() {
            return name().toLowerCase();
        }
    }

    public record Gap(
            String skillId,
            int current,
            int target,
            int gap,
            double weight,
            GapStatus status,
            List<String> missingPrerequisites,
            double impact,
            int confidence) {}

    /** What the user's job description asks for, derived from the stored raw text. */
    public record JobSkill(String skillId, boolean required, int mentions, String quote) {}

    public record OtherRequirement(String skillId, String label) {}

    public record JobRequirements(
            List<JobSkill> skills,
            Integer years,
            String degree,
            List<OtherRequirement> others,
            List<String> ambiguous,
            String seniority) {}

    public enum MatchState {
        STRONG,
        PARTIAL,
        MISSING;

        public String wire() {
            return name().toLowerCase();
        }
    }

    public record SkillMatch(
            String skillId, boolean required, int level, int need, double ratio, MatchState state, int confidence) {}

    public record MatchCategory(String key, String label, int weight, int effective, double value, double points) {}

    public record MatchResult(
            int score,
            int completeness,
            int requiredLevel,
            List<MatchCategory> categories,
            List<String> excluded,
            List<SkillMatch> strong,
            List<SkillMatch> partial,
            List<SkillMatch> missing) {}

    public record RoadmapItem(
            String skillId,
            String title,
            String objective,
            int effortHours,
            int weekStart,
            int weekEnd,
            String priority,
            List<String> prerequisites,
            String produces,
            int gapAtCreation,
            int readinessDelta,
            int skillDelta) {}

    public record Hypothetical(String skillId, EvidenceType type, Integer score, Double months) {}

    public record SimulatedSkill(String skillId, int base, int sim) {}

    public record Simulation(int baseScore, int simScore, int delta, List<SimulatedSkill> levels) {}
}
