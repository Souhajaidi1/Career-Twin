package com.souhajaidi.careertwin.job.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.souhajaidi.careertwin.engine.Results.JobRequirements;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Asks career-service to score parsed requirements. The caller's own bearer token is forwarded, so
 * career-service decides whose evidence to use from the token alone; this service never names a user.
 */
@Component
public class CareerClient {

    private static final Logger log = LoggerFactory.getLogger(CareerClient.class);

    private final RestClient client;

    public CareerClient(RestClient careerClient) {
        this.client = careerClient;
    }

    /** @return the raw match document per job key; empty if career-service could not be reached */
    public Map<String, JsonNode> match(String bearerToken, Map<String, JobRequirements> jobs) {
        try {
            JsonNode body = client.post()
                    .uri("/api/v1/analysis/match")
                    .header("Authorization", "Bearer " + bearerToken)
                    .body(Map.of("jobs", jobs))
                    .retrieve()
                    .body(JsonNode.class);
            Map<String, JsonNode> out = new java.util.LinkedHashMap<>();
            if (body != null && body.has("results")) {
                body.get("results").fields().forEachRemaining(e -> out.put(e.getKey(), e.getValue()));
            }
            return out;
        } catch (RestClientException ex) {
            log.warn("career-service match call failed: {}", ex.getClass().getSimpleName());
            return Map.of();
        }
    }
}
