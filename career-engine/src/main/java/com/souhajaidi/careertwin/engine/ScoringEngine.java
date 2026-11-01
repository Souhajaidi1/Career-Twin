package com.souhajaidi.careertwin.engine;

import com.souhajaidi.careertwin.engine.Results.Contribution;
import com.souhajaidi.careertwin.engine.Results.Gap;
import com.souhajaidi.careertwin.engine.Results.GapStatus;
import com.souhajaidi.careertwin.engine.Results.Hypothetical;
import com.souhajaidi.careertwin.engine.Results.JobRequirements;
import com.souhajaidi.careertwin.engine.Results.JobSkill;
import com.souhajaidi.careertwin.engine.Results.MatchCategory;
import com.souhajaidi.careertwin.engine.Results.MatchResult;
import com.souhajaidi.careertwin.engine.Results.MatchState;
import com.souhajaidi.careertwin.engine.Results.OtherRequirement;
import com.souhajaidi.careertwin.engine.Results.Readiness;
import com.souhajaidi.careertwin.engine.Results.ReadinessPart;
import com.souhajaidi.careertwin.engine.Results.RoadmapItem;
import com.souhajaidi.careertwin.engine.Results.ScoredRequirement;
import com.souhajaidi.careertwin.engine.Results.SimulatedSkill;
import com.souhajaidi.careertwin.engine.Results.Simulation;
import com.souhajaidi.careertwin.engine.Results.SkillLevel;
import com.souhajaidi.careertwin.engine.Results.SkillMatch;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every number the product shows is produced here, from stored records only.
 * No language model may write a score. The engine has no framework dependency
 * and takes a {@link Clock}, so every result is reproducible in a test.
 */
public final class ScoringEngine {

    public static final String VERSION = "scoring-1.4.0";

