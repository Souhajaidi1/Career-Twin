package com.souhajaidi.careertwin.engine;

/** A skill an evidence record supports, with how strongly (0..1). */
public record SkillLink(String skillId, double weight) {}
