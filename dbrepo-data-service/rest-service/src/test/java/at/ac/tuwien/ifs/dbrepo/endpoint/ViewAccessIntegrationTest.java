package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.ViewCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.ViewEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewNotFoundException;
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
class ViewAccessIntegrationTest extends BaseTest {
    @Container static MariaDBContainer<?> maria = MariaDbContainerConfig.getContainer();
    @Container static RedisContainerConfig.CustomRedisContainer redis = RedisContainerConfig.getContainer();
    @MockitoBean MetadataServiceGateway gateway;
    @Autowired ViewEndpoint endpoint;
    @Autowired DatabaseCacheRepository databases;
    @Autowired ViewCacheRepository views;

    @Autowired MetadataService metadata;
    private Database current;

    @BeforeEach
    void prepare() throws Exception {
        databases.deleteAll();
        views.deleteAll();
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        try (var connection = DriverManager.getConnection("jdbc:mariadb://" + CONTAINER_1_CACHE.getHost() + ":" +
                CONTAINER_1_CACHE.getPort() + "/" + DATABASE_1_INTERNAL_NAME,
                CONTAINER_1_CACHE.getUsername(), CONTAINER_1_CACHE.getPassword())) {
            connection.createStatement().execute("CREATE OR REPLACE VIEW junit AS " + VIEW_1_QUERY);
        }
        DATABASE_1_CACHE.setAccesses(List.of(at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
                .username(USER_2_USERNAME).type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.READ).build(),
                DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE));
        DATABASE_1_CACHE.setViews(List.of(VIEW_1_CACHE));
        current = DATABASE_1_CACHE;
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenAnswer(invocation -> current);
        when(gateway.getViewById(DATABASE_1_ID, VIEW_1_ID)).thenReturn(VIEW_1_CACHE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "csv", "head", "statistic"})
    void revokedNonOwnerCannotReadWarmedView(String format) throws Exception {
        assertNotEquals(USER_2_USERNAME, current.getOwnedBy());
        assertTrue(read("json", USER_2_PRINCIPAL).contains("Albury"));
        warm();
        current = snapshot(VIEW_1_CACHE, List.of(DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE));
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
    void publicViewBecomingPrivateDeniesAnonymousDespiteWarmCache(String format) throws Exception {
        VIEW_1_CACHE.setIsPublic(true);
        assertFalse(current.getIsPublic());
        assertTrue(read("json", null).contains("Albury"));
        warm();
        View privateView = View.builder().id(VIEW_1_ID).databaseId(DATABASE_1_ID).internalName(VIEW_1_INTERNAL_NAME)
                .isPublic(false).query(VIEW_1_QUERY).build();
        current = snapshot(privateView, List.of());
        clearInvocations(gateway);
        assertThrows(NotAllowedException.class, () -> read(format, null));
        verify(gateway, times(1)).getDatabaseById(DATABASE_1_ID);
        verifyNoMoreInteractions(gateway);
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "csv", "head", "statistic"})
    void currentPublicViewRemainsReadableWithOneMetadataRequest(String format) throws Exception {
        VIEW_1_CACHE.setIsPublic(true);
        clearInvocations(gateway);
        String result = read(format, null);
        if (format.equals("json") || format.equals("csv")) assertTrue(result.contains("Albury"));
        verify(gateway, times(1)).getDatabaseById(DATABASE_1_ID);
        verifyNoMoreInteractions(gateway);
    }

    @Test
    void wrongParentAndRemovedViewCannotUseWarmCache() throws Exception {
        warm();
        current = snapshot(View.builder().id(VIEW_1_ID).databaseId(UUID.randomUUID()).isPublic(true).build(), List.of());
        assertThrows(ViewNotFoundException.class, () -> read("json", USER_LOCAL_ADMIN_PRINCIPAL));
        current.getViews().getFirst().setDatabaseId(null);
        assertThrows(ViewNotFoundException.class, () -> read("csv", USER_LOCAL_ADMIN_PRINCIPAL));
        current.setViews(List.of());
        assertThrows(ViewNotFoundException.class, () -> read("head", USER_LOCAL_ADMIN_PRINCIPAL));
        current = snapshot(VIEW_1_CACHE, List.of());
        current.setId(UUID.randomUUID());
        assertThrows(ViewNotFoundException.class, () -> read("statistic", USER_LOCAL_ADMIN_PRINCIPAL));
    }

    @Test
    void currentMetadataFailureDoesNotFallBackToCache() throws Exception {
        warm();
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenThrow(new RemoteUnavailableException("unavailable"));
        assertThrows(RemoteUnavailableException.class, () -> read("json", USER_2_PRINCIPAL));
    }

    @Test
    void executionUsesCurrentViewQueryAndInternalName() throws Exception {
        warm();
        try (var connection = DriverManager.getConnection("jdbc:mariadb://" + CONTAINER_1_CACHE.getHost() + ":" +
                CONTAINER_1_CACHE.getPort() + "/" + DATABASE_1_INTERNAL_NAME,
                CONTAINER_1_CACHE.getUsername(), CONTAINER_1_CACHE.getPassword())) {
            connection.createStatement().execute("CREATE OR REPLACE VIEW current_view AS SELECT lat AS current_column FROM weather_location WHERE location = 'Albury'");
        }
        current = snapshot(View.builder().id(VIEW_1_ID).databaseId(DATABASE_1_ID).internalName("current_view")
                .isPublic(true).query("SELECT lat AS current_column FROM weather_location WHERE location = 'Albury'").build(), List.of());
        String json = read("json", null);
        assertTrue(json.contains("current_column"));
        assertFalse(json.contains("Albury"));
        var head = endpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null,
                new MockHttpServletRequest("HEAD", "/"), "application/json", null);
        assertEquals("1", head.getHeaders().getFirst("X-Count"));
        assertTrue(endpoint.getStatistic(DATABASE_1_ID, VIEW_1_ID, null).getBody().toString().contains("current_column"));
    }

    private void warm() throws Exception {
        metadata.getDatabase(DATABASE_1_ID);
        metadata.getView(DATABASE_1_ID, VIEW_1_ID);
        assertTrue(databases.findById(DATABASE_1_ID).isPresent());
        assertTrue(views.findById(VIEW_1_ID).isPresent());
    }

    private Database snapshot(View view, List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess> accesses) {
        return Database.builder().id(DATABASE_1_ID).internalName(DATABASE_1_INTERNAL_NAME)
                .container(CONTAINER_1_CACHE).isPublic(false).isSchemaPublic(false).ownedBy(USER_1_USERNAME)
                .accesses(accesses).views(List.of(view)).tables(DATABASE_1_CACHE.getTables()).build();
    }

    private String read(String format, Principal principal) throws Exception {
        if (format.equals("statistic")) {
            assertEquals(200, endpoint.getStatistic(DATABASE_1_ID, VIEW_1_ID, principal).getStatusCode().value());
            return "";
        }
        String accept = format.equals("csv") ? "text/csv" : "application/json";
        var response = endpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null,
                new MockHttpServletRequest(format.equals("head") ? "HEAD" : "GET", "/"), accept, principal);
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
