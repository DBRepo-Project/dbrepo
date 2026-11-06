package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.TableCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.TableEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.io.ByteArrayOutputStream;
import java.sql.DriverManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class TableAccessIntegrationTest extends BaseTest {
    @Container static MariaDBContainer<?> maria = MariaDbContainerConfig.getContainer();
    @Container static RedisContainerConfig.CustomRedisContainer redis = RedisContainerConfig.getContainer();
    @MockitoBean MetadataServiceGateway gateway;
    @Autowired TableEndpoint endpoint;
    @Autowired DatabaseCacheRepository databases;
    @Autowired TableCacheRepository tables;

    @Autowired MetadataService metadata;
    private Database current;

    @BeforeEach
    void prepare() throws Exception {
        databases.deleteAll();
        tables.deleteAll();
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        DATABASE_1_CACHE.setAccesses(List.of(at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
                .username(USER_2_USERNAME).type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.READ).build(),
                DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE));
        DATABASE_1_CACHE.setTables(List.of(TABLE_2_CACHE));
        current = DATABASE_1_CACHE;
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenAnswer(invocation -> current);
        when(gateway.getTableById(DATABASE_1_ID, TABLE_2_ID)).thenReturn(TABLE_2_CACHE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "csv", "head", "statistic"})
    void revokedNonOwnerCannotReadWarmedTable(String format) throws Exception {
        assertNotEquals(USER_2_USERNAME, current.getOwnedBy());
        assertTrue(read("json", USER_2_PRINCIPAL).contains("Albury"));
        warm();
        current = snapshot(TABLE_2_CACHE, List.of(DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE));
        clearInvocations(gateway);
        assertThrows(NotAllowedException.class, () -> read(format, USER_2_PRINCIPAL));
        verify(gateway, times(1)).getDatabaseById(DATABASE_1_ID);
        verifyNoMoreInteractions(gateway);
        assertEquals(USER_1_USERNAME, current.getOwnedBy());
        assertTrue(read("json", USER_3_PRINCIPAL).contains("Albury"));
        assertTrue(read("json", USER_LOCAL_ADMIN_PRINCIPAL).contains("Albury"));
        assertTrue(databases.findById(DATABASE_1_ID).orElseThrow().getAccesses().stream()
                .anyMatch(access -> USER_2_USERNAME.equals(access.getUsername())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "csv", "head", "statistic"})
    void publicTableBecomingPrivateDeniesAnonymousDespiteWarmCache(String format) throws Exception {
        TABLE_2_CACHE.setIsPublic(true);
        assertFalse(current.getIsPublic());
        assertTrue(read("json", null).contains("Albury"));
        warm();
        Table privateTable = Table.builder().id(TABLE_2_ID).databaseId(DATABASE_1_ID).internalName(TABLE_2_INTERNAL_NAME)
                .isPublic(false).build();
        current = snapshot(privateTable, List.of());
        clearInvocations(gateway);
        assertThrows(NotAllowedException.class, () -> read(format, null));
        verify(gateway, times(1)).getDatabaseById(DATABASE_1_ID);
        verifyNoMoreInteractions(gateway);
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "csv", "head", "statistic"})
    void currentPublicTableRemainsReadableWithOneMetadataRequest(String format) throws Exception {
        TABLE_2_CACHE.setIsPublic(true);
        clearInvocations(gateway);
        String result = read(format, null);
        if (format.equals("json") || format.equals("csv")) assertTrue(result.contains("Albury"));
        verify(gateway, times(1)).getDatabaseById(DATABASE_1_ID);
        verifyNoMoreInteractions(gateway);
    }

    @Test
    void wrongParentAndRemovedTableCannotUseWarmCache() throws Exception {
        warm();
        current = snapshot(Table.builder().id(TABLE_2_ID).databaseId(UUID.randomUUID()).isPublic(true).build(), List.of());
        assertThrows(TableNotFoundException.class, () -> read("json", USER_LOCAL_ADMIN_PRINCIPAL));
        current.getTables().getFirst().setDatabaseId(null);
        assertThrows(TableNotFoundException.class, () -> read("csv", USER_LOCAL_ADMIN_PRINCIPAL));
        current.setTables(List.of());
        assertThrows(TableNotFoundException.class, () -> read("head", USER_LOCAL_ADMIN_PRINCIPAL));
        current = snapshot(TABLE_2_CACHE, List.of());
        current.setId(UUID.randomUUID());
        assertThrows(TableNotFoundException.class, () -> read("statistic", USER_LOCAL_ADMIN_PRINCIPAL));
    }

    @Test
    void currentMetadataFailureDoesNotFallBackToCache() throws Exception {
        warm();
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenThrow(new RemoteUnavailableException("unavailable"));
        assertThrows(RemoteUnavailableException.class, () -> read("json", USER_2_PRINCIPAL));
    }

    @Test
    void executionUsesCurrentTableName() throws Exception {
        warm();
        current = snapshot(Table.builder().id(TABLE_2_ID).databaseId(DATABASE_1_ID).internalName(TABLE_1_INTERNAL_NAME)
                .isPublic(true).build(), List.of());
        String json = read("json", null);
        assertTrue(json.contains("mintemp"));
        assertFalse(json.contains("lat"));
        var head = endpoint.getData(DATABASE_1_ID, TABLE_2_ID, null, null, null, null, null,
                "application/json", new MockHttpServletRequest("HEAD", "/"), null);
        assertEquals("3", head.getHeaders().getFirst("X-Count"));
        assertTrue(endpoint.getStatistic(DATABASE_1_ID, TABLE_2_ID, null).getBody().toString().contains("mintemp"));
    }

    private void warm() throws Exception {
        metadata.getDatabase(DATABASE_1_ID);
        metadata.getTable(DATABASE_1_ID, TABLE_2_ID);
        assertTrue(databases.findById(DATABASE_1_ID).isPresent());
        assertTrue(tables.findById(TABLE_2_ID).isPresent());
    }

    private Database snapshot(Table view, List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess> accesses) {
        return Database.builder().id(DATABASE_1_ID).internalName(DATABASE_1_INTERNAL_NAME)
                .container(CONTAINER_1_CACHE).isPublic(false).isSchemaPublic(false).ownedBy(USER_1_USERNAME)
                .accesses(accesses).tables(List.of(view)).views(List.of()).build();
    }

    private String read(String format, Principal principal) throws Exception {
        if (format.equals("statistic")) {
            assertEquals(200, endpoint.getStatistic(DATABASE_1_ID, TABLE_2_ID, principal).getStatusCode().value());
            return "";
        }
        String accept = format.equals("csv") ? "text/csv" : "application/json";
        var response = endpoint.getData(DATABASE_1_ID, TABLE_2_ID, null, null, null, null, null, accept,
                new MockHttpServletRequest(format.equals("head") ? "HEAD" : "GET", "/"), principal);
        assertEquals(200, response.getStatusCode().value());
        if (format.equals("head")) {
            assertNotNull(response.getHeaders().getFirst("X-Count"));
            return "";
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        response.getBody().writeTo(bytes);
        return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
