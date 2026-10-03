package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.TableCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceImpl;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TableArchivalUnitTest extends BaseTest {

    private final DatabaseRepository repository = mock(DatabaseRepository.class);
    private final DatabaseCacheRepository databaseCache = mock(DatabaseCacheRepository.class);
    private final TableCacheRepository tableCache = mock(TableCacheRepository.class);
    private final DataServiceGateway data = mock(DataServiceGateway.class);
    private final TableServiceImpl service = new TableServiceImpl(null, null, data, repository, null,
            mock(SearchServiceGateway.class), databaseCache, tableCache);

    @Test
    void archivePreservesDataAndCompleteMetadataGraph() throws Exception {
        final var columns = new ArrayList<>(TABLE_1.getColumns());
        final var tables = new ArrayList<>(DATABASE_1.getTables());
        when(repository.save(DATABASE_1)).thenReturn(DATABASE_1);

        service.deleteTable(TABLE_1);
        final Instant archived = TABLE_1.getArchivedAt();
        service.deleteTable(TABLE_1);

        assertNotNull(archived);
        assertEquals(archived, TABLE_1.getArchivedAt());
        assertEquals(columns, TABLE_1.getColumns());
        assertEquals(tables, DATABASE_1.getTables());
        verifyNoInteractions(data);
        verify(databaseCache, times(2)).deleteById(DATABASE_1_ID);
        verify(tableCache, times(2)).deleteById(TABLE_1_ID);
    }

    @Test
    void cacheEvictionWaitsForCommit() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deleteTable(TABLE_1);
            verifyNoInteractions(databaseCache, tableCache);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(databaseCache).deleteById(DATABASE_1_ID);
            verify(tableCache).deleteById(TABLE_1_ID);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void activeListingExcludesArchivedTablesWithoutMutatingEntityOrHistoricListing() {
        final MetadataMapper mapper = Mappers.getMapper(MetadataMapper.class);
        final int before = DATABASE_1.getTables().size();
        final Table table = DATABASE_1.getTables().getFirst();
        table.setArchivedAt(Instant.parse("2026-10-03T10:00:00Z"));

        assertEquals(before - 1, mapper.databaseToActiveDatabaseDto(DATABASE_1).getTables().size());
        assertEquals(before, DATABASE_1.getTables().size());
        assertEquals(before, mapper.databaseToDatabaseDto(DATABASE_1).getTables().size());
        assertEquals(table.getArchivedAt(), mapper.tableToTableDto(table).getArchivedAt());
    }
}
