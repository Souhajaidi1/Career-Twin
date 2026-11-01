package com.souhajaidi.careertwin.job.config;

import com.souhajaidi.careertwin.engine.ScoringEngine;
import java.time.Clock;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EngineConfig {

    @Bean
    public ScoringEngine scoringEngine() {
        return new ScoringEngine(Clock.systemUTC());
    }

    /** Short timeouts: a slow career-service must degrade a job list, not hang it. */
    @Bean
    public RestClient careerClient(CareerClientProperties properties) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.timeout())
                .withReadTimeout(properties.timeout());
        return RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
