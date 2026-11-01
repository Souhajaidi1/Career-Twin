package com.souhajaidi.careertwin.engine;

import java.util.List;

/** A target role: typical years of experience and the skill levels it expects. */
public record Role(String id, String name, int typicalYears, List<Requirement> requirements) {

    /** {@code target} is the level (0..99) expected at mid-level; {@code weight} is its importance. */
    public record Requirement(String skillId, int target, double weight) {}
}
