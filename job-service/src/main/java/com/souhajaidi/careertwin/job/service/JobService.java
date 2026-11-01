package com.souhajaidi.careertwin.job.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souhajaidi.careertwin.job.api.JobDtos.JobBody;
import com.souhajaidi.careertwin.job.api.JobDtos.JobRequest;
import com.souhajaidi.careertwin.job.api.JobDtos.JobSummary;
import com.souhajaidi.careertwin.job.domain.Job;
import com.souhajaidi.careertwin.job.repository.JobRepository;
import com.souhajaidi.careertwin.engine.Results.JobRequirements;
import com.souhajaidi.careertwin.engine.ScoringEngine;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobs;
    private final ScoringEngine engine;
    private final CareerClient career;
    private final ObjectMapper mapper;

    public JobService(JobRepository jobs, ScoringEngine engine, CareerClient career, ObjectMapper mapper) {
        this.jobs = jobs;
        this.engine = engine;
        this.career = career;
        this.mapper = mapper;
    }

    @Transactional
    public JobBody create(UUID userId, JobRequest request) {
        JobRequirements requirements = engine.parseJob(request.raw());
        Job job = new Job(
                userId,
                request.title().trim(),
                orEmpty(request.company()),
                orEmpty(request.location()),
                orEmpty(request.url()),
                request.raw(),
                write(requirements));
        return JobBody.from(jobs.save(job), mapper);
    }

    @Transactional(readOnly = true)
    public JobBody get(UUID userId, UUID id) {
        return JobBody.from(require(userId, id), mapper);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        jobs.delete(require(userId, id));
    }

    /** Lists the user's jobs with a match score each; a score is null when career-service is unreachable. */
    @Transactional(readOnly = true)
    public List<JobSummary> list(UUID userId, String bearerToken) {
        List<Job> mine = jobs.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, JobRequirements> requirements = new LinkedHashMap<>();
        for (Job job : mine) {
            requirements.put(job.getId().toString(), read(job));
        }
        Map<String, JsonNode> scores = mine.isEmpty() ? Map.of() : career.match(bearerToken, requirements);
        return mine.stream()
                .map(job -> {
                    JsonNode match = scores.get(job.getId().toString());
                    Integer score = match != null && match.has("score") ? match.get("score").asInt() : null;
                    return new JobSummary(job.getId(), job.getTitle(), job.getCompany(), job.getLocation(), job.getCreatedAt(), score);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public JsonNode match(UUID userId, UUID id, String bearerToken) {
        Job job = require(userId, id);
        Map<String, JsonNode> result = career.match(bearerToken, Map.of(job.getId().toString(), read(job)));
        JsonNode match = result.get(job.getId().toString());
        if (match == null) {
            throw JobException.analysisUnavailable();
        }
        return match;
    }

    /** Scoped by owner in the query, so another user's job id is indistinguishable from a missing one. */
    private Job require(UUID userId, UUID id) {
        return jobs.findByIdAndUserId(id, userId).orElseThrow(JobException::notFound);
    }

    private JobRequirements read(Job job) {
        try {
            return mapper.readValue(job.getRequirementsJson(), JobRequirements.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("stored requirements for job " + job.getId() + " are unreadable", ex);
        }
    }

    private String write(JobRequirements requirements) {
        try {
            return mapper.writeValueAsString(requirements);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("could not serialise parsed requirements", ex);
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
