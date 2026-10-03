package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.ViewCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.service.impl.ViewServiceImpl;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ViewArchivalUnitTest extends BaseTest {
    private final MetadataMapper mapper = Mappers.getMapper(MetadataMapper.class);
    private final DatabaseRepository repository = mock(DatabaseRepository.class);
    private final DatabaseCacheRepository databaseCache = mock(DatabaseCacheRepository.class);
    private final ViewCacheRepository viewCache = mock(ViewCacheRepository.class);
    private final DataServiceGateway data = mock(DataServiceGateway.class);
    private final ViewServiceImpl service = new ViewServiceImpl(mapper, data, repository,
            mock(SearchServiceGateway.class), databaseCache, viewCache);

    @Test
    void archivePreservesDefinitionAndMetadataAndEvictsOnlyAfterCommit() throws Exception {
        final String definition = VIEW_1.getQuery();
        final var columns = new ArrayList<>(VIEW_1.getColumns());
        final var views = new ArrayList<>(DATABASE_1.getViews());
        when(repository.save(DATABASE_1)).thenReturn(DATABASE_1);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(VIEW_1);
            final Instant archived = VIEW_1.getArchivedAt();
            service.delete(VIEW_1);
            assertNotNull(archived);
            assertEquals(archived, VIEW_1.getArchivedAt());
            assertEquals(definition, VIEW_1.getQuery());
            assertEquals(columns, VIEW_1.getColumns());
            assertEquals(views, DATABASE_1.getViews());
            verifyNoInteractions(data, databaseCache, viewCache);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(databaseCache, times(2)).deleteById(DATABASE_1_ID);
            verify(viewCache, times(2)).deleteById(VIEW_1_ID);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void activeListingHidesArchivedViewsButHistoricalMetadataKeepsThem() {
        final int count = DATABASE_1.getViews().size();
        VIEW_1.setArchivedAt(Instant.parse("2026-10-03T10:00:00Z"));
        assertEquals(count - 1, mapper.databaseToActiveDatabaseDto(DATABASE_1).getViews().size());
        assertEquals(count, DATABASE_1.getViews().size());
        assertEquals(count, mapper.databaseToDatabaseDto(DATABASE_1).getViews().size());
        assertEquals(VIEW_1.getArchivedAt(), mapper.viewToViewDto(VIEW_1).getArchivedAt());
    }
}
