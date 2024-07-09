package at.tuwien.config;

import at.tuwien.auth.AuthTokenFilter;
import at.tuwien.auth.BasicAuthenticationProvider;
import at.tuwien.gateway.KeycloakGateway;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class WebSecurityConfig {

    @Bean
    public AuthTokenFilter authTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, KeycloakGateway keycloakGateway)
            throws Exception {
        final OrRequestMatcher internalEndpoints = new OrRequestMatcher(
                new AntPathRequestMatcher("/actuator/**", "GET"),
                new AntPathRequestMatcher("/v3/api-docs.yaml"),
                new AntPathRequestMatcher("/v3/api-docs/**"),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/swagger-ui.html")
        );
        final OrRequestMatcher publicEndpoints = new OrRequestMatcher(
                new AntPathRequestMatcher("/api/**", "GET"),
                new AntPathRequestMatcher("/api/**", "HEAD")
        );
        /* enable CORS and disable CSRF */
        http = http.cors().and().csrf().disable();
        /* set session management to stateless */
        http = http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and();
        /* set unauthorized requests exception handler */
        http = http
                .exceptionHandling()
                .authenticationEntryPoint(
                        (request, response, ex) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                    ex.getMessage()
                            );
                        }
                ).and();
        /* set permissions on endpoints */
        http.authorizeHttpRequests()
                /* our internal endpoints */
                .requestMatchers(internalEndpoints).permitAll()
                /* our public endpoints */
                .requestMatchers(publicEndpoints).permitAll()
                /* our private endpoints */
                .anyRequest().authenticated();
        /* add JWT token filter */
        http.addFilterBefore(authTokenFilter(),
                UsernamePasswordAuthenticationFilter.class
        );
        http.addFilterBefore(new BasicAuthenticationFilter(new BasicAuthenticationProvider(authTokenFilter(),
                        keycloakGateway)),
                UsernamePasswordAuthenticationFilter.class
        );
        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}
