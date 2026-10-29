package at.ac.tuwien.ifs.dbrepo.service.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplicationTransportTest {
    @TempDir Path directory;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void selectionIsPerDatabaseAndPeerAndSurvivesRestart() throws Exception {
        var path = directory.resolve("transports.json");
        var routes = new ReplicationTransportRoutes(mapper, path);
        var local = UUID.randomUUID();
        var remote = UUID.randomUUID();
        routes.save(local, "https://b.example", ReplicationTransportRoutes.Transport.RABBITMQ, remote);
        var restarted = new ReplicationTransportRoutes(mapper, path);
        assertTrue(restarted.usesRabbit(URI.create("https://b.example/api/v1/database/" + remote + "/subset/replicate"), new byte[0]));
        assertFalse(restarted.usesRabbit(URI.create("https://c.example/api/v1/database/" + remote + "/subset/replicate"), new byte[0]));
        assertFalse(restarted.usesRabbit(URI.create("https://b.example/api/v1/database/" + UUID.randomUUID() + "/subset/replicate"), new byte[0]));
        restarted.save(local, "https://b.example", ReplicationTransportRoutes.Transport.REST, remote);
        assertFalse(restarted.usesRabbit(URI.create("https://b.example/api/v1/database/" + remote + "/subset/replicate"), new byte[0]));
    }

    @Test
    void bootstrapLearnsTargetIdWithoutGuessingIt() throws Exception {
        var routes = new ReplicationTransportRoutes(mapper, directory.resolve("transports.json"));
        var local = UUID.randomUUID();
        var remote = UUID.randomUUID();
        var uri = URI.create("https://b.example/api/v1/database/replicate");
        var body = mapper.writeValueAsBytes(java.util.Map.of("creationId", local));
        routes.save(local, "https://b.example", ReplicationTransportRoutes.Transport.RABBITMQ, null);
        assertTrue(routes.usesRabbit(uri, body));
        routes.created(uri, body, mapper.writeValueAsBytes(java.util.Map.of("id", remote)));
        routes.save(local, "https://b.example", ReplicationTransportRoutes.Transport.RABBITMQ, null);
        assertEquals(remote, routes.findAll().getFirst().remoteDatabaseId());
        assertTrue(routes.usesRabbit(URI.create("https://b.example/api/v1/database/" + remote + "/table/replicate"), new byte[0]));
        assertTrue(routes.usesRabbit(URI.create("https://B.example:443/api/v1/database/" + remote + "/table/replicate"), new byte[0]));
    }

    @Test
    void onlyExplicitReplicationOperationsMayCrossTheBroker() {
        var db = UUID.randomUUID();
        var table = UUID.randomUUID();
        assertEquals("data", ReplicationAmqpProtocol.service("PUT", "/api/v1/database/" + db + "/table/" + table + "/data/replicate"));
        assertEquals("metadata", ReplicationAmqpProtocol.service("POST", "/api/v1/database/replicate"));
        for (String path : new String[]{"/actuator/env", "https://evil.example/", "//evil.example/", "/api/v1/database/" + db,
                "/api/v1/database/" + db + "/subset/replicate?redirect=x", "/api/v1/database/../replicate"}) {
            assertThrows(IllegalArgumentException.class, () -> ReplicationAmqpProtocol.service("POST", path), path);
        }
        assertThrows(IllegalArgumentException.class, () -> ReplicationAmqpProtocol.service("DELETE", "/api/v1/database/replicate"));
    }
}
