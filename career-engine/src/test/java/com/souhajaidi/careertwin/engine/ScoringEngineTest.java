package com.souhajaidi.careertwin.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.souhajaidi.careertwin.engine.ScoringEngine.Signals;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pins the properties the product's claims rest on. These assert relationships ("a claim ranks
 * below a project") rather than magic numbers, so retuning a weight does not break them but
 * breaking the model does.
 */
class ScoringEngineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate RECENT = LocalDate.of(2026, 9, 1);
    private static final LocalDate OLD = LocalDate.of(2019, 9, 1);
    private static final Signals SIGNALS = new Signals(2.0, 0, 0);

    private final ScoringEngine engine = new ScoringEngine(CLOCK);

    private static Evidence evidence(String id, EvidenceType type, LocalDate date, Double months, Integer score, String... skills) {
        return new Evidence(
                id, type, id, date, months, score, false,
                Arrays.stream(skills).map(s -> new SkillLink(s, 1.0)).toList());
    }

    private int level(Evidence evidence, String skill) {
        return engine.skillLevels(List.of(evidence)).get(skill).level();
    }

    private int readiness(List<Evidence> evidence, String role, String seniority) {
        return engine.readiness(engine.skillLevels(evidence), role, seniority, evidence, SIGNALS).score();
    }

    @Nested
    @DisplayName("skill levels")
    class SkillLevels {

        @Test
        @DisplayName("no evidence means no levels and zero readiness")
        void emptyEvidence() {
            assertThat(engine.skillLevels(List.of())).isEmpty();
            assertThat(engine.readiness(engine.skillLevels(List.of()), "backend", "Mid-level", List.of(), new Signals(0, 0, 0)).score())
                    .isZero();
        }

        @Test
        @DisplayName("the same input always gives the same score")
        void deterministic() {
            List<Evidence> evidence = List.of(evidence("p", EvidenceType.PROJECT, RECENT, 6.0, null, "java", "spring"));
            assertThat(readiness(evidence, "backend", "Mid-level")).isEqualTo(readiness(evidence, "backend", "Mid-level"));
        }

        @Test
        @DisplayName("a self-reported claim ranks below a project, and a strong assessment above a claim")
        void trustOrdering() {
            int claim = level(evidence("c", EvidenceType.CLAIM, RECENT, null, null, "java"), "java");
            int project = level(evidence("p", EvidenceType.PROJECT, RECENT, 6.0, null, "java"), "java");
            int assessed = level(evidence("a", EvidenceType.ASSESSMENT, RECENT, null, 90, "java"), "java");
            assertThat(claim).isLessThan(project);
            assertThat(assessed).isGreaterThan(claim);
        }

        @Test
        @DisplayName("piling up evidence never reaches 100")
        void diminishingReturns() {
            List<Evidence> many = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                many.add(evidence("p" + i, EvidenceType.PROJECT, RECENT, 12.0, null, "java"));
            }
            assertThat(engine.skillLevels(many).get("java").level()).isLessThanOrEqualTo(99);
        }

        @Test
        @DisplayName("old evidence counts for less than recent evidence")
        void recency() {
            int fresh = level(evidence("n", EvidenceType.PROJECT, RECENT, 6.0, null, "java"), "java");
            int stale = level(evidence("o", EvidenceType.PROJECT, OLD, 6.0, null, "java"), "java");
            assertThat(stale).isLessThan(fresh);
        }

        @Test
        @DisplayName("every contribution is kept so the arithmetic can be replayed")
        void contributionsAreRetained() {
            List<Evidence> evidence = List.of(
                    evidence("a", EvidenceType.PROJECT, RECENT, 6.0, null, "java"),
                    evidence("b", EvidenceType.EXPERIENCE, RECENT, 12.0, null, "java"));
            assertThat(engine.skillLevels(evidence).get("java").contributions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("readiness and gaps")
    class Readiness {

        private final List<Evidence> base = List.of(evidence("p1", EvidenceType.PROJECT, RECENT, 6.0, null, "java", "spring"));

        @Test
        @DisplayName("adding evidence never lowers readiness")
        void monotonic() {
            List<Evidence> more = new ArrayList<>(base);
            more.add(evidence("p2", EvidenceType.PROJECT, RECENT, 4.0, null, "sql", "rest", "docker"));
            assertThat(readiness(more, "backend", "Mid-level")).isGreaterThanOrEqualTo(readiness(base, "backend", "Mid-level"));
        }

        @Test
        @DisplayName("an unknown role falls back to the default rather than failing")
        void unknownRole() {
            var result = engine.readiness(engine.skillLevels(base), "no-such-role", "Mid-level", base, SIGNALS);
            assertThat(result.role().id()).isEqualTo(Catalog.ROLES.get(0).id());
        }

        @Test
        @DisplayName("the same evidence is worth more against a junior role than a lead role")
        void seniorityRaisesTheBar() {
            assertThat(readiness(base, "backend", "Junior")).isGreaterThan(readiness(base, "backend", "Lead"));
        }

        @Test
        @DisplayName("gaps are ordered by impact, largest first")
        void gapsAreRanked() {
            var gaps = engine.gaps(engine.skillLevels(base), "backend", "Mid-level");
            for (int i = 1; i < gaps.size(); i++) {
                assertThat(gaps.get(i - 1).impact()).isGreaterThanOrEqualTo(gaps.get(i).impact());
            }
        }
    }

    @Nested
    @DisplayName("job parsing and matching")
    class Jobs {

        private final List<Evidence> base = List.of(evidence("p1", EvidenceType.PROJECT, RECENT, 6.0, null, "java", "spring"));

        @Test
        @DisplayName("'is a plus' marks a skill as preferred; an unqualified one is required")
        void requiredVersusPreferred() {
            var job = engine.parseJob("We need Java and Spring Boot.\nPostgreSQL required.\nKafka is a plus.");
            assertThat(job.skills().stream().filter(s -> s.skillId().equals("kafka")).findFirst().orElseThrow().required())
                    .isFalse();
            assertThat(job.skills().stream().filter(s -> s.skillId().equals("java")).findFirst().orElseThrow().required())
                    .isTrue();
        }

        @Test
        @DisplayName("a posting with no recognisable skills, or null text, parses to nothing instead of throwing")
        void degenerateInput() {
            assertThat(engine.parseJob("We are a friendly team.").skills()).isEmpty();
            assertThat(engine.parseJob(null).skills()).isEmpty();
        }

        @Test
        @DisplayName("'3+ years' is read as 3")
        void yearsOfExperience() {
            assertThat(engine.parseJob("3+ years of Java").years()).isEqualTo(3);
        }

        @Test
        @DisplayName("every required skill lands in exactly one bucket and the score stays in range")
        void matchIsWellFormed() {
            var job = engine.parseJob("Java, Spring Boot, PostgreSQL, Docker. Kafka is a plus.");
            var result = engine.match(job, engine.skillLevels(base), base, SIGNALS);
            assertThat(result.strong().size() + result.partial().size() + result.missing().size())
                    .isEqualTo(job.skills().size());
            assertThat(result.score()).isBetween(0, 100);
        }

        @Test
        @DisplayName("a posting with nothing measurable still yields a bounded score")
        void nothingToMeasure() {
            var result = engine.match(engine.parseJob("We are a friendly team."), engine.skillLevels(base), base, SIGNALS);
            assertThat(result.score()).isBetween(0, 100);
        }
    }

    @Nested
    @DisplayName("what-if simulation")
    class Simulation {

        private final List<Evidence> base = List.of(evidence("p1", EvidenceType.PROJECT, RECENT, 6.0, null, "java", "spring"));

        @Test
        @DisplayName("simulating nothing changes nothing")
        void noHypotheticals() {
            assertThat(engine.simulate(base, "backend", "Mid-level", SIGNALS, List.of()).delta()).isZero();
        }

        @Test
        @DisplayName("the delta is exactly simulated minus base, and a hypothetical assessment never lowers readiness")
        void deltaIsConsistent() {
            var sim = engine.simulate(
                    base, "backend", "Mid-level", SIGNALS,
                    List.of(new Results.Hypothetical("sql", EvidenceType.ASSESSMENT, 85, null)));
            assertThat(sim.delta()).isEqualTo(sim.simScore() - sim.baseScore());
            assertThat(sim.delta()).isGreaterThanOrEqualTo(0);
        }
    }
}
