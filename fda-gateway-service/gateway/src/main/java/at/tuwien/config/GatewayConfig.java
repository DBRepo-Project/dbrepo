package at.tuwien.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authentication-service", r -> r.path("/api/auth/**",
                                "/api/user/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://authentication-service"))
                .route("broker-service", r -> r.path("/api/broker/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://broker-service"))
                .route("analyse-service", r -> r.path("/api/analyse/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://analyse-service"))
                .route("identifier-service", r -> r.path("/api/pid/**",
                                "/api/container/**/database/**/identifier/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://identifier-service"))
                .route("query-service", r -> r.path("/api/container/**/database/**/query/**",
                                "/api/container/**/database/**/table/**/history/**",
                                "/api/container/**/database/**/table/**/data/**",
                                "/api/container/**/database/**/table/**/query/**",
                                "/api/container/**/database/**/table/**/export/**",
                                "/api/container/**/database/**/version/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://query-service"))
                .route("table-service", r -> r.path("/api/container/**/database/**/table/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://table-service"))
                .route("database-service", r -> r.path("/api/container/**/database/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://database-service"))
                .route("container-service", r -> r.path("/api/container/**",
                                "/api/image/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://container-service"))
                .route("units-service", r -> r.path("/api/units/**")
                        .and()
                        .method("POST", "GET", "PUT", "DELETE")
                        .and()
                        .uri("lb://units-service"))
                .build();

    }

}
