package com.souhajaidi.careertwin.career.service;

import com.souhajaidi.careertwin.career.api.dto.Dtos.*;
import com.souhajaidi.careertwin.career.api.dto.Dtos.EvidenceResponse;
import com.souhajaidi.careertwin.career.domain.EvidenceRecord;
import com.souhajaidi.careertwin.career.domain.Goal;
import com.souhajaidi.careertwin.career.repository.EvidenceRepository;
import com.souhajaidi.careertwin.career.repository.GoalRepository;
import com.souhajaidi.careertwin.career.repository.ProfileRepository;
import com.souhajaidi.careertwin.engine.Catalog;
import com.souhajaidi.careertwin.engine.Evidence;
import com.souhajaidi.careertwin.engine.EvidenceType;
import com.souhajaidi.careertwin.engine.Results;
import com.souhajaidi.careertwin.engine.ScoringEngine;
import com.souhajaidi.careertwin.engine.ScoringEngine.Signals;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a user's stored records into scores. Every number returned here is produced by the
 * deterministic {@link ScoringEngine}; nothing is estimated, cached or guessed.
 */
@Service
@Transactional(readOnly = true)
public class AnalysisService {

    private final ScoringEngine engine;
    private final EvidenceRepository evidenceRepository;
    private final ProfileRepository profiles;
    private final GoalRepository goals;

    public AnalysisService(
            ScoringEngine engine, EvidenceRepository evidenceRepository, ProfileRepository profiles, GoalRepository goals) {
        this.engine = engine;
        this.evidenceRepository = evidenceRepository;
        this.profiles = profiles;
        this.goals = goals;
    }

    /** A user's records, loaded once per request and shared by every calculation in it. */
    private record Snapshot(List<Evidence> evidence, Signals signals, Goal goal) {}

    private Snapshot snapshot(UUID userId) {
        List<EvidenceRecord> records = evidenceRepository.findByUserIdOrderByDateDescIdAsc(userId);
        List<Evidence> evidence = records.stream().map(EvidenceResponse::toEngine).toList();
        double years = profiles.findById(userId).map(p -> p.getExperienceYears()).orElse(0.0);
        int assessments = (int) records.stream().filter(r -> r.type() == EvidenceType.ASSESSMENT).count();
        int interviews = (int) records.stream().filter(r -> r.type() == EvidenceType.INTERVIEW).count();
        Goal goal = goals.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(userId).orElse(null);
        return new Snapshot(evidence, new Signals(years, assessments, interviews), goal);
    }

    public TwinResponse twin(UUID userId) {
        Snapshot s = snapshot(userId);
        Map<String, Results.SkillLevel> levels = engine.skillLevels(s.evidence());
        List<SkillLevelBody> levelBodies = levels.values().stream()
                .map(AnalysisService::level)
                .sorted((a, b) -> Integer.compare(b.level(), a.level()))
                .toList();
        if (s.goal() == null) {
            return new TwinResponse(null, null, levelBodies, List.of());
        }
        Goal g = s.goal();
        Results.Readiness readiness = engine.readiness(levels, g.getRoleId(), g.getSeniority(), s.evidence(), s.signals());
        List<GapBody> gaps = engine.gaps(levels, g.getRoleId(), g.getSeniority()).stream()
                .map(AnalysisService::gap)
                .toList();
        return new TwinResponse(
                new GoalSummary(readiness.role().id(), readiness.role().name(), g.getSeniority()),
                new ReadinessBody(readiness.score(), readiness.parts()),
                levelBodies,
                gaps);
    }

    public List<Results.RoadmapItem> roadmap(UUID userId, double hoursPerWeek) {
        Snapshot s = snapshot(userId);
        if (s.goal() == null) {
            return List.of();
        }
        return engine.roadmap(s.evidence(), s.goal().getRoleId(), s.goal().getSeniority(), s.signals(), hoursPerWeek);
    }

    public SimulationResponse simulate(UUID userId, SimulationRequest request) {
        Snapshot s = snapshot(userId);
        if (s.goal() == null) {
            throw CareerException.noGoal();
        }
        for (HypotheticalBody h : request.hypotheticals()) {
            if (!Catalog.SKILL_BY_ID.containsKey(h.skillId())) {
                throw CareerException.unknownSkill(h.skillId());
            }
        }
        List<Results.Hypothetical> hypotheticals = request.hypotheticals().stream()
                .map(h -> new Results.Hypothetical(h.skillId(), EvidenceType.fromWire(h.type()), h.score(), h.months()))
                .toList();
        Results.Simulation sim = engine.simulate(
                s.evidence(), s.goal().getRoleId(), s.goal().getSeniority(), s.signals(), hypotheticals);
        return new SimulationResponse(
                sim.baseScore(),
                sim.simScore(),
                sim.delta(),
                sim.levels().stream()
                        .map(l -> new SimulatedLevel(l.skillId(), Catalog.SKILL_BY_ID.get(l.skillId()).name(), l.base(), l.sim()))
                        .toList());
    }

    /** Scores several parsed job postings against this user in one pass. */
    public MatchBatchResponse match(UUID userId, MatchBatchRequest request) {
        Snapshot s = snapshot(userId);
        Map<String, Results.SkillLevel> levels = engine.skillLevels(s.evidence());
        Map<String, MatchResponse> out = new LinkedHashMap<>();
        request.jobs().forEach((key, requirements) -> {
            Results.MatchResult m = engine.match(requirements, levels, s.evidence(), s.signals());
            out.put(key, toResponse(m));
        });
        return new MatchBatchResponse(out);
    }

    private static MatchResponse toResponse(Results.MatchResult m) {
        return new MatchResponse(
                m.score(),
                m.completeness(),
                m.requiredLevel(),
                m.categories(),
                m.excluded(),
                m.strong().stream().map(AnalysisService::row).toList(),
                m.partial().stream().map(AnalysisService::row).toList(),
                m.missing().stream().map(AnalysisService::row).toList());
    }

    private static MatchRow row(Results.SkillMatch r) {
        return new MatchRow(
                r.skillId(), Catalog.SKILL_BY_ID.get(r.skillId()).name(), r.required(), r.level(), r.need(), r.ratio(), r.state().wire());
    }

    private static SkillLevelBody level(Results.SkillLevel l) {
        var skill = Catalog.SKILL_BY_ID.get(l.skillId());
        return new SkillLevelBody(
                l.skillId(),
                skill.name(),
                skill.category(),
                l.level(),
                l.confidence(),
                l.simulated(),
                l.contributions().stream()
                        .map(c -> new ContributionBody(c.evidenceId(), c.type().wire(), c.title(), c.date(), c.points(), c.detail()))
                        .toList());
    }

    private static GapBody gap(Results.Gap g) {
        var skill = Catalog.SKILL_BY_ID.get(g.skillId());
        return new GapBody(
                g.skillId(),
                skill.name(),
                skill.category(),
                g.current(),
                g.target(),
                g.gap(),
                g.weight(),
                g.status().wire(),
                g.missingPrerequisites(),
                g.impact(),
                g.confidence());
    }
}
