package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.CacheConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.MetadataServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubsetReplicationBackfillUnitTest {
    private static final String A = "https://a.example", B = "https://b.example", C = "https://c.example";
    private final SubsetResultService results = mock(SubsetResultService.class);
    private final SubsetReplicationService service = spy(new SubsetReplicationService(
            Mappers.getMapper(MariaDbMapper.class), new ObjectMapper(), new ReplicationPeers(A + "," + B + "," + C),
            mock(Validator.class), A, results));

    @Test
    void rejectsUntrustedLocalUnconfiguredAndUnmappedTargetsBeforeOpeningDatabase() {
        final Database database = Database.builder().creationLocation(A).replicaUrls(Map.of(B, UUID.randomUUID())).build();
        for (String target : new String[]{"https://evil.example", B + "/path", "", A, C}) {
            assertThrows(ResponseStatusException.class, () -> service.backfill(database, target));
        }
        database.setReplicaUrls(Collections.singletonMap(B, null));
        assertThrows(ResponseStatusException.class, () -> service.backfill(database, B));
        verify(service, never()).getDataSource(any(Database.class));
        verifyNoInteractions(results);
    }

    @Test
    void bindsCanonicalTargetAndClosesResourcesWhenEnqueueFails() throws Exception {
        final Database database = Database.builder().creationLocation(A).replicaUrls(Map.of(B, UUID.randomUUID())).build();
        final var pool = mock(ComboPooledDataSource.class);
        final var connection = mock(Connection.class);
        final var ddl = mock(Statement.class);
        final var insert = mock(PreparedStatement.class);
        doReturn(pool).when(service).getDataSource(database);
        when(pool.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(ddl);
        when(connection.prepareStatement(anyString())).thenReturn(insert);
        when(insert.executeUpdate()).thenThrow(new SQLException("outbox unavailable"));
        assertThrows(SQLException.class, () -> service.backfill(database, "https://B.example:443/"));
        verify(insert).setString(1, B);
        verify(insert).close();
        verify(connection).close();
        verify(pool).close();
        verifyNoInteractions(results);
    }

    @Test
    void metadataRefreshReplacesTopologyForSubsequentDispatcherReads() throws Exception {
        final UUID id = UUID.randomUUID();
        final var cache = mock(DatabaseCacheRepository.class);
        final var gateway = mock(MetadataServiceGateway.class);
        final var config = mock(CacheConfig.class);
        final var metadata = new MetadataServiceImpl(config, null, null, null, null, cache, null, gateway);
        final var stale = Database.builder().id(id).replicaUrls(Map.of()).build();
        final var fresh = Database.builder().id(id).replicaUrls(Map.of(B, UUID.randomUUID())).build();
        when(cache.findById(id)).thenReturn(Optional.of(stale));
        when(gateway.getDatabaseById(id)).thenReturn(fresh);
        when(cache.save(fresh)).thenAnswer(call -> {
            when(cache.findById(id)).thenReturn(Optional.of(fresh));
            return fresh;
        });
        assertSame(stale, metadata.getDatabase(id));
        assertSame(fresh, metadata.refreshDatabase(id));
        assertSame(fresh, metadata.getDatabase(id));
        verify(gateway).getDatabaseById(id);
        verify(cache).save(fresh);
        assertEquals(config.getTtl(), fresh.getExp());
    }
}