    private static final double AVG_MONTH_DAYS = 30.44;
    private static final Pattern SOFT_HINT =
            Pattern.compile("(nice to have|plus|bonus|preferred|advantage|a plus|desirable|would be)", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEARS = Pattern.compile(
            "(\\d{1,2})\\s*\\+?\\s*(?:-|to|\\u2013)?\\s*(?:\\d{1,2})?\\s*(?:\\+)?\\s*(?:years?|ans|yrs)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEGREE = Pattern.compile(
            "(bachelor|master|b\\.?sc|m\\.?sc|degree in|engineering degree|computer science|licence|ing\\u00e9nieur)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEGREE_LINE =
            Pattern.compile("[^.\\n]*(bachelor|master|degree in|computer science)[^.\\n]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMBIGUOUS = Pattern.compile(
            "(fast[- ]paced|rockstar|ninja|wear many hats|self[- ]starter|passion)", Pattern.CASE_INSENSITIVE);

    private record OtherRule(String skillId, Pattern pattern, String label) {}

    private static final List<OtherRule> OTHER_RULES = List.of(
            new OtherRule("comm", ci("(communicat|stakeholder|english|pr\\u00e9sentation|presentation|documentation)"), "Communication"),
            new OtherRule("agile", ci("(agile|scrum|sprint|kanban)"), "Agile delivery"),
            new OtherRule("lead", ci("(mentor|lead a team|leadership|ownership|autonom)"), "Leadership / ownership"),
            new OtherRule("problem", ci("(problem solving|analytical|troubleshoot|debug)"), "Problem solving"),
            new OtherRule("testing", ci("(test|qa|quality)"), "Quality & testing"));

    /** Facts about the person that the readiness and match formulas need besides evidence. */
    public record Signals(double experienceYears, int assessmentCount, int completedInterviews) {}

    private final Clock clock;

    public ScoringEngine(Clock clock) {
        this.clock = clock;
    }

    private static Pattern ci(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int round(double v) {
        return (int) Math.round(v);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Whole-and-fractional months between a date and today; undated evidence counts as 12. */
    double monthsSince(LocalDate date) {
        if (date == null) {
            return 12;
        }
        return Math.max(0, ChronoUnit.DAYS.between(date, today()) / AVG_MONTH_DAYS);
    }

    // ---- job description -> structured requirements --------------------------------

    public JobRequirements parseJob(String raw) {
        String text = raw == null ? "" : raw;
        List<String> lines = new ArrayList<>();
        for (String l : text.split("\\n|\\u2022|;|(?<=[.!?])\\s+")) {
            if (!l.trim().isEmpty()) {
                lines.add(l.trim());
            }
        }

        List<JobSkill> skills = new ArrayList<>();
        for (SkillNormalizer.Hit hit : SkillNormalizer.findSkills(text)) {
            List<String> ctx = lines.stream()
                    .filter(l -> hit.terms().stream().anyMatch(t -> l.toLowerCase().contains(t)))
                    .toList();
            boolean preferred = !ctx.isEmpty() && ctx.stream().allMatch(l -> SOFT_HINT.matcher(l).find());
            String quote = ctx.isEmpty() ? "" : ctx.get(0).substring(0, Math.min(160, ctx.get(0).length()));
            skills.add(new JobSkill(hit.skillId(), !preferred, hit.hits(), quote));
        }

        Matcher ym = YEARS.matcher(text);
        Integer years = ym.find() ? (int) clamp(Integer.parseInt(ym.group(1)), 0, 20) : null;

        String degree = null;
        if (DEGREE.matcher(text).find()) {
            Matcher dl = DEGREE_LINE.matcher(text);
            String line = dl.find() ? dl.group().trim() : "";
            degree = line.substring(0, Math.min(140, line.length()));
        }

        List<OtherRequirement> others = OTHER_RULES.stream()
                .filter(r -> r.pattern().matcher(text).find())
                .map(r -> new OtherRequirement(r.skillId(), r.label()))
                .toList();

        List<String> ambiguous = lines.stream()
                .filter(l -> AMBIGUOUS.matcher(l).find())
                .limit(4)
                .toList();

        String lower = text.toLowerCase(Locale.ROOT);
        String seniority = Pattern.compile("(senior|lead|principal|staff)").matcher(lower).find()
                ? "Senior"
                : Pattern.compile("(junior|entry|graduate|d\\u00e9butant)").matcher(lower).find() ? "Junior" : "Mid-level";

        return new JobRequirements(skills, years, degree, others, ambiguous, seniority);
    }

    // ---- evidence -> skill levels ---------------------------------------------------

    /**
     * level = 100 * (1 - exp(-sum of contributions / 70)). Every term is retained so the
     * drawer can replay the arithmetic. Diminishing returns mean repeated weak evidence
     * never fakes mastery.
     */
    public Map<String, SkillLevel> skillLevels(List<Evidence> evidence) {
        Map<String, List<Contribution>> bySkill = new LinkedHashMap<>();
        for (Evidence ev : evidence) {
            EvidenceType spec = ev.type();
            double rec = ev.date() != null ? clamp(1 - monthsSince(ev.date()) / 96, 0.55, 1) : 0.8;
            for (SkillLink link : ev.skills()) {
                if (!Catalog.SKILL_BY_ID.containsKey(link.skillId())) {
                    continue;
                }
                double w = link.weight();
                int span = spec == EvidenceType.EXPERIENCE ? 24 : 6;
                boolean timed = spec == EvidenceType.EXPERIENCE
                        || spec == EvidenceType.PROJECT
                        || spec == EvidenceType.REPOSITORY;
                double dur = timed
                        ? clamp(
                                (ev.months() != null ? ev.months() : span * 0.6) / span,
                                spec == EvidenceType.EXPERIENCE ? 0.35 : 0.55,
                                1.15)
                        : 1;
                boolean scored = spec == EvidenceType.ASSESSMENT || spec == EvidenceType.INTERVIEW;
                double base = scored
                        ? spec.max() * ((ev.score() != null ? ev.score() : 0) / 100.0)
                        : spec.max() * dur;
                double pts = base * w * rec;
                String detail = String.format(
                        Locale.ROOT, "%s · cap %d · weight %s · recency ×%.2f",
                        spec.label(), spec.max(), trim(w), rec);
                bySkill.computeIfAbsent(link.skillId(), k -> new ArrayList<>())
                        .add(new Contribution(
                                ev.id(), spec, ev.title(), ev.date(),
                                Math.round(pts * 10) / 10.0, w, Math.round(rec * 100) / 100.0, ev.simulated(), detail));
            }
        }

        Map<String, SkillLevel> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<Contribution>> e : bySkill.entrySet()) {
            List<Contribution> c = e.getValue();
            double raw = c.stream().mapToDouble(Contribution::points).sum();
            int level = (int) clamp(round(100 * (1 - Math.exp(-raw / 70))), 0, 99);
            Set<EvidenceType> kinds = new HashSet<>();
            c.forEach(x -> kinds.add(x.type()));
            boolean measured = c.stream().anyMatch(x -> x.type() == EvidenceType.ASSESSMENT || x.type() == EvidenceType.INTERVIEW);
            boolean fresh = c.stream().anyMatch(x -> monthsSince(x.date()) < 18);
            int confidence = (int) clamp(round(28 + kinds.size() * 16 + (measured ? 22 : 0) + (fresh ? 14 : 0)), 10, 96);
            LocalDate last = c.stream().map(Contribution::date).filter(d -> d != null).max(Comparator.naturalOrder()).orElse(null);
            out.put(e.getKey(), new SkillLevel(e.getKey(), level, confidence, last, c.stream().anyMatch(Contribution::simulated), List.copyOf(c)));
        }
        return out;
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // ---- career readiness -----------------------------------------------------------

    public Readiness readiness(Map<String, SkillLevel> levels, String roleId, String seniority, List<Evidence> evidence, Signals signals) {
        Role role = Catalog.roleOrDefault(roleId);
        String sen = seniority == null ? "Mid-level" : seniority;
        double mult = Catalog.seniorityMultiplier(sen);
        List<ScoredRequirement> reqs = role.requirements().stream()
                .map(r -> new ScoredRequirement(r.skillId(), (int) clamp(round(r.target() * mult), 25, 95), r.weight()))
                .toList();

        double totalWeight = reqs.stream().mapToDouble(ScoredRequirement::weight).sum();
        double cover = reqs.stream()
                        .mapToDouble(r -> r.weight() * Math.min(1, levelOf(levels, r.skillId()) / (double) r.target()))
                        .sum()
                / totalWeight;
        double years = signals.experienceYears();
        double expFit = clamp(years / Math.max(1, role.typicalYears() * (mult > 1 ? 1.4 : 1)), 0, 1);
        long nonClaim = evidence.stream().filter(e -> e.type() != EvidenceType.CLAIM).count();
        double depth = clamp(nonClaim / 8.0, 0, 1);
        double practice = clamp((signals.assessmentCount() * 0.55 + signals.completedInterviews() * 0.55) / 4, 0, 1);
        int score = round(100 * (0.55 * cover + 0.20 * expFit + 0.15 * depth + 0.10 * practice));

        List<ReadinessPart> parts = List.of(
                new ReadinessPart("cover", "Target-skill coverage", 55, cover,
                        reqs.size() + " skills required by " + role.name() + ", each capped at its target level"),
                new ReadinessPart("exp", "Experience fit", 20, expFit,
                        trim(years) + " yr recorded vs " + role.typicalYears() + " yr typical for this role at " + sen),
                new ReadinessPart("depth", "Evidence depth", 15, depth,
                        nonClaim + " of 8 non-self-reported evidence records"),
                new ReadinessPart("practice", "Practice signal", 10, practice,
                        signals.assessmentCount() + " assessments · " + signals.completedInterviews() + " interviews"));
        return new Readiness(score, role, reqs, parts);
    }

    private static int levelOf(Map<String, SkillLevel> levels, String skillId) {
        SkillLevel l = levels.get(skillId);
        return l == null ? 0 : l.level();
    }

    // ---- gaps -----------------------------------------------------------------------

    public List<Gap> gaps(Map<String, SkillLevel> levels, String roleId, String seniority) {
        Role role = Catalog.roleOrDefault(roleId);
        double mult = Catalog.seniorityMultiplier(seniority == null ? "Mid-level" : seniority);
        List<Gap> out = new ArrayList<>();
        for (Role.Requirement r : role.requirements()) {
            int target = (int) clamp(round(r.target() * mult), 25, 95);
            int cur = levelOf(levels, r.skillId());
            int gap = Math.max(0, target - cur);
            GapStatus status = gap <= 0 ? GapStatus.MET : gap < 20 ? GapStatus.MINOR : gap < 40 ? GapStatus.MAJOR : GapStatus.CRITICAL;
            List<String> missing = Catalog.SKILL_BY_ID.get(r.skillId()).prerequisites().stream()
                    .filter(p -> levelOf(levels, p) < 40)
                    .toList();
            SkillLevel sl = levels.get(r.skillId());
            out.add(new Gap(r.skillId(), cur, target, gap, r.weight(), status, missing, gap * r.weight(), sl == null ? 0 : sl.confidence()));
        }
        out.sort(Comparator.comparingDouble(Gap::impact).reversed());
        return out;
    }

    // ---- job match ------------------------------------------------------------------

    /** Fixed weights, renormalised when a category has no data in the posting. */
    public MatchResult match(JobRequirements req, Map<String, SkillLevel> levels, List<Evidence> evidence, Signals signals) {
        double mult = Catalog.seniorityMultiplier(req.seniority());
        int reqLevel = (int) clamp(round(62 * mult), 45, 85);

        List<SkillMatch> rows = new ArrayList<>();
        for (JobSkill s : req.skills()) {
            int lv = levelOf(levels, s.skillId());
            double ratio = Math.min(1, lv / (double) reqLevel);
            MatchState st = ratio >= 1 ? MatchState.STRONG : ratio >= 0.55 ? MatchState.PARTIAL : MatchState.MISSING;
            SkillLevel sl = levels.get(s.skillId());
            rows.add(new SkillMatch(s.skillId(), s.required(), lv, reqLevel, ratio, st, sl == null ? 0 : sl.confidence()));
        }
        double totalW = rows.stream().mapToDouble(r -> r.required() ? 1 : 0.5).sum();
        if (totalW == 0) {
            totalW = 1;
        }
        final double tw = totalW;
        double technical = rows.stream().mapToDouble(r -> (r.required() ? 1 : 0.5) * r.ratio()).sum() / tw;

        Double experience = req.years() == null ? null : clamp(signals.experienceYears() / req.years(), 0, 1);
        boolean hasDegree = evidence.stream().anyMatch(e -> e.type() == EvidenceType.EDUCATION);
        Double education = req.degree() == null ? null : (hasDegree ? 1.0 : 0.35);

        Set<String> jobIds = new HashSet<>();
        req.skills().forEach(s -> jobIds.add(s.skillId()));
        long relevant = evidence.stream()
                .filter(e -> e.type() == EvidenceType.PROJECT || e.type() == EvidenceType.REPOSITORY)
                .filter(e -> e.skills().stream().anyMatch(s -> jobIds.contains(s.skillId())))
                .count();
        int needed = (int) clamp(Math.ceil(req.skills().size() / 4.0), 1, 4);
        Double projects = req.skills().isEmpty() ? null : clamp(relevant / (double) needed, 0, 1);

        Double other = null;
        if (!req.others().isEmpty()) {
            long ok = req.others().stream().filter(o -> levelOf(levels, o.skillId()) >= 50).count();
            other = ok / (double) req.others().size();
        }

        record Def(String key, String label, int weight, Double value) {}
        List<Def> defs = List.of(
                new Def("technical", "Technical skills", 35, technical),
                new Def("experience", "Experience", 20, experience),
                new Def("education", "Education", 15, education),
                new Def("projects", "Project evidence", 15, projects),
                new Def("other", "Other requirements", 15, other));
        List<Def> live = defs.stream().filter(d -> d.value() != null).toList();
        int totalWeight = live.stream().mapToInt(Def::weight).sum();
        if (totalWeight == 0) {
            totalWeight = 1;
        }
        final int totalWeightFinal = totalWeight;
        List<MatchCategory> cats = live.stream()
                .map(d -> new MatchCategory(
                        d.key(), d.label(), d.weight(), round(d.weight() / (double) totalWeightFinal * 100), d.value(),
                        d.value() * (d.weight() / (double) totalWeightFinal) * 100))
                .toList();
        int score = round(cats.stream().mapToDouble(MatchCategory::points).sum());
        List<String> excluded = defs.stream().filter(d -> d.value() == null).map(Def::label).toList();

        long withEvidence = rows.stream()
                .filter(r -> levels.containsKey(r.skillId()) && !levels.get(r.skillId()).contributions().isEmpty())
                .count();
        int completeness = round(100 * clamp(
                0.5 * (rows.isEmpty() ? 0 : withEvidence / (double) rows.size())
                        + 0.2 * clamp(evidence.size() / 6.0, 0, 1)
                        + 0.15 * (req.years() != null ? 1 : 0)
                        + 0.15 * (req.degree() != null ? 1 : 0),
                0, 1));

        return new MatchResult(
                score, completeness, reqLevel, cats, excluded,
                rows.stream().filter(r -> r.state() == MatchState.STRONG).toList(),
                rows.stream().filter(r -> r.state() == MatchState.PARTIAL).toList(),
                rows.stream().filter(r -> r.state() == MatchState.MISSING).toList());
    }

    // ---- simulation -----------------------------------------------------------------

    /** Re-runs the same functions with extra, never-stored evidence. */
    public Simulation simulate(List<Evidence> evidence, String roleId, String seniority, Signals signals, List<Hypothetical> hypotheticals) {
        Map<String, SkillLevel> base = skillLevels(evidence);
        List<Evidence> extra = new ArrayList<>();
        int i = 0;
        for (Hypothetical h : hypotheticals) {
            extra.add(new Evidence(
                    "sim-" + i++, h.type(), "simulated", today(), h.months(), h.score(), true,
                    List.of(new SkillLink(h.skillId(), 1.0))));
        }
        List<Evidence> all = new ArrayList<>(evidence);
        all.addAll(extra);
        Map<String, SkillLevel> sim = skillLevels(all);

        int baseScore = readiness(base, roleId, seniority, evidence, signals).score();
        int simScore = readiness(sim, roleId, seniority, all, signals).score();

        List<SimulatedSkill> changed = new ArrayList<>();
        for (Hypothetical h : hypotheticals) {
            changed.add(new SimulatedSkill(h.skillId(), levelOf(base, h.skillId()), levelOf(sim, h.skillId())));
        }
        return new Simulation(baseScore, simScore, simScore - baseScore, changed);
    }

    // ---- roadmap --------------------------------------------------------------------

    /**
     * One item per real gap, ordered so prerequisites come first. The readiness delta is
     * recomputed by simulating the evidence the step would produce, not invented.
     */
    public List<RoadmapItem> roadmap(List<Evidence> evidence, String roleId, String seniority, Signals signals, double hoursPerWeek) {
        Map<String, SkillLevel> levels = skillLevels(evidence);
        List<Gap> gaps = gaps(levels, roleId, seniority).stream().filter(g -> g.gap() > 8).toList();
        Map<String, Gap> gapById = new LinkedHashMap<>();
        gaps.forEach(g -> gapById.put(g.skillId(), g));

        List<Gap> ordered = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        for (Gap g : gaps) {
            place(g, gapById, placed, ordered);
        }

        Role role = Catalog.roleOrDefault(roleId);
        int baseScore = readiness(levels, roleId, seniority, evidence, signals).score();
        List<RoadmapItem> out = new ArrayList<>();
        int cum = 0;
        int limit = Math.min(9, ordered.size());
        for (int i = 0; i < limit; i++) {
            Gap g = ordered.get(i);
            Skill skill = Catalog.SKILL_BY_ID.get(g.skillId());
            int hours = (int) clamp(round(g.gap() * 0.85 / 5) * 5, 10, 90);
            cum += hours;
            boolean human = skill.category().equals("Human") || skill.category().equals("Ways of working");
            String produces = human ? "interview" : g.gap() >= 40 ? "assessment" : "project";
            EvidenceType simType = produces.equals("assessment") ? EvidenceType.ASSESSMENT : EvidenceType.PROJECT;

            List<Evidence> withStep = new ArrayList<>(evidence);
            withStep.add(new Evidence("sim", simType, "simulated", today(), 6.0, 75, true, List.of(new SkillLink(g.skillId(), 1.0))));
            Map<String, SkillLevel> simLevels = skillLevels(withStep);
            int delta = readiness(simLevels, roleId, seniority, withStep, signals).score() - baseScore;

            out.add(new RoadmapItem(
                    g.skillId(),
                    g.gap() >= 40 ? "Build working proficiency in " + skill.name() : "Close the " + skill.name() + " gap",
                    "Move " + skill.name() + " from an estimated " + g.current() + " to the " + g.target()
                            + " required by " + role.name() + ".",
                    hours,
                    (int) Math.ceil((cum - hours) / hoursPerWeek) + 1,
                    (int) Math.ceil(cum / hoursPerWeek),
                    i < 2 ? "High" : i < 5 ? "Medium" : "Low",
                    skill.prerequisites().stream().filter(gapById::containsKey).toList(),
                    produces,
                    g.gap(),
                    delta,
                    levelOf(simLevels, g.skillId()) - g.current()));
        }
        return out;
    }

    private void place(Gap g, Map<String, Gap> gapById, Set<String> placed, List<Gap> ordered) {
        if (placed.contains(g.skillId())) {
            return;
        }
        placed.add(g.skillId());
        for (String p : Catalog.SKILL_BY_ID.get(g.skillId()).prerequisites()) {
            Gap pg = gapById.get(p);
            if (pg != null && !placed.contains(p)) {
                place(pg, gapById, placed, ordered);
            }
        }
        ordered.add(g);
    }
}
