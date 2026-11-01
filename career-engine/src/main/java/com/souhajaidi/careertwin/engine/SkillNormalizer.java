package com.souhajaidi.careertwin.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds canonical skills in free text by matching their aliases on word boundaries. */
public final class SkillNormalizer {

    public record Hit(String skillId, int hits, List<String> terms) {}

    private SkillNormalizer() {}

    public static List<Hit> findSkills(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String t = " " + text.toLowerCase().replaceAll("[^a-z0-9+#./ -]", " ").replaceAll("\\s+", " ") + " ";
        List<Hit> out = new ArrayList<>();
        for (Skill skill : Catalog.SKILLS) {
            int hits = 0;
            List<String> terms = new ArrayList<>();
            for (String alias : skill.aliases()) {
                Pattern p = Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(alias) + "([^a-z0-9]|$)");
                Matcher m = p.matcher(t);
                int n = 0;
                while (m.find()) {
                    n++;
                }
                if (n > 0) {
                    hits += n;
                    terms.add(alias);
                }
            }
            if (hits > 0) {
                out.add(new Hit(skill.id(), hits, terms));
            }
        }
        out.sort(Comparator.comparingInt(Hit::hits).reversed());
        return out;
    }
}
