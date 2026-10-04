package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.AddReplicaDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseReplicaService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(DatabaseReplicaEndpointUnitTest.Config.class)
class DatabaseReplicaEndpointUnitTest {
    @Autowired DatabaseReplicaEndpoint endpoint;
    @Autowired DatabaseService databases;
    @Autowired DatabaseReplicaService replicas;
    private final UUID databaseId = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean EntityManagerFactory entityManagerFactory() { return mock(EntityManagerFactory.class); }
        @Bean DatabaseService databases() { return mock(DatabaseService.class); }
        @Bean DatabaseReplicaService replicas() { return mock(DatabaseReplicaService.class); }
        @Bean DatabaseReplicaEndpoint endpoint(DatabaseService databases, DatabaseReplicaService replicas) {
            return new DatabaseReplicaEndpoint(databases, replicas);
        }
    }

    @BeforeEach
    void setup() throws Exception {
        reset(databases, replicas);
        when(databases.findById(databaseId)).thenReturn(Database.builder().id(databaseId).ownedBy("alice").build());
    }

    @Test
    @WithMockUser(username = "alice", authorities = "create-database")
    void ownerCanRequestButCannotUseInternalRegistration() throws Exception {
        MockMvcBuilders.standaloneSetup(endpoint).build().perform(post("/api/v1/database/" + databaseId + "/replicas")
                .principal(SecurityContextHolder.getContext().getAuthentication()).contentType("application/json")
                .content("{\"replica_url\":\"https://peer.example\"}")).andExpect(status().isAccepted());
        verify(replicas).request(any(Database.class), eq("https://peer.example"));
        assertThrows(AccessDeniedException.class,
                () -> endpoint.register(databaseId, new AddReplicaDto("https://peer.example"), Set.of()));
    }

    @Test
    @WithMockUser(username = "bob", authorities = "create-database")
    void otherDatabaseOwnerCannotAddTargets() throws Exception {
        MockMvcBuilders.standaloneSetup(endpoint).build().perform(post("/api/v1/database/" + databaseId + "/replicas")
                .principal(SecurityContextHolder.getContext().getAuthentication()).contentType("application/json")
                .content("{\"replica_url\":\"https://peer.example\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(replicas);
    }

    @Test
    @WithMockUser(authorities = "system")
    void systemCanRegisterAnEmptyDatabaseWithNoPreparedTableParameters() throws Exception {
        MockMvcBuilders.standaloneSetup(endpoint).build().perform(post("/api/v1/database/" + databaseId + "/replicas/register")
                .contentType("application/json").content("{\"replica_url\":\"https://peer.example\"}"))
                .andExpect(status().isNoContent());
        verify(replicas).register(databaseId, "https://peer.example", Set.of());
    }

    @Test
    @WithMockUser(authorities = "replication")
    void peerCredentialCannotRegisterOrActivateSourceReplication() {
        assertThrows(AccessDeniedException.class, () -> endpoint.register(databaseId, new AddReplicaDto("https://peer.example"), Set.of()));
        assertThrows(AccessDeniedException.class, () -> endpoint.add(databaseId, new AddReplicaDto("https://peer.example"),
                SecurityContextHolder.getContext().getAuthentication()));
        verifyNoInteractions(replicas);
    }
}
