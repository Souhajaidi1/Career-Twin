package com.souhajaidi.careertwin.career.config;

import com.souhajaidi.careertwin.engine.ScoringEngine;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    /** A clock bean so recency maths is testable with a fixed instant. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ScoringEngine scoringEngine(Clock clock) {
        return new ScoringEngine(clock);
    }
}
