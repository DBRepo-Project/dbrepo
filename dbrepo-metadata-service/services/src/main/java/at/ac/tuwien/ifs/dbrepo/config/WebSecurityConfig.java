package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.auth.BasicAuthenticationProvider;
import at.ac.tuwien.ifs.dbrepo.auth.BearerAuthenticationProvider;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
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
    public SecurityFilterChain filterChain(HttpSecurity http, BasicAuthenticationProvider basicAuthenticationProvider,
                                           BearerAuthenticationProvider bearerAuthenticationProvider)
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
        http.csrf(AbstractHttpConfigurer::disable);
        /* set session management to stateless */
        http.sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        /* set unauthorized requests exception handler */
        http.exceptionHandling(configurer -> configurer.authenticationEntryPoint((request, response, ex) -> {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    ex.getMessage()
            );
        }));
        /* set permissions on endpoints */
        http.authorizeHttpRequests(configurer -> configurer
                /* our internal endpoints */
                .requestMatchers(internalEndpoints).permitAll()
                /* our public endpoints */
                .requestMatchers(publicEndpoints).permitAll()
                /* our private endpoints */
                .anyRequest().authenticated());
        /* add basic auth */
        http.httpBasic(Customizer.withDefaults())
                .authenticationManager(authentication -> {
                    if (bearerAuthenticationProvider.supports(authentication.getClass())) {
                        return bearerAuthenticationProvider.authenticate(authentication);
                    }
                    if (basicAuthenticationProvider.supports(authentication.getClass())) {
                        return basicAuthenticationProvider.authenticate(authentication);
                    }
                    throw new BadCredentialsException("Unsupported authentication type: " + authentication.getClass());
                });
        /* add JWT token filter */
        http.oauth2ResourceServer((oauth2) -> oauth2
                .jwt(configurer -> configurer.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final ExpressionJwtGrantedAuthoritiesConverter converter = new ExpressionJwtGrantedAuthoritiesConverter(
                new SpelExpressionParser().parseRaw("[realm_access][roles]"));
        converter.setAuthorityPrefix("");
        final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtAuthenticationConverter;
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
