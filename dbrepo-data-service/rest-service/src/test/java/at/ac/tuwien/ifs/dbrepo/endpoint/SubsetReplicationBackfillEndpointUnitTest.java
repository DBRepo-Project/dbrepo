package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetReplicationBackfillEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(SubsetReplicationBackfillEndpointUnitTest.Config.class)
class SubsetReplicationBackfillEndpointUnitTest {
    @Autowired SubsetReplicationBackfillEndpoint endpoint;
    @Autowired MetadataService metadata;
    @Autowired SubsetReplicationService replication;
    @Autowired SubsetService subsets;

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean MetadataService metadata() { return mock(MetadataService.class); }
        @Bean SubsetReplicationService replication() { return mock(SubsetReplicationService.class); }
        @Bean SubsetService subsets() { return mock(SubsetService.class); }
        @Bean SubsetReplicationBackfillEndpoint endpoint(MetadataService metadata, SubsetReplicationService replication, SubsetService subsets) {
            return new SubsetReplicationBackfillEndpoint(metadata, replication, subsets);
        }
    }

    @BeforeEach
    void resetMocks() { reset(metadata, replication, subsets); }

    @Test
    @WithMockUser(authorities = "system")
    void queuesWithFreshTopologyOnProductionRoute() throws Exception {
        final UUID id = UUID.randomUUID();
        final Database fresh = Database.builder().id(id).build();
        when(metadata.refreshDatabase(id)).thenReturn(fresh);
        MockMvcBuilders.standaloneSetup(endpoint).build()
                .perform(post("/api/v1/database/" + id + "/subset/replication-backfill")
                        .param("targetSite", "https://b.example"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("queued"));
        verify(replication).backfill(fresh, "https://b.example");
        verify(subsets).upgradeQueryStore(fresh);
        verify(metadata, never()).getDatabase(any());
    }

    @Test
    @WithMockUser(authorities = "system")
    void targetIsRequired() throws Exception {
        MockMvcBuilders.standaloneSetup(endpoint).build()
                .perform(post("/api/v1/database/" + UUID.randomUUID() + "/subset/replication-backfill"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(metadata, replication);
    }

    @Test
    @WithMockUser(authorities = "replication")
    void replicationCredentialCannotBootstrap() {
        assertThrows(AccessDeniedException.class, () -> endpoint.backfill(UUID.randomUUID(), "https://b.example"));
        verifyNoInteractions(metadata, replication);
    }

    @Test
    @WithAnonymousUser
    void anonymousCannotBootstrap() {
        assertThrows(AccessDeniedException.class, () -> endpoint.backfill(UUID.randomUUID(), "https://b.example"));
        verifyNoInteractions(metadata, replication);
    }
}
