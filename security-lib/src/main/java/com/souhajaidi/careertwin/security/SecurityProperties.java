package com.souhajaidi.careertwin.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param publicPaths ant patterns reachable without a token, in addition to health and API docs */
@ConfigurationProperties(prefix = "careertwin.security")
public record SecurityProperties(List<String> publicPaths) {

    public SecurityProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
