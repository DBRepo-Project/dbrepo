package at.ac.tuwien.ifs.dbrepo.service.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReplicationAmqpTest {
    @Test
    void credentialsAndQueueNamesAreServerControlledAndTlsIsRequired() {
        var peer = new ReplicationAmqp.Peer("amqp://user:secret@remote.example/vhost", "dbrepo.replication.a");
        assertThrows(IllegalArgumentException.class, () -> new ReplicationAmqp(new ObjectMapper(),
                new ReplicationAmqp.Settings(null, Map.of(), Map.of("https://b.example", peer), false), r -> null, 1000));
        assertThrows(IllegalArgumentException.class, () -> new ReplicationAmqp(new ObjectMapper(),
                new ReplicationAmqp.Settings(null, Map.of(), Map.of("https://b.example",
                        new ReplicationAmqp.Peer("amqps://user:secret@remote.example/vhost", "arbitrary")), false), r -> null, 1000));
    }

    @Test
    void brokerAcceptanceIsNotAnApplicationReceipt() throws Exception {
        try (var transport = new ReplicationAmqp(new ObjectMapper(),
                new ReplicationAmqp.Settings(null, Map.of(), Map.of(), false), r -> null, 1000)) {
            assertThrows(java.io.IOException.class, () -> transport.send("https://unconfigured.example",
                    new ReplicationAmqpProtocol.Request("POST", "/api/v1/database/replicate", new byte[0])));
        }
    }
}
