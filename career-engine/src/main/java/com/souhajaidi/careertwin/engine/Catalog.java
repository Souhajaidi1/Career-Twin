package com.souhajaidi.careertwin.engine;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skills and roles as data, not code. Supporting a new profession means editing
 * these tables, not the engine.
 */
public final class Catalog {

    public static final List<String> SENIORITIES = List.of("Junior", "Mid-level", "Senior", "Lead");

    private static final Map<String, Double> SENIORITY_MULTIPLIER =
            Map.of("Junior", 0.75, "Mid-level", 1.0, "Senior", 1.18, "Lead", 1.3);

    public static final List<Skill> SKILLS = List.of(
            s("java", "Java", "Languages", "java,jdk,j2ee,java 8,java 11,java 17", ""),
            s("python", "Python", "Languages", "python,django,flask,fastapi,pandas", ""),
            s("ts", "TypeScript / JavaScript", "Languages", "typescript,javascript,node,node.js,nodejs,es6", ""),
            s("go", "Go", "Languages", "golang,go lang", ""),
            s("spring", "Spring Boot", "Backend", "spring,spring boot,springboot,spring mvc,spring security", "java"),
            s("rest", "REST APIs", "Backend", "rest,rest api,restful,api design,http api,web services", ""),
            s("graphql", "GraphQL", "Backend", "graphql,apollo", "rest"),
            s("sql", "SQL & Relational DB", "Data", "sql,postgresql,postgres,mysql,rdbms,relational database,oracle db", ""),
            s("nosql", "NoSQL", "Data", "nosql,mongodb,redis,cassandra,dynamodb,elasticsearch", ""),
            s("dataeng", "Data Engineering", "Data", "etl,data pipeline,spark,airflow,data engineering,dbt", "sql"),
            s("ml", "Machine Learning", "Data", "machine learning,deep learning,tensorflow,pytorch,scikit,llm", "python"),
            s("docker", "Docker", "DevOps", "docker,container,containerization,podman", ""),
            s("k8s", "Kubernetes", "DevOps", "kubernetes,k8s,eks,gke,aks,helm", "docker"),
            s("cicd", "CI/CD", "DevOps", "ci/cd,cicd,jenkins,github actions,gitlab ci,continuous integration,continuous delivery", ""),
            s("terraform", "Infrastructure as Code", "DevOps", "terraform,iac,infrastructure as code,cloudformation,ansible,pulumi", "aws"),
            s("aws", "AWS / Cloud", "Cloud", "aws,amazon web services,ec2,s3,lambda,cloud,azure,gcp", ""),
            s("linux", "Linux & Shell", "Platform", "linux,bash,shell,unix,ubuntu", ""),
            s("monitoring", "Observability", "Platform", "monitoring,observability,prometheus,grafana,logging,datadog,tracing", ""),
            s("micro", "Microservices", "Architecture", "microservices,microservice,service oriented,soa,domain driven", "rest"),
            s("sysdesign", "System Design", "Architecture", "system design,scalability,distributed systems,architecture,high availability,load balancing", "micro"),
            s("kafka", "Event Streaming", "Architecture", "kafka,event streaming,rabbitmq,message queue,pub/sub,event driven", ""),
            s("caching", "Caching & Performance", "Architecture", "caching,cache,cdn,memcached,performance tuning", ""),
            s("react", "React", "Frontend", "react,react.js,next.js,redux,frontend framework", "ts"),
            s("ux", "UI & Accessibility", "Frontend", "ui,css,tailwind,accessibility,wcag,responsive design", ""),
            s("testing", "Testing", "Quality", "testing,unit test,junit,jest,tdd,integration test,pytest,qa", ""),
            s("security", "Application Security", "Security", "security,owasp,authentication,authorization,oauth,jwt,encryption,pen test", ""),
            s("git", "Git & Version Control", "Platform", "git,github,gitlab,bitbucket,version control", ""),
            s("agile", "Agile Delivery", "Ways of working", "agile,scrum,kanban,sprint,jira", ""),
            s("comm", "Communication", "Human", "communication,stakeholder,presentation,documentation,collaboration,english", ""),
            s("lead", "Leadership & Mentoring", "Human", "leadership,mentoring,team lead,ownership,mentorship,coaching", ""),
            s("problem", "Problem Solving", "Human", "problem solving,analytical,debugging,troubleshooting,root cause", ""));

