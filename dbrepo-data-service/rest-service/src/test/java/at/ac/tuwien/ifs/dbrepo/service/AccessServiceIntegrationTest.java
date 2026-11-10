package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.impl.AccessServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class AccessServiceIntegrationTest extends BaseTest {

    @Autowired
    private AccessService accessService;

    @MockitoBean
    private DataService dataService;

    @Autowired
    private at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper mariaDbMapper;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Container
    private static RedisContainerConfig.CustomRedisContainer redisContainer = RedisContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
    }

    @Test
    public void create_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void create_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.WRITE_OWN);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void create_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.WRITE_ALL);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.WRITE_OWN);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.WRITE_ALL);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_notFound_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            accessService.update(DATABASE_1_CACHE, USER_5_CACHE, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void delete_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.delete(DATABASE_1_CACHE, USER_1_CACHE);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(new String[]{"USAGE"}, privileges.toArray());
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            accessService.delete(DATABASE_1_CACHE, USER_5_CACHE);
        });
    }

    @Test
    public void broadConfiguredGrants_areRejected() {
        for (String grants : new String[]{"SELECT, EXECUTE", "ALL", "ALL PRIVILEGES", "ALL    PRIVILEGES", "ALL/**/PRIVILEGES", "SELECT, /*! EXECUTE */"}) {
            assertThrows(IllegalArgumentException.class, () -> mariaDbMapper.databaseGrantPrivilegesQuery(
                    DATABASE_1_INTERNAL_NAME, USER_1_USERNAME, grants));
        }
    }

    @Test
    public void reader_cannotExecuteInternalRoutinesOrReadAnotherDatabase() throws Exception {
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        try (Connection admin = adminConnection(); Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS access_private_probe");
            statement.execute("CREATE TABLE IF NOT EXISTS access_private_probe.secret (value VARCHAR(32))");
            try (Connection reader = readerConnection(); Statement query = reader.createStatement()) {
                assertTrue(query.executeQuery("SELECT COUNT(*) FROM qs_queries").next());
                assertThrows(SQLException.class, () -> query.executeQuery("SELECT * FROM access_private_probe.secret"));
                assertInternalCallsDenied(query);
                assertEquals(1370, assertThrows(SQLException.class, () -> query.execute(
                        "CALL store_query('SELECT * FROM access_private_probe.secret', 'SELECT * FROM access_private_probe.secret', NOW(), @id)")).getErrorCode());
            } finally {
                statement.execute("DROP DATABASE access_private_probe");
            }
            statement.execute("CALL _store_query('service', 'SELECT 1 AS value', 'SELECT 1 AS value', NOW(), @id)");
            try (var result = statement.executeQuery("SELECT @id")) {
                assertTrue(result.next());
                assertNotNull(result.getString(1));
            }
        }
    }

    @Test
    public void update_removesLegacyDatabaseAndRoutineExecuteGrants() throws Exception {
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        addLegacyRoutineGrants();
        accessService.update(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        try (Connection reader = readerConnection(); Statement query = reader.createStatement()) {
            assertTrue(query.executeQuery("SELECT COUNT(*) FROM qs_queries").next());
            assertInternalCallsDenied(query);
        }
    }

    @Test
    public void update_disconnectsSessionsWithCachedExecutePrivileges() throws Exception {
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        addLegacyRoutineGrants();
        try (Connection existing = readerConnection(); Statement query = existing.createStatement()) {
            query.execute("CALL store_query('SELECT 1 AS value', 'SELECT 1 AS value', NOW(), @id)");
            accessService.update(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
            assertThrows(SQLException.class, () -> query.execute("SELECT 1"));
        }
        try (Connection fresh = readerConnection(); Statement query = fresh.createStatement()) {
            assertTrue(query.executeQuery("SELECT 1").next());
            assertInternalCallsDenied(query);
        }
    }

    @Test
    public void update_disconnectsSessionsWithServerPreparedStatementsEnabled() throws Exception {
        AccessServiceMariaDbImpl service = new AccessServiceMariaDbImpl(mariaDbMapper) {
            @Override
            public ComboPooledDataSource getDataSource(Database database) {
                ComboPooledDataSource source = super.getDataSource(database);
                source.setJdbcUrl(source.getJdbcUrl() + "&useServerPrepStmts=true");
                return source;
            }
        };
        ReflectionTestUtils.setField(service, "grantDefaultRead", "SELECT");
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        addLegacyRoutineGrants();
        try (Connection existing = readerConnection(); Statement query = existing.createStatement()) {
            query.execute("CALL store_query('SELECT 1 AS value', 'SELECT 1 AS value', NOW(), @id)");
            service.update(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
            assertThrows(SQLException.class, () -> query.execute("SELECT 1"));
        }
        try (Connection fresh = readerConnection(); Statement query = fresh.createStatement()) {
            assertInternalCallsDenied(query);
        }
    }

    @Test
    public void delete_removesLegacyRoutineGrantsForNewConnections() throws Exception {
        accessService.create(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);
        addLegacyRoutineGrants();
        accessService.delete(DATABASE_1_CACHE, USER_1_CACHE);
        try (Connection reader = DriverManager.getConnection(
                "jdbc:mariadb://" + CONTAINER_1_CACHE.getHost() + ":" + CONTAINER_1_CACHE.getPort() + "?sslMode=disable",
                USER_1_USERNAME, USER_1_PASSWORD); Statement query = reader.createStatement()) {
            assertThrows(SQLException.class, () -> query.executeQuery("SELECT * FROM `" + DATABASE_1_INTERNAL_NAME + "`.qs_queries"));
            assertEquals(1370, assertThrows(SQLException.class, () -> query.execute(
                    "CALL `" + DATABASE_1_INTERNAL_NAME + "`.store_query('SELECT 1', 'SELECT 1', NOW(), @id)")).getErrorCode());
        }
    }

    private Connection adminConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://" + CONTAINER_1_CACHE.getHost() + ":"
                + CONTAINER_1_CACHE.getPort() + "/" + DATABASE_1_INTERNAL_NAME + "?sslMode=disable",
                CONTAINER_1_CACHE.getUsername(), CONTAINER_1_CACHE.getPassword());
    }

    private Connection readerConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://" + CONTAINER_1_CACHE.getHost() + ":"
                + CONTAINER_1_CACHE.getPort() + "/" + DATABASE_1_INTERNAL_NAME + "?sslMode=disable",
                USER_1_USERNAME, USER_1_PASSWORD);
    }

    private void addLegacyRoutineGrants() throws SQLException {
        try (Connection admin = adminConnection(); Statement statement = admin.createStatement()) {
            statement.execute("GRANT EXECUTE ON `" + DATABASE_1_INTERNAL_NAME + "`.* TO '" + USER_1_USERNAME + "'@'%'");
            for (String name : new String[]{"store_query", "_store_query", "hash_query", "hash_table"}) {
                // Reproduce the legacy definer routines before testing revocation of warm sessions.
                statement.execute("ALTER PROCEDURE `" + DATABASE_1_INTERNAL_NAME + "`." + name + " SQL SECURITY DEFINER");
                statement.execute("GRANT EXECUTE ON PROCEDURE `" + DATABASE_1_INTERNAL_NAME + "`." + name + " TO '" + USER_1_USERNAME + "'@'%'");
            }
        }
    }

    private void assertInternalCallsDenied(Statement query) {
        for (String call : new String[]{
                "CALL store_query('SELECT 1', 'SELECT 1', NOW(), @id)",
                "CALL _store_query('user', 'SELECT 1', 'SELECT 1', NOW(), @id)",
                "CALL hash_query('SELECT 1', @hash)", "CALL hash_table('qs_queries', @hash, @count)"}) {
            assertEquals(1370, assertThrows(SQLException.class, () -> query.execute(call)).getErrorCode());
        }
    }

}
