package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationCreation;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DatabaseReplicaCreationUnitTest {
    private static final String ORIGIN = "https://s46.datalab.tuwien.ac.at";
    private static final String TARGET = "https://s93.datalab.tuwien.ac.at";
    private final UUID parent = UUID.randomUUID();
    private final UUID creation = UUID.randomUUID();
    private final Container container = Container.builder().id(parent).build();
    private final DatabaseServiceMariaDbImpl service = spy(new DatabaseServiceMariaDbImpl(mock(MariaDbMapper.class)));
    private final ComboPooledDataSource pool = mock(ComboPooledDataSource.class);
    private final Connection connection = mock(Connection.class);

    @BeforeEach
    void setup() throws Exception {
        ReflectionTestUtils.setField(service, "baseUrl", TARGET);
        doReturn(pool).when(service).getDataSource(container);
        when(pool.getConnection()).thenReturn(connection);
    }

    @Test
    void acceptsIdentityReservedForThisTargetSite() throws Exception {
        verifyCreation(ReplicationCreation.localId("DATABASE", parent, ORIGIN, creation, TARGET));
    }

    @Test
    void resumesLegacyIdentity() throws Exception {
        verifyCreation(ReplicationCreation.localId("DATABASE", parent, ORIGIN, creation));
    }

    @Test
    void rejectsIdentityReservedForAnotherTargetBeforeDdl() throws Exception {
        UUID foreign = ReplicationCreation.localId("DATABASE", parent, ORIGIN, creation, "https://s73.datalab.tuwien.ac.at");
        try (var ddl = mockStatic(ReplicaDdl.class)) {
            assertThrows(DatabaseMalformedException.class, () -> service.create(container, payload(foreign)));
            ddl.verifyNoInteractions();
            verify(connection).rollback();
            verify(connection, never()).commit();
        }
    }

    private void verifyCreation(UUID identity) throws Exception {
        try (var ddl = mockStatic(ReplicaDdl.class)) {
            service.create(container, payload(identity));
            ddl.verify(() -> ReplicaDdl.createDatabase(connection, ReplicationCreation.databaseName(identity), identity.toString()));
            verify(connection).commit();
        }
    }

    private CreateDatabaseDto payload(UUID identity) {
        return CreateDatabaseDto.builder().containerId(parent).creationLocation(ORIGIN)
                .replicaUrls(Map.of(ORIGIN, creation)).internalName(ReplicationCreation.databaseName(identity)).build();
    }
}
