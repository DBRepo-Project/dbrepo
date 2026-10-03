package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.AccessServiceMariaDbImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "REPLICA_SQL_TEST_PORT", matches = "[0-9]+")
class ReplicaSqlAccessIntegrationTest {

    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("REPLICA_SQL_TEST_PORT");
    private final String password = System.getenv("REPLICA_SQL_TEST_PASSWORD");
    private final AccessServiceMariaDbImpl service = new AccessServiceMariaDbImpl(Mappers.getMapper(MariaDbMapper.class));
    private final User user = User.builder().id(UUID.randomUUID()).username("replica_access_reader").password("test-reader").build();
    private Database database;

    @BeforeEach
    void setup() throws Exception {
        database = Database.builder().id(UUID.randomUUID()).internalName("replica_access_test")
                .creationLocation("https://origin.example")
                .container(Container.builder().host("127.0.0.1").port(Integer.valueOf(System.getenv("REPLICA_SQL_TEST_PORT")))
                        .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build())
                .build();
        ReflectionTestUtils.setField(service, "baseUrl", "https://local.example");
        ReflectionTestUtils.setField(service, "replicationUsername", "replication");
        ReflectionTestUtils.setField(service, "grantDefaultRead", "SELECT, EXECUTE");
        ReflectionTestUtils.setField(service, "grantDefaultWrite", "SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, EXECUTE");
        try (Connection root = DriverManager.getConnection(url, "root", password)) {
            root.createStatement().execute("DROP DATABASE IF EXISTS replica_access_test");
            root.createStatement().execute("DROP USER IF EXISTS replica_access_reader@'%'");
            root.createStatement().execute("CREATE DATABASE replica_access_test");
            root.createStatement().execute("CREATE TABLE replica_access_test.data (id INT PRIMARY KEY, value INT)");
            root.createStatement().execute("INSERT INTO replica_access_test.data VALUES (1, 10)");
            root.createStatement().execute("CREATE TABLE replica_access_test.qs_queries (query TEXT)");
            root.createStatement().execute("CREATE PROCEDURE replica_access_test.store_query() SQL SECURITY DEFINER "
                    + "INSERT INTO replica_access_test.qs_queries VALUES ('SELECT * FROM data')");
        }
    }

    @Test
    void replicaReaderCanSelectButCannotMutateOrCallDefinerProcedure() throws Exception {
        service.create(database, user, AccessTypeDto.READ);
        assertReadOnly();
        try (Connection root = DriverManager.getConnection(url + "/replica_access_test", "root", password)) {
            assertDoesNotThrow(() -> root.createStatement().execute("CALL store_query()"));
        }
    }

    @Test
    void existingDatabaseAndProcedureWriteGrantsAreRemovedAndRepairIsRepeatable() throws Exception {
        database.setCreationLocation(null);
        service.create(database, user, AccessTypeDto.WRITE_ALL);
        try (Connection reader = reader()) {
            reader.createStatement().execute("UPDATE data SET value = 11 WHERE id = 1");
            reader.createStatement().execute("CALL store_query()");
        }
        database.setCreationLocation("https://origin.example");
        service.update(database, user, AccessTypeDto.READ);
        assertReadOnly();
        service.update(database, user, AccessTypeDto.READ);
        assertReadOnly();
    }

    @Test
    void legacyDatabaseRemainsWritable() throws Exception {
        database.setCreationLocation(null);
        service.create(database, user, AccessTypeDto.WRITE_ALL);
        try (Connection reader = reader()) {
            assertEquals(1, reader.createStatement().executeUpdate("UPDATE data SET value = 12 WHERE id = 1"));
        }
    }

    @Test
    void physicalDeleteCannotDestroyHistoricalRows() throws Exception {
        try (Connection root = DriverManager.getConnection(url + "/replica_access_test", "root", password)) {
            root.createStatement().execute("ALTER TABLE data ADD SYSTEM VERSIONING");
            root.createStatement().execute("UPDATE data SET value = 20 WHERE id = 1");
            final var tables = new at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceMariaDbImpl(null, null, null, null, null);
            assertThrows(at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException.class,
                    () -> tables.delete(database, at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder().internalName("data").build()));
            try (var result = root.createStatement().executeQuery("SELECT COUNT(*) FROM data FOR SYSTEM_TIME ALL")) {
                assertTrue(result.next());
                assertEquals(2, result.getInt(1));
            }
        }
    }

    private Connection reader() throws SQLException {
        return DriverManager.getConnection(url + "/replica_access_test", user.getUsername(), user.getPassword());
    }

    private void assertReadOnly() throws SQLException {
        try (Connection reader = reader()) {
            assertTrue(reader.createStatement().executeQuery("SELECT * FROM data").next());
            for (String sql : new String[]{"INSERT INTO data VALUES (2, 20)", "UPDATE data SET value = 0",
                    "DELETE FROM data", "DROP TABLE data", "CREATE TABLE other (id INT)", "CALL store_query()"}) {
                assertThrows(SQLException.class, () -> reader.createStatement().execute(sql), sql);
            }
        }
    }
}
