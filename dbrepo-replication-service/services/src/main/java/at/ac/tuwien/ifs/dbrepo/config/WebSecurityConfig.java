package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.auth.BasicAuthenticationProvider;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, BasicAuthenticationProvider basicAuthenticationProvider)
            throws Exception {
        final OrRequestMatcher publicEndpoints = new OrRequestMatcher(
                new AntPathRequestMatcher("/actuator/**", "GET"),
                new AntPathRequestMatcher("/v3/api-docs.yaml"),
                new AntPathRequestMatcher("/v3/api-docs/**"),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/swagger-ui.html")
        );
        http.cors(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(configurer -> configurer.authenticationEntryPoint((request, response, ex) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage())));
        http.authorizeHttpRequests(configurer -> configurer
                .requestMatchers(publicEndpoints).permitAll()
                .anyRequest().authenticated());
        http.httpBasic(Customizer.withDefaults())
                .authenticationManager(authentication -> {
                    if (basicAuthenticationProvider.supports(authentication.getClass())) {
                        return basicAuthenticationProvider.authenticate(authentication);
                    }
                    throw new BadCredentialsException("Unsupported authentication type: " + authentication.getClass());
                });
        return http.build();
    }

}
