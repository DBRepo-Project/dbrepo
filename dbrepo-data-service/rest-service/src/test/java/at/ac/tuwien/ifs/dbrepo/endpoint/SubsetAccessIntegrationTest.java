package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.cache.SubsetCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.DataService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataServiceSparkImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import at.ac.tuwien.ifs.dbrepo.validation.SubsetAccessValidator;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
class SubsetAccessIntegrationTest extends BaseTest {
    @Container
    private static final MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    private final MetadataServiceGateway gateway = mock(MetadataServiceGateway.class);
    private final MetadataService metadataCache = mock(MetadataService.class);
    private final Map<UUID, Subset> subsetCache = new HashMap<>();
    private Database current;
    private SubsetEndpoint endpoint;
    private SubsetService subsets;
    private DataService data;

    @BeforeEach
    void prepare() throws Exception {
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        current = catalog(false, true);
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenAnswer(invocation -> current);
        when(metadataCache.getDatabase(DATABASE_1_ID)).thenReturn(current);
        SubsetCacheRepository repository = mock(SubsetCacheRepository.class);
        when(repository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(subsetCache.get(invocation.getArgument(0))));
        when(repository.save(any())).thenAnswer(invocation -> {
            Subset subset = invocation.getArgument(0);
            subsetCache.put(subset.getId(), subset);
            return subset;
        });
        MariaDbMapper mapper = mock(MariaDbMapper.class, CALLS_REAL_METHODS);
        var context = DSL.using(SQLDialect.MARIADB);
        subsets = spy(new SubsetServiceMariaDbImpl(context, mock(DataMapper.class, CALLS_REAL_METHODS),
                mapper, null, repository, gateway));
        data = spy(new DataServiceSparkImpl(null, null));
        endpoint = new SubsetEndpoint(data, mapper, subsets, null, metadataCache, new EndpointValidator(), gateway,
                new SubsetAccessValidator(), context);
    }

    @Test
    void privateSourceIsRejectedBeforeQueryStoreExecution() throws Exception {
        assertEquals(0, storedCount());
        assertThrows(NotAllowedException.class, () -> endpoint.create(DATABASE_1_ID, QUERY_1_SUBSET_DTO,
                USER_4_PRINCIPAL, new MockHttpServletRequest("POST", "/"), Instant.now(), 0L, 10L));
        assertEquals(0, storedCount());
        verifyNoInteractions(subsets, data, metadataCache);
    }

    @ParameterizedTest
    @ValueSource(strings = {"filter", "alias"})
    void unsupportedGeneratedSqlIsRejectedBeforeQueryStoreExecution(String kind) throws Exception {
        current = catalog(true, false);
        SubsetDto request = SubsetDto.builder().datasourceIds(Set.of(TABLE_1_ID))
                .columns(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID)
                        .alias(kind.equals("alias") ? "unsupported\\alias" : null).build())).build();
        if (kind.equals("filter")) {
            request.setFilters(List.of(FilterDto.builder().type(FilterTypeDto.WHERE).columnId(COLUMN_1_1_ID)
                    .operatorId(IMAGE_1_OPERATORS_2_ID).value("unsupported\\value").build()));
        }
        assertThrows(NotAllowedException.class, () -> endpoint.create(DATABASE_1_ID, request, USER_4_PRINCIPAL,
                new MockHttpServletRequest("POST", "/"), Instant.now(), 0L, 10L));
        assertEquals(0, storedCount());
        verifyNoInteractions(subsets, data, metadataCache);
    }

    @Test
    void cachedSubsetUsesCurrentGrantsForEveryReplayMode() throws Exception {
        ResponseEntity<StreamingResponseBody> created = endpoint.create(DATABASE_1_ID, QUERY_1_SUBSET_DTO,
                USER_1_PRINCIPAL, new MockHttpServletRequest("POST", "/"), Instant.now(), 0L, 10L);
        assertEquals(201, created.getStatusCode().value());
        assertTrue(body(created).contains("Albury"));
        UUID id = UUID.fromString(created.getHeaders().getFirst("X-Id"));
        assertTrue(subsetCache.containsKey(id));
        current = catalog(false, false);
        clearInvocations(subsets, data);
        for (String mode : List.of("json", "csv", "head")) {
            assertThrows(NotAllowedException.class, () -> replay(id, mode));
        }
        verify(data, never()).query(any(), anyString());
        verify(subsets, never()).getMetadata(any(), anyString());
        verifyNoInteractions(metadataCache);
        current = catalog(false, true);
        assertTrue(body(replay(id, "json")).contains("Albury"));
        assertTrue(body(replay(id, "csv")).contains("Albury"));
        assertEquals("3", replay(id, "head").getHeaders().getFirst("X-Count"));
    }

    @Test
    void publicSourceCanBeCreatedWithoutGrantAndLaterMadePrivate() throws Exception {
        current = catalog(true, false);
        ResponseEntity<StreamingResponseBody> created = endpoint.create(DATABASE_1_ID, QUERY_1_SUBSET_DTO,
                USER_4_PRINCIPAL, new MockHttpServletRequest("POST", "/"), Instant.now(), 0L, 10L);
        assertTrue(body(created).contains("Albury"));
        UUID id = UUID.fromString(created.getHeaders().getFirst("X-Id"));
        current = catalog(false, false);
        clearInvocations(subsets, data);
        assertThrows(NotAllowedException.class, () -> endpoint.getData(DATABASE_1_ID, id, USER_4_PRINCIPAL,
                "application/json", new MockHttpServletRequest("GET", "/"), null, null, null));
        verify(data, never()).query(any(), anyString());
        verify(subsets, never()).getMetadata(any(), anyString());
        verifyNoInteractions(metadataCache);
    }

    private ResponseEntity<StreamingResponseBody> replay(UUID id, String mode) throws Exception {
        return endpoint.getData(DATABASE_1_ID, id, USER_1_PRINCIPAL, mode.equals("csv") ? "text/csv" : "application/json",
                new MockHttpServletRequest(mode.equals("head") ? "HEAD" : "GET", "/"), null, null, null);
    }

    private String body(ResponseEntity<StreamingResponseBody> response) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertNotNull(response.getBody());
        response.getBody().writeTo(output);
        return output.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Database catalog(boolean visible, boolean granted) {
        Table table = Table.builder().id(TABLE_1_ID).internalName(TABLE_1_INTERNAL_NAME).isPublic(visible)
                .columns(TABLE_1_CACHE.getColumns()).build();
        return Database.builder().id(DATABASE_1_ID).internalName(DATABASE_1_INTERNAL_NAME).isPublic(true)
                .tables(List.of(table)).views(List.of()).container(CONTAINER_1_CACHE)
                .accesses(granted ? DATABASE_1_CACHE.getAccesses() : List.of()).build();
    }

    private int storedCount() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:mariadb://" + CONTAINER_1_CACHE.getHost() + ":" +
                CONTAINER_1_CACHE.getPort() + "/" + DATABASE_1_INTERNAL_NAME,
                CONTAINER_1_CACHE.getUsername(), CONTAINER_1_CACHE.getPassword());
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM qs_queries")) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }
}
