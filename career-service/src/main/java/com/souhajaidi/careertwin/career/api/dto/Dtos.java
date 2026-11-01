package com.souhajaidi.careertwin.career.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.souhajaidi.careertwin.career.domain.EvidenceRecord;
import com.souhajaidi.careertwin.career.domain.Goal;
import com.souhajaidi.careertwin.career.domain.Profile;
import com.souhajaidi.careertwin.engine.Evidence;
import com.souhajaidi.careertwin.engine.EvidenceType;
import com.souhajaidi.careertwin.engine.Results;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Wire types for career-service. Field names are the contract the Angular client is written against. */
public final class Dtos {

    private Dtos() {}

    // ---- profile
    public record ProfileBody(
            @NotNull @Size(max = 120) String name,
            @NotNull @Size(max = 120) String currentRole,
            @DecimalMin("0") @DecimalMax("60") double experienceYears,
            @NotNull @Size(max = 120) String location) {

        public static ProfileBody from(Profile p) {
            return new ProfileBody(p.getName(), p.getCurrentRole(), p.getExperienceYears(), p.getLocation());
        }
    }

    // ---- evidence
    public record SkillRefBody(
            @NotBlank @Size(max = 40) String id,
            @DecimalMin(value = "0.01") @DecimalMax("1") double weight) {}

    public record EvidenceRequest(
            @NotBlank @Pattern(regexp = "experience|project|certification|education|repository|assessment|interview|claim")
                    String type,
            @NotBlank @Size(max = 160) String title,
            @Size(max = 160) String org,
            LocalDate date,
            @DecimalMin("0") @DecimalMax("600") Double months,
            @Min(0) @Max(100) Integer score,
            @Size(max = 1000) String note,
            @NotNull @Size(min = 1, max = 20) List<@Valid SkillRefBody> skills) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record EvidenceResponse(
            UUID id,
            String type,
            String title,
            String org,
            LocalDate date,
            Double months,
            Integer score,
            String note,
            String origin,
            List<SkillRefBody> skills) {

        public static EvidenceResponse from(EvidenceRecord e) {
            return new EvidenceResponse(
                    e.getId(),
                    e.type().wire(),
                    e.getTitle(),
                    e.getOrg(),
                    e.getDate(),
                    e.getMonths(),
                    e.getScore(),
                    e.getNote(),
                    e.getOrigin(),
                    e.getSkills().stream()
                            .map(s -> new SkillRefBody(s.getSkillId(), s.getWeight()))
                            .toList());
        }

        /** The engine's view of a stored record: no PII, no notes, only what scoring uses. */
        public static Evidence toEngine(EvidenceRecord e) {
            return new Evidence(
                    e.getId().toString(),
                    e.type(),
                    e.getTitle(),
                    e.getDate(),
                    e.getMonths(),
                    e.getScore(),
                    false,
                    e.getSkills().stream()
                            .map(s -> new com.souhajaidi.careertwin.engine.SkillLink(s.getSkillId(), s.getWeight()))
                            .toList());
        }
    }

    // ---- goals
    public record GoalRequest(@NotBlank @Size(max = 40) String roleId, @NotBlank @Size(max = 16) String seniority) {}

    public record GoalResponse(UUID id, String roleId, String seniority, boolean active, Instant createdAt) {
        public static GoalResponse from(Goal g) {
            return new GoalResponse(g.getId(), g.getRoleId(), g.getSeniority(), g.isActive(), g.getCreatedAt());
        }
    }

    // ---- catalog
    public record CatalogSkill(String id, String name, String category, List<String> prerequisites) {}

    public record CatalogRequirement(String skillId, int target, double weight) {}

    public record CatalogRole(String id, String name, int typicalYears, List<CatalogRequirement> requirements) {}

    public record CatalogEvidenceType(String type, String label, int max, String trust) {}

    public record CatalogResponse(
            List<CatalogSkill> skills,
            List<CatalogRole> roles,
            List<String> seniorities,
            List<CatalogEvidenceType> evidenceTypes) {}

    // ---- analysis
    public record GoalSummary(String roleId, String roleName, String seniority) {}

    public record ReadinessBody(int score, List<Results.ReadinessPart> parts) {}

    public record ContributionBody(
            String evidenceId, String type, String title, LocalDate date, double points, String detail) {}

    public record SkillLevelBody(
            String skillId,
            String name,
            String category,
            int level,
            int confidence,
            boolean simulated,
            List<ContributionBody> contributions) {}

    public record GapBody(
            String skillId,
            String name,
            String category,
            int current,
            int target,
            int gap,
            double weight,
            String status,
            List<String> missingPrerequisites,
            double impact,
            int confidence) {}

    public record TwinResponse(
            GoalSummary goal, ReadinessBody readiness, List<SkillLevelBody> levels, List<GapBody> gaps) {}

    public record HypotheticalBody(
            @NotBlank @Size(max = 40) String skillId,
            @NotBlank @Pattern(regexp = "assessment|project|certification") String type,
            @Min(0) @Max(100) Integer score,
            @DecimalMin("0") @DecimalMax("120") Double months) {}

    public record SimulationRequest(@NotNull @Size(min = 1, max = 10) List<@Valid HypotheticalBody> hypotheticals) {}

    public record SimulatedLevel(String skillId, String name, int base, int sim) {}

    public record SimulationResponse(int baseScore, int simScore, int delta, List<SimulatedLevel> levels) {}

    // ---- job matching (called by job-service)
    public record MatchBatchRequest(
            @NotNull @Size(min = 1, max = 100) Map<@NotBlank String, @Valid @NotNull Results.JobRequirements> jobs) {}

    public record MatchRow(
            String skillId, String name, boolean required, int level, int need, double ratio, String state) {}

    public record MatchResponse(
            int score,
            int completeness,
            int requiredLevel,
            List<Results.MatchCategory> categories,
            List<String> excluded,
            List<MatchRow> strong,
            List<MatchRow> partial,
            List<MatchRow> missing) {}

    public record MatchBatchResponse(Map<String, MatchResponse> results) {}

    public static EvidenceType typeOf(String wire) {
        return EvidenceType.fromWire(wire);
    }
}
