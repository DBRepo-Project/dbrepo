package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.SubsetCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.SubsetType;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubsetServiceCacheUnitTest {

    private static final UUID DATABASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_DATABASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID QUERY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final Database database = Database.builder().id(DATABASE_ID).internalName("requested_database").build();
    private SubsetCacheRepository cache;
    private DataMapper mapper;
    private ComboPooledDataSource dataSource;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private SubsetServiceMariaDbImpl service;

    @BeforeEach
    void setUp() throws Exception {
        cache = mock(SubsetCacheRepository.class);
        mapper = mock(DataMapper.class);
        dataSource = mock(ComboPooledDataSource.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        final Connection connection = mock(Connection.class);
        final MariaDbMapper sqlMapper = mock(MariaDbMapper.class);
        when(sqlMapper.queryStoreFindQueryRawQuery()).thenReturn("SELECT * FROM query_store WHERE id = ?");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT * FROM query_store WHERE id = ?")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        service = spy(new SubsetServiceMariaDbImpl(mock(DSLContext.class), mapper, sqlMapper,
                mock(MetadataMapper.class), cache, mock(MetadataServiceGateway.class), mock(SubsetReplicationService.class)));
        doReturn(dataSource).when(service).getDataSource(database);
    }

    static Stream<Optional<Subset>> cacheMisses() {
        return Stream.of(Optional.empty(), Optional.of(subset(OTHER_DATABASE_ID)), Optional.of(subset(null)), Optional.of(subset(DATABASE_ID)));
    }

    @ParameterizedTest
    @MethodSource("cacheMisses")
    void miss_loadsSameQueryIdFromRequestedDatabase(Optional<Subset> cached) throws Exception {
        final Subset fetched = Subset.builder().id(QUERY_ID).query("SELECT requested_data").build();
        when(cache.findById(QUERY_ID)).thenReturn(cached);
        when(resultSet.next()).thenReturn(true);
        when(mapper.resultSetToSubset(resultSet)).thenReturn(fetched);

        assertSame(fetched, service.findById(database, QUERY_ID));
        assertEquals(DATABASE_ID, fetched.getDatabaseId());
        assertEquals(SubsetType.QUERY, fetched.getType());
        verify(cache, never()).findById(any());
        verify(service).getDataSource(database);
        verify(statement).setString(1, QUERY_ID.toString());
        verify(cache, never()).save(any());
        verify(dataSource).close();
    }

    @ParameterizedTest
    @MethodSource("cacheMisses")
    void queryMissingFromRequestedDatabase_neverReturnsCachedObject(Optional<Subset> cached) throws Exception {
        when(cache.findById(QUERY_ID)).thenReturn(cached);
        when(resultSet.next()).thenReturn(false);

        assertThrows(QueryNotFoundException.class, () -> service.findById(database, QUERY_ID));

        verify(service).getDataSource(database);
        verify(statement).setString(1, QUERY_ID.toString());
        verify(cache, never()).save(any());
        verifyNoInteractions(mapper);
        verify(dataSource).close();
    }

    private static Subset subset(UUID parent) {
        return Subset.builder().id(QUERY_ID).databaseId(parent).query("SELECT cached_data").build();
    }
}
