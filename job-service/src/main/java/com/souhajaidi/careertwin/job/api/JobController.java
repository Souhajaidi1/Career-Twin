package com.souhajaidi.careertwin.job.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.souhajaidi.careertwin.job.api.JobDtos.JobBody;
import com.souhajaidi.careertwin.job.api.JobDtos.JobRequest;
import com.souhajaidi.careertwin.job.api.JobDtos.JobSummary;
import com.souhajaidi.careertwin.job.service.JobService;
import com.souhajaidi.careertwin.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs", description = "Saved job postings and how well you match them")
public class JobController {

    private final JobService service;
    private final CurrentUser me;

    public JobController(JobService service, CurrentUser me) {
        this.service = service;
        this.me = me;
    }

    @PostMapping
    @Operation(summary = "Save a job posting", description = "The text is parsed into structured requirements. The raw text is kept untouched.")
    public ResponseEntity<JobBody> create(@Valid @RequestBody JobRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(me.requireId(), body));
    }

    @GetMapping
    public List<JobSummary> list(HttpServletRequest request) {
        return service.list(me.requireId(), bearer(request));
    }

    @GetMapping("/{id}")
    public JobBody get(@PathVariable UUID id) {
        return service.get(me.requireId(), id);
    }

    @GetMapping("/{id}/match")
    @Operation(summary = "Score this posting against your evidence")
    public JsonNode match(@PathVariable UUID id, HttpServletRequest request) {
        return service.match(me.requireId(), id, bearer(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(me.requireId(), id);
    }

    /** The filter chain has already verified this header; it is forwarded, never inspected. */
    private static String bearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    }
}
