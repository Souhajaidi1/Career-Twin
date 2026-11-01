package com.souhajaidi.careertwin.engine;

/** Kinds of evidence and how much a single record may contribute to a skill. */
public enum EvidenceType {
    EXPERIENCE("Work experience", 48, "stated"),
    PROJECT("Project", 30, "stated"),
    CERTIFICATION("Certification", 22, "stated"),
    EDUCATION("Education", 18, "stated"),
    REPOSITORY("Repository", 26, "imported"),
    ASSESSMENT("Assessment result", 38, "measured"),
    INTERVIEW("Interview practice", 12, "measured"),
    CLAIM("Self-reported", 14, "claimed");

    private final String label;
    private final int max;
    private final String trust;

    EvidenceType(String label, int max, String trust) {
        this.label = label;
        this.max = max;
        this.trust = trust;
    }

    public String label() {
        return label;
    }

    public int max() {
        return max;
    }

    public String trust() {
        return trust;
    }

    public String wire() {
        return name().toLowerCase();
    }

    public static EvidenceType fromWire(String value) {
        return valueOf(value.toUpperCase());
    }
}
