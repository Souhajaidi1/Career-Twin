package com.souhajaidi.careertwin.career.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile")
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String name = "";

    @Column(name = "current_title", nullable = false)
    private String currentRole = "";

    @Column(name = "experience_years", nullable = false)
    private double experienceYears;

    @Column(nullable = false)
    private String location = "";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Profile() {}

    public Profile(UUID userId) {
        this.userId = userId;
    }

    public void update(String name, String currentRole, double experienceYears, String location) {
        this.name = name;
        this.currentRole = currentRole;
        this.experienceYears = experienceYears;
        this.location = location;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getCurrentRole() { return currentRole; }
    public double getExperienceYears() { return experienceYears; }
    public String getLocation() { return location; }
}
