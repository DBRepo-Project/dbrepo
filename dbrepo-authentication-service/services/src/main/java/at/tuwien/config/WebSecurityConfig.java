package at.tuwien.config;

import at.tuwien.auth.AuthTokenFilter;
import at.tuwien.auth.JwtUtils;
import at.tuwien.service.TokenService;
import at.tuwien.service.impl.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtUtils jwtUtils;
    private final TokenService tokenService;
    private final SecurityConfig securityConfig;
    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public WebSecurityConfig(JwtUtils jwtUtils, TokenService tokenService, SecurityConfig securityConfig,
                             UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.tokenService = tokenService;
        this.securityConfig = securityConfig;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public AuthTokenFilter authTokenFilter() {
        return new AuthTokenFilter(jwtUtils, tokenService, userDetailsService);
    }

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder.userDetailsService(userDetailsService)
                .passwordEncoder(securityConfig.passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
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
        http.authorizeRequests()
                /* our internal endpoints */
                .antMatchers(HttpMethod.GET, "/actuator/prometheus/**").permitAll()
                /* our public endpoints */
                .antMatchers(HttpMethod.GET, "/api/user/secret").permitAll()
                .antMatchers(HttpMethod.GET, "/api/user/secret/resend").permitAll()
                .antMatchers(HttpMethod.POST, "/api/user").permitAll()
                .antMatchers(HttpMethod.GET, "/api/user").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/user").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/user/reset").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth").permitAll()
                .antMatchers("/v3/api-docs.yaml",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html").permitAll()
                /* our private endpoints */
                .anyRequest().authenticated();
        /* add JWT token filter */
        http.addFilterBefore(authTokenFilter(),
                UsernamePasswordAuthenticationFilter.class
        );
    }

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
