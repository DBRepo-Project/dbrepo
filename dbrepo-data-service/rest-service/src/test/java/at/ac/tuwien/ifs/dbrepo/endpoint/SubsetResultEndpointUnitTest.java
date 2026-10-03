package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetResultEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(SubsetResultEndpointUnitTest.Config.class)
class SubsetResultEndpointUnitTest {
    @Autowired SubsetResultEndpoint endpoint;
    @Autowired SubsetResultService results;
    @Autowired SubsetReplicationService replication;
    @Autowired MetadataService metadata;
    private final UUID databaseId = UUID.randomUUID(), queryId = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean SubsetResultService results() { return mock(SubsetResultService.class); }
        @Bean SubsetReplicationService replication() { return mock(SubsetReplicationService.class); }
        @Bean MetadataService metadata() { return mock(MetadataService.class); }
        @Bean SubsetResultEndpoint endpoint(MetadataService metadata, SubsetReplicationService replication, SubsetResultService results) {
            return new SubsetResultEndpoint(metadata, replication, results);
        }
    }

    @BeforeEach void clean() { reset(results, replication, metadata); }

    @Test
    @WithMockUser(authorities = "replication")
    void routesValidateIdentityAndBoundChunkBodies() throws Exception {
        var database = Database.builder().id(databaseId).build();
        when(metadata.getDatabase(databaseId)).thenReturn(database);
        var query = new SubsetReplicationDto(queryId, "https://a.example", "https://a.example", databaseId,
                "SELECT 1", "SELECT 1", Instant.parse("2020-01-01T00:00:00Z"), true, "v2:" + "1".repeat(64), 1L, 1,
                "2".repeat(64));
        var manifest = new SubsetResultManifestDto(query, "[]", "3".repeat(64));
        var mvc = MockMvcBuilders.standaloneSetup(endpoint).build();
        String path = "/api/v1/database/" + databaseId + "/subset/" + queryId + "/result";
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().findAndRegisterModules().writeValueAsBytes(manifest))).andExpect(status().isOk());
        verify(replication).receive(database, query);
        verify(results).begin(database, manifest);
        mvc.perform(put(path + "/rows/0/chunks/0").contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Subset-Sender", "https://a.example").header("X-Subset-Database", databaseId)
                .header("X-Row-Length", 70000).header("X-Row-Hash", "a".repeat(64)).header("X-Chunk-Hash", "b".repeat(64))
                .content(new byte[SubsetResultService.CHUNK_SIZE + 1])).andExpect(status().isPayloadTooLarge());
        verify(replication).requireSender(database, "https://a.example", databaseId);
        verifyNoMoreInteractions(results);
        mvc.perform(post(path + "/publish").header("X-Subset-Sender", "https://a.example")
                .header("X-Subset-Database", databaseId)).andExpect(status().isNoContent());
        verify(results).publish(database, queryId);
    }

    private void denied() {
        assertThrows(AccessDeniedException.class, () -> endpoint.begin(databaseId, queryId, null));
        assertThrows(AccessDeniedException.class, () -> endpoint.append(databaseId, queryId, 0, 0, null, null, 0, null, null, null));
        assertThrows(AccessDeniedException.class, () -> endpoint.publish(databaseId, queryId, null, null));
        verifyNoInteractions(results, replication, metadata);
    }

    @Test @WithMockUser(authorities = "system") void systemCannotWriteArtifacts() { denied(); }
    @Test @WithMockUser(authorities = "persist-query") void userCannotWriteArtifacts() { denied(); }
    @Test @WithAnonymousUser void anonymousCannotWriteArtifacts() { denied(); }
}
