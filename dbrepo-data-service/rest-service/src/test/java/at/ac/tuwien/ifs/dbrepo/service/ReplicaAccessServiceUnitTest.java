package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.AccessServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReplicaAccessServiceUnitTest {

    private final AccessServiceMariaDbImpl service = spy(new AccessServiceMariaDbImpl(Mappers.getMapper(MariaDbMapper.class)));
    private final Database database = Database.builder().internalName("replica").creationLocation("https://origin.example").build();
    private final User user = User.builder().username("alice").password("test").build();
    private final ComboPooledDataSource source = mock(ComboPooledDataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final ResultSet result = mock(ResultSet.class);

    @BeforeEach
    void setup() throws Exception {
        ReflectionTestUtils.setField(service, "baseUrl", "https://local.example");
        ReflectionTestUtils.setField(service, "replicationUsername", "replication");
        ReflectionTestUtils.setField(service, "grantDefaultRead", "SELECT, EXECUTE");
        ReflectionTestUtils.setField(service, "grantDefaultWrite", "SELECT, INSERT, UPDATE, DELETE");
        doReturn(source).when(service).getDataSource(database);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
    }

    @Test
    void replicaRejectsWriteAccessBeforeOpeningConnection() {
        assertThrows(DatabaseMalformedException.class, () -> service.create(database, user, AccessTypeDto.WRITE_ALL));
        assertThrows(DatabaseMalformedException.class, () -> service.update(database, user, AccessTypeDto.WRITE_OWN));
        verify(service, never()).getDataSource(database);
    }

    @Test
    void newReplicaReaderGetsSelectOnly() throws Exception {
        service.create(database, user, AccessTypeDto.READ);
        verify(connection).prepareStatement("GRANT SELECT ON `replica`.* TO `alice`@`%`;");
        verify(connection, never()).prepareStatement(startsWith("GRANT EXECUTE"));
    }

    @Test
    void downgradeRevokesDatabaseAndOldProcedureGrantsBeforeGrantingSelect() throws Exception {
        when(result.next()).thenReturn(true);
        service.update(database, user, AccessTypeDto.READ);

        final var order = inOrder(connection);
        order.verify(connection).prepareStatement("REVOKE ALL PRIVILEGES ON `replica`.* FROM `alice`@`%`;");
        order.verify(connection).prepareStatement("REVOKE EXECUTE ON PROCEDURE `replica`.`store_query` FROM `alice`@`%`;");
        order.verify(connection).prepareStatement("GRANT SELECT ON `replica`.* TO `alice`@`%`;");
    }

    @Test
    void technicalReplicationUserRetainsWriteAccess() throws Exception {
        user.setUsername("replication");
        service.create(database, user, AccessTypeDto.WRITE_ALL);
        verify(connection).prepareStatement("GRANT SELECT, INSERT, UPDATE, DELETE ON `replica`.* TO `replication`@`%`;");
    }

    @Test
    void legacyDatabaseRetainsExistingReadPrivileges() throws Exception {
        database.setCreationLocation(null);
        service.create(database, user, AccessTypeDto.READ);
        verify(connection).prepareStatement("GRANT SELECT, EXECUTE ON `replica`.* TO `alice`@`%`;");
        verify(connection).prepareStatement("GRANT EXECUTE ON PROCEDURE `store_query` TO `alice`@`%`;");
    }

    @Test
    void replicatedPrimaryRetainsApiWriteAclButGrantsSelectOnly() throws Exception {
        database.setCreationLocation("https://local.example");
        database.setReplicaUrls(java.util.Collections.singletonMap("https://peer.example", null));
        service.create(database, user, AccessTypeDto.WRITE_ALL);
        verify(connection).prepareStatement("GRANT SELECT ON `replica`.* TO `alice`@`%`;");
        verify(connection, never()).prepareStatement(startsWith("GRANT EXECUTE"));
    }

    @Test
    void privilegedContainerAccountCanStillApplyJournalledWrites() throws Exception {
        database.setContainer(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container.builder().username("alice").build());
        service.create(database, user, AccessTypeDto.WRITE_ALL);
        verify(connection).prepareStatement("GRANT SELECT, INSERT, UPDATE, DELETE ON `replica`.* TO `alice`@`%`;");
    }
}
