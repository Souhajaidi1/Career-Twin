package com.souhajaidi.careertwin.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job")
public class Job {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false, columnDefinition = "text")
    private String raw;

    @Column(name = "requirements_json", nullable = false, columnDefinition = "text")
    private String requirementsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Job() {}

    public Job(UUID userId, String title, String company, String location, String url, String raw, String requirementsJson) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
        this.raw = raw;
        this.requirementsJson = requirementsJson;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getUrl() { return url; }
    public String getRaw() { return raw; }
    public String getRequirementsJson() { return requirementsJson; }
    public Instant getCreatedAt() { return createdAt; }
}
