package com.souhajaidi.careertwin.job.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "careertwin.career-service")
public record CareerClientProperties(@NotBlank String url, Duration timeout) {

    public CareerClientProperties {
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
    }
}
