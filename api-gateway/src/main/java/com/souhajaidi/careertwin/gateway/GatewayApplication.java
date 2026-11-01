package com.souhajaidi.careertwin.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Routing only. Authentication is deliberately NOT done here: every service verifies the JWT itself,
 * so a request that reaches a service some other way (a misconfigured network, a sidecar) is still
 * rejected. The gateway is a convenience for clients, not a trust boundary.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
