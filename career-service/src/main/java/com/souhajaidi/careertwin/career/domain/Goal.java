package com.souhajaidi.careertwin.career.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goal")
public class Goal {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private String roleId;

    @Column(nullable = false)
    private String seniority;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Goal() {}

    public Goal(UUID userId, String roleId, String seniority) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.roleId = roleId;
        this.seniority = seniority;
    }

    public void deactivate() { this.active = false; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getRoleId() { return roleId; }
    public String getSeniority() { return seniority; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
