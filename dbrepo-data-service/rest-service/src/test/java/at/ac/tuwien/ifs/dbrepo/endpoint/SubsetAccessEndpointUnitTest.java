package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.api.SubsetMetadata;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.JoinDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.JoinTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.DataService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.QueryResultStream;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import at.ac.tuwien.ifs.dbrepo.validation.SubsetAccessValidator;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubsetAccessEndpointUnitTest extends BaseTest {
    private final UUID databaseId = UUID.randomUUID();
    private final UUID subsetId = UUID.randomUUID();
    private final Table publicTable = Table.builder().id(UUID.randomUUID()).internalName("public_rows").isPublic(true).columns(List.of()).build();
    private final Table privateTable = Table.builder().id(UUID.randomUUID()).internalName("private_rows").isPublic(false).columns(List.of()).build();
    private final View publicView = View.builder().id(UUID.randomUUID()).internalName("public_view").isPublic(true)
            .query("SELECT * FROM private_rows").columns(List.of()).build();
    private final View privateView = View.builder().id(UUID.randomUUID()).internalName("private_view").isPublic(false).columns(List.of()).build();
    private final Database database = Database.builder().id(databaseId).internalName("testdb").isPublic(true)
            .accesses(List.of()).tables(List.of(publicTable, privateTable)).views(List.of(publicView, privateView)).build();
    private final SubsetService subsets = mock(SubsetService.class);
    private final DataService data = mock(DataService.class);
    private final MetadataService cache = mock(MetadataService.class);
    private final MetadataServiceGateway gateway = mock(MetadataServiceGateway.class);
    private final SubsetEndpoint endpoint = new SubsetEndpoint(data, mock(MariaDbMapper.class, CALLS_REAL_METHODS),
            subsets, null, cache, new EndpointValidator(), gateway, new SubsetAccessValidator(), DSL.using(SQLDialect.MARIADB));

    @BeforeEach
    void prepare() throws Exception {
        when(gateway.getDatabaseById(databaseId)).thenReturn(database);
        when(data.query(any(), anyString())).thenReturn(mock(QueryResultStream.class));
        when(subsets.getMetadata(any(), anyString())).thenReturn(SubsetMetadata.builder().resultCount(1L).build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "find", "persist"})
    void metadataOperationsRejectCurrentRevocationBeforeQueryStore(String operation) throws Exception {
        when(cache.getDatabase(databaseId)).thenReturn(DATABASE_1_CACHE);
        database.setIsPublic(false);
        database.setAccesses(List.of());
        assertThrows(NotAllowedException.class, () -> metadataOperation(operation));
        verify(gateway).getDatabaseById(databaseId);
        verifyNoInteractions(cache, subsets, data);
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "find", "persist"})
    void metadataOperationsDoNotFallBackAfterMetadataFailure(String operation) throws Exception {
        when(cache.getDatabase(databaseId)).thenReturn(DATABASE_1_CACHE);
        when(gateway.getDatabaseById(databaseId)).thenThrow(new at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException("unavailable"));
        assertThrows(at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException.class, () -> metadataOperation(operation));
        verify(gateway).getDatabaseById(databaseId);
        verifyNoInteractions(cache, subsets, data);
    }

    private void metadataOperation(String operation) throws Exception {
        switch (operation) {
            case "list" -> endpoint.list(databaseId, null, USER_2_PRINCIPAL);
            case "find" -> endpoint.findById(databaseId, subsetId, null, USER_2_PRINCIPAL);
            case "persist" -> endpoint.persist(databaseId, subsetId,
                    at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryPersistDto.builder().persist(true).build(), USER_2_PRINCIPAL);
            default -> throw new IllegalArgumentException(operation);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"table", "view", "tableJoin", "viewJoin", "unknown", "empty"})
    void createDeniesUnreadableSourcesBeforeStoring(String kind) throws Exception {
        UUID id = kind.startsWith("view") ? privateView.getId() : privateTable.getId();
        Set<UUID> ids = kind.equals("empty") ? Set.of() : Set.of(kind.equals("unknown") ? UUID.randomUUID() : id);
        SubsetDto request = SubsetDto.builder().datasourceIds(ids).columns(Set.of()).build();
        if (kind.endsWith("Join")) {
            request.setDatasourceIds(Set.of(publicTable.getId()));
            request.setJoins(Set.of(JoinDto.builder().datasourceId(id).type(JoinTypeDto.INNER).conditionals(Set.of()).build()));
        }
        assertThrows(NotAllowedException.class, () -> endpoint.create(databaseId, request, USER_4_PRINCIPAL,
                new MockHttpServletRequest("POST", "/"), Instant.now(), 0L, 10L));
        verifyNoInteractions(subsets, data, cache);
        verify(gateway).getDatabaseById(databaseId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"table", "view", "grant"})
    void createChecksFreshSourcesAgainBeforeReturningData(String kind) throws Exception {
        boolean view = kind.equals("view");
        UUID id = view ? publicView.getId() : publicTable.getId();
        String source = view ? publicView.getInternalName() : publicTable.getInternalName();
        if (kind.equals("grant")) {
            database.setAccesses(DATABASE_1_CACHE.getAccesses());
            id = privateTable.getId();
            source = privateTable.getInternalName();
        }
        when(subsets.create(eq(database), any(), any(), any())).thenReturn(subsetId);
        stored("SELECT * FROM " + source);
        assertEquals(201, endpoint.create(databaseId, SubsetDto.builder().datasourceIds(Set.of(id)).columns(Set.of()).build(),
                USER_1_PRINCIPAL, new MockHttpServletRequest("POST", "/"), Instant.now(), 0L, 10L).getStatusCode().value());
        verify(gateway, times(2)).getDatabaseById(databaseId);
        verifyNoInteractions(cache);
        verify(data).query(eq(database), anyString());
    }

    @ParameterizedTest
    @CsvSource({"GET,application/json", "GET,text/csv", "HEAD,application/json"})
    void replayDeniesPrivateNormalizedQueryBeforeReading(String method, String accept) throws Exception {
        // The unversioned text is public, but the actual executed statement reads a private source.
        when(subsets.findById(database, subsetId)).thenReturn(Subset.builder().query("SELECT * FROM public_rows")
                .queryNormalized("SELECT * FROM private_rows").build());
        assertThrows(NotAllowedException.class, () -> endpoint.getData(databaseId, subsetId, null, accept,
                new MockHttpServletRequest(method, "/"), null, null, null));
        verify(data, never()).query(any(), anyString());
        verify(subsets, never()).getMetadata(any(), anyString());
        verifyNoInteractions(cache);
    }

    @ParameterizedTest
    @CsvSource({"GET,application/json", "GET,text/csv", "HEAD,application/json"})
    void replayRefreshesVisibilityAfterAnAllowedRequest(String method, String accept) throws Exception {
        stored("SELECT * FROM public_rows");
        when(cache.getDatabase(databaseId)).thenReturn(database);
        Database changed = Database.builder().id(databaseId).internalName("testdb").isPublic(true).accesses(List.of())
                .tables(List.of(Table.builder().id(publicTable.getId()).internalName("public_rows").isPublic(false).build()))
                .views(List.of()).build();
        when(gateway.getDatabaseById(databaseId)).thenReturn(database, changed);
        when(subsets.findById(changed, subsetId)).thenReturn(Subset.builder().queryNormalized("SELECT * FROM public_rows").build());
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/");
        assertEquals(200, endpoint.getData(databaseId, subsetId, null, accept, request, null, null, null).getStatusCode().value());
        clearInvocations(data, subsets);
        assertThrows(NotAllowedException.class, () -> endpoint.getData(databaseId, subsetId, null, accept, request, null, null, null));
        verify(data, never()).query(any(), anyString());
        verify(subsets, never()).getMetadata(any(), anyString());
        verify(gateway, times(2)).getDatabaseById(databaseId);
        verifyNoInteractions(cache);
    }

    @ParameterizedTest
    @CsvSource({"GET,application/json", "GET,text/csv", "HEAD,application/json"})
    void replayRefreshesRevokedGrant(String method, String accept) throws Exception {
        database.setAccesses(DATABASE_1_CACHE.getAccesses());
        stored("SELECT * FROM private_rows");
        Database revoked = Database.builder().id(databaseId).internalName("testdb").isPublic(true).accesses(List.of())
                .tables(database.getTables()).views(database.getViews()).build();
        when(cache.getDatabase(databaseId)).thenReturn(database);
        when(gateway.getDatabaseById(databaseId)).thenReturn(database, revoked);
        when(subsets.findById(revoked, subsetId)).thenReturn(Subset.builder().queryNormalized("SELECT * FROM private_rows").build());
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/");
        assertEquals(200, endpoint.getData(databaseId, subsetId, USER_1_PRINCIPAL, accept, request, null, null, null).getStatusCode().value());
        clearInvocations(data, subsets);
        assertThrows(NotAllowedException.class, () -> endpoint.getData(databaseId, subsetId, USER_1_PRINCIPAL, accept, request, null, null, null));
        verify(data, never()).query(any(), anyString());
        verify(subsets, never()).getMetadata(any(), anyString());
        verify(gateway, times(2)).getDatabaseById(databaseId);
        verifyNoInteractions(cache);
    }

    @ParameterizedTest
    @CsvSource({"GET,application/json", "GET,text/csv", "HEAD,application/json"})
    void namedPublicViewRemainsReadable(String method, String accept) throws Exception {
        stored("SELECT * FROM public_view");
        assertEquals(200, endpoint.getData(databaseId, subsetId, null, accept,
                new MockHttpServletRequest(method, "/"), null, null, null).getStatusCode().value());
    }

    @Test
    void freshPrivateDatabaseGateStillAppliesToPublicSources() throws Exception {
        database.setIsPublic(false);
        stored("SELECT * FROM public_rows");
        assertThrows(NotAllowedException.class, () -> endpoint.getData(databaseId, subsetId, USER_4_PRINCIPAL,
                "application/json", new MockHttpServletRequest("GET", "/"), null, null, null));
        verifyNoInteractions(subsets, data, cache);
    }

    private void stored(String query) throws Exception {
        when(subsets.findById(database, subsetId)).thenReturn(Subset.builder().queryNormalized(query).build());
    }
}
