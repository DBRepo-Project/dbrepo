package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.TableCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.TableRepository;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplicaTopologyUnitTest {
    private static final String ORIGIN = "https://source.example", TARGET = "https://new.example";

    @Test
    void existingSecondaryLearnsNewPeerIdsWithoutLosingThePrimaryAndRejectsUnknownSites() throws Exception {
        final var databases = mock(DatabaseRepository.class);
        final var tables = mock(TableRepository.class);
        final var search = mock(SearchServiceGateway.class);
        final var cache = mock(DatabaseCacheRepository.class);
        final var tableCache = mock(TableCacheRepository.class);
        final var dbService = new DatabaseServiceImpl(null, null, databases, null, search, cache);
        final var tableService = new TableServiceImpl(null, null, null, databases, tables, search, cache, tableCache);
        for (var service : List.of(dbService, tableService)) {
            ReflectionTestUtils.setField(service, "entityManager", mock(EntityManager.class));
            ReflectionTestUtils.setField(service, "allowedSites", ORIGIN + "," + TARGET);
        }
        final var database = new Database();
        database.setId(UUID.randomUUID()); database.setCreationLocation(ORIGIN);
        final UUID sourceId = UUID.randomUUID(), sourceTable = UUID.randomUUID();
        database.getReplicaUrls().add(ReplicaLocation.builder().url(ORIGIN).replicaDatabaseId(sourceId).build());
        final var table = new Table();
        table.setId(UUID.randomUUID()); table.setDatabase(database);
        table.getReplicaUrls().add(ReplicaTableLocation.builder().url(ORIGIN).replicaTableId(sourceTable).build());
        when(databases.findById(database.getId())).thenReturn(Optional.of(database));
        when(databases.save(database)).thenReturn(database);
        when(tables.findById(table.getId())).thenReturn(Optional.of(table));
        final UUID remoteDatabase = UUID.randomUUID(), remoteTable = UUID.randomUUID();
        final var dbUpdate = DatabaseUpdateReplicationUrlDto.builder().replicaUrl(TARGET).replicaDatabaseId(remoteDatabase).build();
        final var tableUpdate = TableUpdateReplicationUrlDto.builder().replicaUrl(TARGET).replicaTableId(remoteTable).build();
        dbService.updateReplicationUrl(database.getId(), dbUpdate);
        dbService.updateReplicationUrl(database.getId(), dbUpdate);
        tableService.updateReplicationUrl(table.getId(), tableUpdate);
        tableService.updateReplicationUrl(table.getId(), tableUpdate);
        assertEquals(2, database.getReplicaUrls().size());
        assertEquals(2, table.getReplicaUrls().size());
        assertEquals(sourceId, database.getReplicaUrls().getFirst().getReplicaDatabaseId());
        assertEquals(sourceTable, table.getReplicaUrls().getFirst().getReplicaTableId());
        assertEquals(remoteDatabase, database.getReplicaUrls().getLast().getReplicaDatabaseId());
        assertEquals(remoteTable, table.getReplicaUrls().getLast().getReplicaTableId());
        assertThrows(MalformedException.class, () -> dbService.updateReplicationUrl(database.getId(),
                DatabaseUpdateReplicationUrlDto.builder().replicaUrl("https://evil.example").replicaDatabaseId(UUID.randomUUID()).build()));
        assertThrows(MalformedException.class, () -> tableService.updateReplicationUrl(table.getId(),
                TableUpdateReplicationUrlDto.builder().replicaUrl("https://evil.example").replicaTableId(UUID.randomUUID()).build()));
        assertEquals(2, database.getReplicaUrls().size());
        assertEquals(2, table.getReplicaUrls().size());
    }
}
