package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.service.impl.BrokerServiceRabbitMqImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplicaBrokerPermissionUnitTest {

    @Test
    void staleWriteAllAccessDoesNotPermitPublishingToReplica() {
        final RabbitConfig config = mock(RabbitConfig.class);
        when(config.getExchangeName()).thenReturn("dbrepo");
        final var service = new BrokerServiceRabbitMqImpl(config, null);
        ReflectionTestUtils.setField(service, "baseUrl", "https://local.example");
        final UUID localId = UUID.randomUUID();
        final UUID replicaId = UUID.randomUUID();
        final var replica = DatabaseAccess.builder().type(AccessType.WRITE_ALL)
                .database(Database.builder().id(replicaId).creationLocation("https://origin.example").build()).build();
        final var local = DatabaseAccess.builder().type(AccessType.WRITE_ALL)
                .database(Database.builder().id(localId).build()).build();

        assertEquals("", service.userToExchangeWritePermissionString("alice", List.of(replica)));
        final String permissions = service.userToExchangeWritePermissionString("alice", List.of(local, replica));
        assertTrue(("dbrepo." + localId + ".table").matches(permissions));
        assertFalse(("dbrepo." + replicaId + ".table").matches(permissions));
    }
}
