package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.service.transport.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RestTemplate;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransportEndpointTest {
    @TempDir Path directory;

    @Test
    void onlyConfiguredPeersCanBeSelectedAndIdsComeFromMetadata() throws Exception {
        var mapper = new ObjectMapper();
        var routes = new ReplicationTransportRoutes(mapper, directory.resolve("routes.json"));
        var metadata = mock(RestTemplate.class);
        var rabbit = new ReplicationAmqp(mapper, new ReplicationAmqp.Settings(null, Map.of(), Map.of(), false), r -> null, 1000);
        var endpoint = new TransportEndpoint(routes, rabbit, metadata, mock(RestTemplate.class),
                "https://b.example", "https://a.example");
        UUID local = UUID.randomUUID(), remote = UUID.randomUUID();
        when(metadata.getForEntity("/api/v1/database/" + local, DatabaseDto.class))
                .thenReturn(ResponseEntity.ok(DatabaseDto.builder().id(local).replicaUrls(Map.of("https://b.example/", remote)).build()));
        endpoint.save(local, new TransportEndpoint.Selection("https://b.example", ReplicationTransportRoutes.Transport.REST));
        assertEquals(remote, routes.findAll().getFirst().remoteDatabaseId());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> endpoint.save(local, new TransportEndpoint.Selection("https://evil.example", ReplicationTransportRoutes.Transport.REST)));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> endpoint.save(local, new TransportEndpoint.Selection("https://b.example", ReplicationTransportRoutes.Transport.RABBITMQ)));
        assertEquals("hasAuthority('system')", TransportEndpoint.class.getAnnotation(PreAuthorize.class).value());
    }
}
