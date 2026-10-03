package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(OutboxEndpointCancellationTest.Config.class)
class OutboxEndpointCancellationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy filters;
    @Autowired private ReplicationOutboxService outbox;
    @Autowired private ReplicationService replication;
    private MockMvc mvc;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reset(outbox, replication);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
    }

    @Test
    void cancellationRequiresSystemAuthority() throws Exception {
        mvc.perform(post(path()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"retired\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path()).header(HttpHeaders.AUTHORIZATION, authorization("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"retired\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(outbox, replication);
    }

    @Test
    void reasonIsRequiredAndBounded() throws Exception {
        for (String body : new String[]{"{}", "{\"reason\":\" \"}", "{\"reason\":\"" + "x".repeat(2001) + "\"}"}) {
            mvc.perform(post(path()).header(HttpHeaders.AUTHORIZATION, authorization("operator"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(outbox, replication);
    }

    @Test
    void auditActorComesFromAuthenticatedPrincipal() throws Exception {
        when(outbox.cancel(id, "retired", "operator"))
                .thenReturn(ReplicationOutboxEntry.builder().id(id).status(ReplicationOutboxStatus.CANCELLED).build());
        mvc.perform(post(path()).header(HttpHeaders.AUTHORIZATION, authorization("operator"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"retired\"}"))
                .andExpect(status().isOk());
        verify(outbox).cancel(id, "retired", "operator");
        verifyNoInteractions(replication);
    }

    @Test
    void unknownAndSucceededJobsReturnExplicitErrors() throws Exception {
        when(outbox.cancel(id, "retired", "operator")).thenThrow(new NoSuchElementException("missing"));
        mvc.perform(post(path()).header(HttpHeaders.AUTHORIZATION, authorization("operator"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"retired\"}"))
                .andExpect(status().isNotFound());
        doThrow(new IllegalStateException("already succeeded")).when(outbox).cancel(id, "retired", "operator");
        mvc.perform(post(path()).header(HttpHeaders.AUTHORIZATION, authorization("operator"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"retired\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelledJobCannotBeManuallyRetried() throws Exception {
        when(outbox.findById(id)).thenReturn(Optional.of(ReplicationOutboxEntry.builder()
                .id(id).status(ReplicationOutboxStatus.CANCELLED).build()));
        mvc.perform(post("/api/replication/outbox/" + id + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, authorization("operator")))
                .andExpect(status().isConflict());
        verifyNoInteractions(replication);
    }

    private String path() {
        return "/api/replication/outbox/" + id + "/cancel";
    }

    private String authorization(String username) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":test").getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {
        @Bean ReplicationOutboxService outbox() { return mock(ReplicationOutboxService.class); }
        @Bean ReplicationService replication() { return mock(ReplicationService.class); }
        @Bean OutboxEndpoint endpoint(ReplicationService replication, ReplicationOutboxService outbox) {
            return new OutboxEndpoint(replication, outbox);
        }
        @Bean UserDetailsService users() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("operator").password("{noop}test").authorities("system").build(),
                    User.withUsername("reader").password("{noop}test").authorities("read").build());
        }
        @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).httpBasic(withDefaults())
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated()).build();
        }
    }
}
