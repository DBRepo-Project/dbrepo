package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationActivationService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplicationActivationConfigurationUnitTest {
    private static final String SOURCE = "https://s46.datalab.tuwien.ac.at";
    private static final String TARGET = "https://s93.datalab.tuwien.ac.at";

    @Test
    void configuredPeerReachesActivationWithProductionYaml() throws Exception {
        final MetadataService metadata = mock(MetadataService.class);
        final UUID databaseId = UUID.randomUUID();
        final IllegalStateException reachedMetadata = new IllegalStateException("Reached activation metadata lookup");
        when(metadata.refreshDatabase(databaseId)).thenThrow(reachedMetadata);
        final var activation = activation(metadata, "https://s73.datalab.tuwien.ac.at," + TARGET);

        assertSame(reachedMetadata, assertThrows(IllegalStateException.class,
                () -> activation.activate(databaseId, TARGET)));
        verify(metadata).refreshDatabase(databaseId);
    }

    @Test
    void missingPeersRemainRejectedBeforeAnyDatabaseAccess() throws Exception {
        final MetadataService metadata = mock(MetadataService.class);
        final var activation = activation(metadata, "");
        assertThrows(IllegalArgumentException.class, () -> activation.activate(UUID.randomUUID(), TARGET));
        verifyNoInteractions(metadata);
    }

    private ReplicationActivationService activation(MetadataService metadata, String peers) throws Exception {
        final StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("deployment", Map.of("REPLICATION_ALLOWED_SITES", peers)));
        for (var source : new YamlPropertySourceLoader().load("production-yaml", new ClassPathResource("application.yml"))) {
            environment.getPropertySources().addLast(source);
        }
        return new ReplicationActivationService(metadata, mock(RestTemplate.class), mock(TupleReplicationOutboxService.class),
                environment.getProperty("dbrepo.replication.allowedSites", ""), SOURCE);
    }
}
