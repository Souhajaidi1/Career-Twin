package com.souhajaidi.careertwin.career.api;

import com.souhajaidi.careertwin.career.api.dto.Dtos.*;
import com.souhajaidi.careertwin.career.service.AnalysisService;
import com.souhajaidi.careertwin.career.service.EvidenceService;
import com.souhajaidi.careertwin.career.service.GoalService;
import com.souhajaidi.careertwin.career.service.ProfileService;
import com.souhajaidi.careertwin.engine.Catalog;
import com.souhajaidi.careertwin.engine.EvidenceType;
import com.souhajaidi.careertwin.engine.Results;
import com.souhajaidi.careertwin.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** All endpoints act on the authenticated user's own records; there is no user id in any path. */
public final class CareerControllers {

    private CareerControllers() {}

    @RestController
    @RequestMapping("/api/v1/profile")
    @Tag(name = "Profile")
    public static class ProfileController {
        private final ProfileService service;
        private final CurrentUser me;

        public ProfileController(ProfileService service, CurrentUser me) {
            this.service = service;
            this.me = me;
        }

        @GetMapping
        @Operation(summary = "Read your profile")
        public ProfileBody get() {
            return service.get(me.requireId());
        }

        @PutMapping
        @Operation(summary = "Replace your profile")
        public ProfileBody put(@Valid @RequestBody ProfileBody body) {
            return service.put(me.requireId(), body);
        }
    }

    @RestController
    @RequestMapping("/api/v1/evidence")
    @Tag(name = "Evidence", description = "Records that back each skill level")
    public static class EvidenceController {
        private final EvidenceService service;
        private final CurrentUser me;

        public EvidenceController(EvidenceService service, CurrentUser me) {
            this.service = service;
            this.me = me;
        }

        @GetMapping
        public List<EvidenceResponse> list() {
            return service.list(me.requireId());
        }

        @PostMapping
        @Operation(summary = "Add evidence", description = "assessment and interview types are reserved for in-product tests.")
        public ResponseEntity<EvidenceResponse> create(@Valid @RequestBody EvidenceRequest body) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(me.requireId(), body));
        }

        @PutMapping("/{id}")
        public EvidenceResponse update(@PathVariable UUID id, @Valid @RequestBody EvidenceRequest body) {
            return service.update(me.requireId(), id, body);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@PathVariable UUID id) {
            service.delete(me.requireId(), id);
        }
    }

    @RestController
    @RequestMapping("/api/v1/goals")
    @Tag(name = "Goals")
    public static class GoalController {
        private final GoalService service;
        private final CurrentUser me;

        public GoalController(GoalService service, CurrentUser me) {
            this.service = service;
            this.me = me;
        }

        @PostMapping
        @Operation(summary = "Set your target role and seniority", description = "Replaces any previous active goal.")
        public ResponseEntity<GoalResponse> set(@Valid @RequestBody GoalRequest body) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.set(me.requireId(), body));
        }
    }

    @RestController
    @RequestMapping("/api/v1/catalog")
    @Tag(name = "Catalog")
    public static class CatalogController {

        @GetMapping
        @Operation(summary = "Skills, roles and evidence types the engine understands")
        public CatalogResponse get() {
            return new CatalogResponse(
                    Catalog.SKILLS.stream()
                            .map(s -> new CatalogSkill(s.id(), s.name(), s.category(), s.prerequisites()))
                            .toList(),
                    Catalog.ROLES.stream()
                            .map(r -> new CatalogRole(
                                    r.id(),
                                    r.name(),
                                    r.typicalYears(),
                                    r.requirements().stream()
                                            .map(q -> new CatalogRequirement(q.skillId(), q.target(), q.weight()))
                                            .toList()))
                            .toList(),
                    Catalog.SENIORITIES,
                    // Only types a user may enter by hand are advertised.
                    Arrays.stream(EvidenceType.values())
                            .filter(EvidenceService::userCreatable)
                            .map(t -> new CatalogEvidenceType(t.wire(), t.label(), t.max(), t.trust()))
                            .toList());
        }
    }

    @RestController
    @RequestMapping("/api/v1/analysis")
    @Tag(name = "Analysis", description = "Deterministic scores computed from your stored evidence")
    @Validated
    public static class AnalysisController {
        private final AnalysisService service;
        private final CurrentUser me;

        public AnalysisController(AnalysisService service, CurrentUser me) {
            this.service = service;
            this.me = me;
        }

        @GetMapping("/twin")
        @Operation(summary = "Skill levels, readiness for your goal, and ranked gaps")
        public TwinResponse twin() {
            return service.twin(me.requireId());
        }

        @GetMapping("/roadmap")
        public List<Results.RoadmapItem> roadmap(
                @RequestParam(defaultValue = "6") @DecimalMin("1") @DecimalMax("60") double hoursPerWeek) {
            return service.roadmap(me.requireId(), hoursPerWeek);
        }

        @PostMapping("/simulate")
        @Operation(summary = "What would my readiness be if I added this evidence?")
        public SimulationResponse simulate(@Valid @RequestBody SimulationRequest body) {
            return service.simulate(me.requireId(), body);
        }

        @PostMapping("/match")
        @Operation(summary = "Score parsed job requirements against you", description = "Called by job-service with your own bearer token.")
        public MatchBatchResponse match(@Valid @RequestBody MatchBatchRequest body) {
            return service.match(me.requireId(), body);
        }
    }
}
