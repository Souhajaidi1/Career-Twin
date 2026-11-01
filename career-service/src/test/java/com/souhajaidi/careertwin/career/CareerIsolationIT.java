package com.souhajaidi.careertwin.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souhajaidi.careertwin.security.JwtVerifier;
import com.souhajaidi.careertwin.security.KeyCodec;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs the real service against a real PostgreSQL, so Flyway, constraints and security are all exercised. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CareerIsolationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final String[] KEYS = KeyCodec.generateBase64Pair();

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("careertwin.jwt.public-key", () -> KEYS[1]);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private static String tokenFor(UUID user) {
        return Jwts.builder()
                .issuer("careertwin-identity")
                .subject(user.toString())
                .claim(JwtVerifier.CLAIM_ROLE, "USER")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(KeyCodec.decodePrivate(KEYS[0]), Jwts.SIG.RS256)
                .compact();
    }

    private MvcResult call(MockHttpServletRequestBuilder request, UUID user, String body) throws Exception {
        if (user != null) {
            request = request.header("Authorization", "Bearer " + tokenFor(user));
        }
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request).andReturn();
    }

    private static final String PROJECT =
            "{\"type\":\"project\",\"title\":\"Wallet engine\",\"months\":3,\"date\":\"2026-10-01\","
                    + "\"skills\":[{\"id\":\"java\",\"weight\":1},{\"id\":\"spring\",\"weight\":1}]}";

    private JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("anonymous callers get 401, not data")
    void requiresAuthentication() throws Exception {
        assertThat(call(get("/api/v1/evidence"), null, null).getResponse().getStatus()).isEqualTo(401);
        assertThat(call(get("/api/v1/analysis/twin"), null, null).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("one user cannot see, change or delete another user's evidence")
    void evidenceIsIsolated() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        MvcResult created = call(post("/api/v1/evidence"), alice, PROJECT);
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        String id = read(created).get("id").asText();

        assertThat(read(call(get("/api/v1/evidence"), alice, null))).hasSize(1);
        assertThat(read(call(get("/api/v1/evidence"), bob, null))).isEmpty();

        assertThat(call(put("/api/v1/evidence/" + id), bob, PROJECT).getResponse().getStatus()).isEqualTo(404);
        assertThat(call(delete("/api/v1/evidence/" + id), bob, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(read(call(get("/api/v1/evidence"), alice, null))).hasSize(1);

        assertThat(call(delete("/api/v1/evidence/" + id), alice, null).getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("a user cannot award themselves measured evidence")
    void reservedTypesAreRefused() throws Exception {
        String forged = "{\"type\":\"assessment\",\"title\":\"Java test\",\"score\":100,"
                + "\"skills\":[{\"id\":\"java\",\"weight\":1}]}";
        MvcResult r = call(post("/api/v1/evidence"), UUID.randomUUID(), forged);
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(read(r).get("code").asText()).isEqualTo("RESERVED_EVIDENCE_TYPE");
    }

    @Test
    @DisplayName("the catalog does not advertise reserved evidence types")
    void catalogHidesReservedTypes() throws Exception {
        JsonNode types = read(call(get("/api/v1/catalog"), UUID.randomUUID(), null)).get("evidenceTypes");
        for (JsonNode t : types) {
            assertThat(t.get("type").asText()).isNotIn("assessment", "interview");
        }
    }

    @Test
    @DisplayName("analysis reflects only the caller's evidence, and a goal unlocks readiness")
    void analysisIsPerUser() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        call(post("/api/v1/evidence"), alice, PROJECT);

        JsonNode noGoal = read(call(get("/api/v1/analysis/twin"), alice, null));
        assertThat(noGoal.get("goal").isNull()).isTrue();
        assertThat(noGoal.get("levels")).isNotEmpty();

        MvcResult goal = call(post("/api/v1/goals"), alice, "{\"roleId\":\"backend\",\"seniority\":\"Mid-level\"}");
        assertThat(goal.getResponse().getStatus()).isEqualTo(201);

        JsonNode twin = read(call(get("/api/v1/analysis/twin"), alice, null));
        assertThat(twin.get("readiness").get("score").asInt()).isPositive();
        assertThat(twin.get("gaps")).isNotEmpty();

        JsonNode bobTwin = read(call(get("/api/v1/analysis/twin"), bob, null));
        assertThat(bobTwin.get("levels")).isEmpty();
        assertThat(bobTwin.get("goal").isNull()).isTrue();
    }

    @Test
    @DisplayName("setting a new goal replaces the old one")
    void onlyOneActiveGoal() throws Exception {
        UUID user = UUID.randomUUID();
        call(post("/api/v1/goals"), user, "{\"roleId\":\"backend\",\"seniority\":\"Junior\"}");
        call(post("/api/v1/goals"), user, "{\"roleId\":\"devops\",\"seniority\":\"Senior\"}");
        JsonNode goal = read(call(get("/api/v1/analysis/twin"), user, null)).get("goal");
        assertThat(goal.get("roleId").asText()).isEqualTo("devops");
        assertThat(goal.get("seniority").asText()).isEqualTo("Senior");
    }

    @Test
    @DisplayName("unknown roles and skills are rejected with a stable error code")
    void validatesAgainstCatalog() throws Exception {
        UUID user = UUID.randomUUID();
        MvcResult role = call(post("/api/v1/goals"), user, "{\"roleId\":\"wizard\",\"seniority\":\"Senior\"}");
        assertThat(role.getResponse().getStatus()).isEqualTo(400);
        assertThat(read(role).get("code").asText()).isEqualTo("UNKNOWN_ROLE");

        String bad = "{\"type\":\"project\",\"title\":\"x\",\"skills\":[{\"id\":\"cobol-9000\",\"weight\":1}]}";
        assertThat(read(call(post("/api/v1/evidence"), user, bad)).get("code").asText()).isEqualTo("UNKNOWN_SKILL");
    }

    @Test
    @DisplayName("simulating without a goal is a conflict, not a guess")
    void simulationNeedsGoal() throws Exception {
        String body = "{\"hypotheticals\":[{\"skillId\":\"docker\",\"type\":\"project\",\"months\":3}]}";
        assertThat(call(post("/api/v1/analysis/simulate"), UUID.randomUUID(), body).getResponse().getStatus())
                .isEqualTo(409);
    }
}
