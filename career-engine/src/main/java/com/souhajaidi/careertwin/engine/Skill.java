package com.souhajaidi.careertwin.engine;

import java.util.List;

public record Skill(String id, String name, String category, List<String> aliases, List<String> prerequisites) {}