    public static final Map<String, Skill> SKILL_BY_ID = index();

    public static final List<Role> ROLES = List.of(
            role("backend", "Backend Engineer", 3,
                    r("java", 75, 1.5), r("spring", 72, 1.5), r("sql", 70, 1.3), r("rest", 75, 1.3), r("micro", 60, 1),
                    r("docker", 60, 1), r("sysdesign", 60, 1.2), r("testing", 58, 1), r("aws", 50, 1), r("security", 45, 1),
                    r("kafka", 45, 1), r("comm", 55, 1), r("git", 65, 1), r("problem", 60, 1)),
            role("devops", "DevOps Engineer", 3,
                    r("docker", 80, 1.5), r("k8s", 75, 1.5), r("aws", 75, 1.4), r("cicd", 78, 1.4), r("terraform", 65, 1.2),
                    r("linux", 72, 1.2), r("monitoring", 65, 1), r("security", 55, 1), r("python", 50, 1), r("git", 70, 1),
                    r("comm", 55, 1)),
            role("fullstack", "Full Stack Engineer", 3,
                    r("ts", 72, 1.4), r("react", 72, 1.4), r("rest", 70, 1.3), r("sql", 60, 1), r("java", 45, 1),
                    r("testing", 55, 1), r("docker", 45, 1), r("ux", 55, 1), r("git", 65, 1), r("comm", 60, 1)),
            role("frontend", "Frontend Engineer", 3,
                    r("ts", 78, 1.5), r("react", 80, 1.5), r("ux", 70, 1.3), r("testing", 55, 1), r("rest", 55, 1),
                    r("caching", 45, 1), r("git", 65, 1), r("comm", 60, 1)),
            role("cloud", "Cloud Engineer", 4,
                    r("aws", 82, 1.5), r("terraform", 70, 1.4), r("k8s", 68, 1.3), r("docker", 70, 1.2), r("linux", 68, 1),
                    r("monitoring", 62, 1), r("security", 60, 1), r("cicd", 65, 1), r("comm", 55, 1)),
            role("data", "Data Scientist", 3,
                    r("python", 78, 1.5), r("sql", 75, 1.4), r("ml", 65, 1.4), r("dataeng", 60, 1.2), r("comm", 65, 1.2),
                    r("testing", 40, 1), r("problem", 65, 1)),
            role("secops", "Cybersecurity Engineer", 4,
                    r("security", 82, 1.6), r("linux", 68, 1.2), r("monitoring", 62, 1), r("aws", 58, 1), r("python", 55, 1),
                    r("testing", 50, 1), r("comm", 58, 1)),
            role("pm", "Product Manager", 3,
                    r("comm", 80, 1.6), r("lead", 70, 1.3), r("agile", 75, 1.3), r("problem", 68, 1.2), r("sql", 42, 1),
                    r("ux", 50, 1)));

    public static final Map<String, Role> ROLE_BY_ID = indexRoles();

    private Catalog() {}

    public static double seniorityMultiplier(String seniority) {
        return SENIORITY_MULTIPLIER.getOrDefault(seniority, 1.0);
    }

    public static Role roleOrDefault(String roleId) {
        return ROLE_BY_ID.getOrDefault(roleId, ROLES.get(0));
    }

    private static Skill s(String id, String name, String category, String aliases, String prerequisites) {
        return new Skill(id, name, category, split(aliases), split(prerequisites));
    }

    private static List<String> split(String csv) {
        return csv.isEmpty() ? List.of() : Arrays.asList(csv.split(","));
    }

    private static Role.Requirement r(String skillId, int target, double weight) {
        return new Role.Requirement(skillId, target, weight);
    }

    private static Role role(String id, String name, int years, Role.Requirement... reqs) {
        return new Role(id, name, years, List.of(reqs));
    }

    private static Map<String, Skill> index() {
        Map<String, Skill> m = new LinkedHashMap<>();
        SKILLS.forEach(sk -> m.put(sk.id(), sk));
        return Map.copyOf(m);
    }

    private static Map<String, Role> indexRoles() {
        Map<String, Role> m = new LinkedHashMap<>();
        ROLES.forEach(ro -> m.put(ro.id(), ro));
        return Map.copyOf(m);
    }
}
