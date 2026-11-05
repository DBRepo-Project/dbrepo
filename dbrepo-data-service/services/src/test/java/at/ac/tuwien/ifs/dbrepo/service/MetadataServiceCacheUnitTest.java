package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.*;
import at.ac.tuwien.ifs.dbrepo.config.CacheConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.impl.MetadataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetadataServiceCacheUnitTest {

    private static final UUID DATABASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_DATABASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CHILD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private TableCacheRepository tables;
    private ViewCacheRepository views;
    private MetadataServiceGateway gateway;
    private MetadataService service;

    @BeforeEach
    void setUp() {
        tables = mock(TableCacheRepository.class);
        views = mock(ViewCacheRepository.class);
        gateway = mock(MetadataServiceGateway.class);
        final CacheConfig config = mock(CacheConfig.class);
        when(config.getTtl()).thenReturn(60L);
        service = new MetadataServiceImpl(config, mock(UserCacheRepository.class), views,
                mock(ImageCacheRepository.class), tables, mock(DatabaseCacheRepository.class),
                mock(ContainerCacheRepository.class), gateway);
    }

    static Stream<Optional<Table>> tableMisses() {
        return Stream.of(Optional.empty(), Optional.of(table(OTHER_DATABASE_ID)), Optional.of(table(null)));
    }

    static Stream<Optional<View>> viewMisses() {
        return Stream.of(Optional.empty(), Optional.of(view(OTHER_DATABASE_ID)), Optional.of(view(null)));
    }

    static Stream<UUID> invalidParents() {
        return Stream.of(OTHER_DATABASE_ID, null);
    }

    @Test
    void tableMatchingParent_returnsCachedObject() throws Exception {
        final Table cached = table(DATABASE_ID);
        when(tables.findById(CHILD_ID)).thenReturn(Optional.of(cached));

        assertSame(cached, service.getTable(DATABASE_ID, CHILD_ID));

        verifyNoInteractions(gateway);
        verify(tables, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("tableMisses")
    void tableCacheMiss_loadsRequestedDatabaseForSameId(Optional<Table> cached) throws Exception {
        final Table fetched = table(DATABASE_ID);
        when(tables.findById(CHILD_ID)).thenReturn(cached);
        when(gateway.getTableById(DATABASE_ID, CHILD_ID)).thenReturn(fetched);
        when(tables.save(fetched)).thenReturn(fetched);

        assertSame(fetched, service.getTable(DATABASE_ID, CHILD_ID));
        assertEquals(DATABASE_ID, fetched.getDatabaseId());
        assertEquals(60L, fetched.getExp());
        cached.ifPresent(value -> assertNotEquals(DATABASE_ID, value.getDatabaseId()));
        verify(gateway).getTableById(DATABASE_ID, CHILD_ID);
        verify(tables).save(fetched);
    }

    @ParameterizedTest
    @MethodSource("tableMisses")
    void tableMissingFromRequestedDatabase_neverReturnsCachedObject(Optional<Table> cached) throws Exception {
        when(tables.findById(CHILD_ID)).thenReturn(cached);
        when(gateway.getTableById(DATABASE_ID, CHILD_ID)).thenThrow(new TableNotFoundException("not found"));

        assertThrows(TableNotFoundException.class, () -> service.getTable(DATABASE_ID, CHILD_ID));

        verify(gateway).getTableById(DATABASE_ID, CHILD_ID);
        verify(tables, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("invalidParents")
    void tableFetchedWithWrongOrMissingParent_isNotCached(UUID parent) throws Exception {
        when(tables.findById(CHILD_ID)).thenReturn(Optional.empty());
        final Table fetched = table(parent);
        when(gateway.getTableById(DATABASE_ID, CHILD_ID)).thenReturn(fetched);

        assertThrows(TableNotFoundException.class, () -> service.getTable(DATABASE_ID, CHILD_ID));

        assertNull(fetched.getExp());
        verify(tables, never()).save(any());
    }

    @Test
    void viewMatchingParent_returnsCachedObject() throws Exception {
        final View cached = view(DATABASE_ID);
        when(views.findById(CHILD_ID)).thenReturn(Optional.of(cached));

        assertSame(cached, service.getView(DATABASE_ID, CHILD_ID));

        verifyNoInteractions(gateway);
        verify(views, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("viewMisses")
    void viewCacheMiss_loadsRequestedDatabaseForSameId(Optional<View> cached) throws Exception {
        final View fetched = view(DATABASE_ID);
        when(views.findById(CHILD_ID)).thenReturn(cached);
        when(gateway.getViewById(DATABASE_ID, CHILD_ID)).thenReturn(fetched);
        when(views.save(fetched)).thenReturn(fetched);

        assertSame(fetched, service.getView(DATABASE_ID, CHILD_ID));
        assertEquals(DATABASE_ID, fetched.getDatabaseId());
        assertEquals(60L, fetched.getExp());
        cached.ifPresent(value -> assertNotEquals(DATABASE_ID, value.getDatabaseId()));
        verify(gateway).getViewById(DATABASE_ID, CHILD_ID);
        verify(views).save(fetched);
    }

    @ParameterizedTest
    @MethodSource("viewMisses")
    void viewMissingFromRequestedDatabase_neverReturnsCachedObject(Optional<View> cached) throws Exception {
        when(views.findById(CHILD_ID)).thenReturn(cached);
        when(gateway.getViewById(DATABASE_ID, CHILD_ID)).thenThrow(new ViewNotFoundException("not found"));

        assertThrows(ViewNotFoundException.class, () -> service.getView(DATABASE_ID, CHILD_ID));

        verify(gateway).getViewById(DATABASE_ID, CHILD_ID);
        verify(views, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("invalidParents")
    void viewFetchedWithWrongOrMissingParent_isNotCached(UUID parent) throws Exception {
        when(views.findById(CHILD_ID)).thenReturn(Optional.empty());
        final View fetched = view(parent);
        when(gateway.getViewById(DATABASE_ID, CHILD_ID)).thenReturn(fetched);

        assertThrows(ViewNotFoundException.class, () -> service.getView(DATABASE_ID, CHILD_ID));

        assertNull(fetched.getExp());
        verify(views, never()).save(any());
    }

    private static Table table(UUID parent) {
        return Table.builder().id(CHILD_ID).databaseId(parent).build();
    }

    private static View view(UUID parent) {
        return View.builder().id(CHILD_ID).databaseId(parent).build();
    }
}
