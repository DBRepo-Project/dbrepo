package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(SubsetReplicationEndpointUnitTest.Config.class)
class SubsetReplicationEndpointUnitTest {
    @Autowired SubsetEndpoint endpoint;
    @Autowired SubsetService service;
    @Autowired MetadataService metadata;

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean SubsetService service() { return mock(SubsetService.class); }
        @Bean MetadataService metadata() { return mock(MetadataService.class); }
        @Bean SubsetEndpoint endpoint(SubsetService service, MetadataService metadata) {
            return new SubsetEndpoint(null, null, null, service, null, null, metadata, null, null, new ObjectMapper());
        }
    }

    @Test
    @WithMockUser(authorities = "replication")
    void receivesCanonicalStateOnTheProductionRoute() throws Exception {
        final UUID id = UUID.randomUUID();
        final Database database = Database.builder().id(id).build();
        final var state = new SubsetReplicationDto(UUID.randomUUID(), "https://a.example", "https://a.example",
                UUID.randomUUID(), "SELECT 1", "SELECT 1", Instant.parse("2020-02-29T12:34:56.123456Z"),
                true, "v2:" + "0".repeat(64), 1L, 1);
        when(metadata.getDatabase(id)).thenReturn(database);
        MockMvcBuilders.standaloneSetup(endpoint).build().perform(put("/api/v1/database/" + id + "/subset/replicate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().findAndRegisterModules().writeValueAsBytes(state)))
                .andExpect(status().isNoContent());
        verify(service).replicate(database, state);
        assertThrows(AccessDeniedException.class, () -> endpoint.upgradeQueryStore(id));
    }

    @Test
    @WithMockUser(authorities = "system")
    void internalSystemCredentialCannotReceiveExternalReplication() {
        assertThrows(AccessDeniedException.class, () -> endpoint.replicate(UUID.randomUUID(), null));
    }

    @Test
    @WithMockUser(authorities = "persist-query")
    void ordinarySubsetUsersCannotReceiveExternalReplication() {
        assertThrows(AccessDeniedException.class, () -> endpoint.replicate(UUID.randomUUID(), null));
    }

    @Test
    @WithAnonymousUser
    void anonymousCannotReceiveExternalReplication() {
        assertThrows(AccessDeniedException.class, () -> endpoint.replicate(UUID.randomUUID(), null));
    }
}
