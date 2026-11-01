package com.souhajaidi.careertwin.career.domain;

import com.souhajaidi.careertwin.engine.EvidenceType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One thing the user can point to as proof of a skill. Owned by exactly one user. */
@Entity
@Table(name = "evidence")
public class EvidenceRecord {

    @Embeddable
    public static class SkillRef {
        @Column(name = "skill_id", nullable = false)
        private String skillId;

        @Column(nullable = false)
        private double weight;

        protected SkillRef() {}

        public SkillRef(String skillId, double weight) {
            this.skillId = skillId;
            this.weight = weight;
        }

        public String getSkillId() { return skillId; }
        public double getWeight() { return weight; }
    }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    private String org;

    @Column(name = "occurred")
    private LocalDate date;

    private Double months;
    private Integer score;
    private String note;

    @Column(nullable = false)
    private String origin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "evidence_skill", joinColumns = @JoinColumn(name = "evidence_id"))
    private List<SkillRef> skills = new ArrayList<>();

    protected EvidenceRecord() {}

    public EvidenceRecord(UUID userId, EvidenceType type, String origin) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type.wire();
        this.origin = origin;
    }

    public void apply(String title, String org, LocalDate date, Double months, Integer score, String note, List<SkillRef> skills) {
        this.title = title;
        this.org = org;
        this.date = date;
        this.months = months;
        this.score = score;
        this.note = note;
        this.skills.clear();
        this.skills.addAll(skills);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public EvidenceType type() { return EvidenceType.fromWire(type); }
    public String getTitle() { return title; }
    public String getOrg() { return org; }
    public LocalDate getDate() { return date; }
    public Double getMonths() { return months; }
    public Integer getScore() { return score; }
    public String getNote() { return note; }
    public String getOrigin() { return origin; }
    public List<SkillRef> getSkills() { return List.copyOf(skills); }
}
