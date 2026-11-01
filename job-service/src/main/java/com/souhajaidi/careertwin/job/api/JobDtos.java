package com.souhajaidi.careertwin.job.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souhajaidi.careertwin.job.domain.Job;
import com.souhajaidi.careertwin.engine.Results.JobRequirements;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class JobDtos {

    private JobDtos() {}

    public record JobRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 160) String company,
            @Size(max = 160) String location,
            // http(s) only: this value is rendered as a link, and a javascript: URL must never get that far.
            @Pattern(regexp = "^$|^https?://\\S{1,490}$", message = "url must start with http:// or https://") String url,
            @NotBlank @Size(max = 20000) String raw) {}

    public record JobSummary(UUID id, String title, String company, String location, Instant createdAt, Integer score) {}

    public record SkillBody(String id, boolean required, int mentions, String quote) {}

    public record RequirementsBody(List<SkillBody> skills, Integer years, String degree, String seniority) {}

    public record JobBody(
            UUID id,
            String title,
            String company,
            String location,
            String url,
            String raw,
            Instant createdAt,
            RequirementsBody requirements) {

        public static JobBody from(Job job, ObjectMapper mapper) {
            try {
                JobRequirements r = mapper.readValue(job.getRequirementsJson(), JobRequirements.class);
                RequirementsBody body = new RequirementsBody(
                        r.skills().stream()
                                .map(s -> new SkillBody(s.skillId(), s.required(), s.mentions(), s.quote()))
                                .toList(),
                        r.years(),
                        r.degree(),
                        r.seniority());
                return new JobBody(
                        job.getId(), job.getTitle(), job.getCompany(), job.getLocation(), job.getUrl(), job.getRaw(), job.getCreatedAt(), body);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("stored requirements for job " + job.getId() + " are unreadable", ex);
            }
        }
    }
}
