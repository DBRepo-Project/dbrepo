package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationCreation;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreCreateException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "DDL_SQL_TEST_PORT", matches = "[0-9]+")
class ReplicaDdlMariaDbTest {
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("DDL_SQL_TEST_PORT") + "/";
    private final String password = System.getenv("DDL_SQL_TEST_PASSWORD");
    private final UUID parentId = UUID.randomUUID();
    private final UUID creationId = UUID.randomUUID();
    private final String origin = "https://origin.example";
    private final UUID localId = ReplicationCreation.localId("DATABASE", parentId, origin, creationId);
    private final String schema = ReplicationCreation.databaseName(localId);

    @AfterEach
    void cleanup() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + schema + "`");
        }
    }

    @Test
    void partialQueryStoreInitializationAndLostResponseResumeWithoutDroppingData() throws Exception {
        final MariaDbMapper mapper = Mappers.getMapper(MariaDbMapper.class);
        final MariaDbMapper failing = spy(mapper);
        when(failing.queryStoreCreateStoreQueryProcedureRawQuery())
                .thenReturn("SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'injected initialization failure'");
        final var first = service(failing);
        first.create(container(), request());
        assertThrows(QueryStoreCreateException.class, () -> first.createQueryStore(container(), schema));
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE `" + schema + "`.preserved (id INT)");
            statement.execute("INSERT INTO `" + schema + "`.preserved VALUES (7)");
            connection.commit();
        }

        final var retry = service(mapper);
        for (int i = 0; i < 2; i++) {
            retry.create(container(), request());
            retry.createQueryStore(container(), schema);
        }
        assertEquals(4, count("information_schema.routines WHERE routine_schema = '" + schema + "'"));
        assertEquals(1, count("`" + schema + "`.preserved WHERE id = 7"));
        assertEquals(1, count("`" + schema + "`.dbrepo_replication_ddl WHERE object_name = ''"));
    }

    @Test
    void cannotAdoptAnUnrelatedDatabaseOrChangeItsIdentity() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "`");
            statement.execute("CREATE TABLE `" + schema + "`.unrelated (id INT)");
        }
        assertThrows(DatabaseMalformedException.class,
                () -> service(Mappers.getMapper(MariaDbMapper.class)).create(container(), request()));
        assertEquals(1, count("information_schema.tables WHERE table_schema = '" + schema + "'"));
    }

    @Test
    void resumesAnEmptyFixedNameShellAfterDatabaseDdlButBeforeReceipt() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "`");
        }
        final var service = service(Mappers.getMapper(MariaDbMapper.class));
        service.create(container(), request());
        service.create(container(), request());
        assertEquals(1, count("`" + schema + "`.dbrepo_replication_ddl WHERE object_name = ''"));
        final var differentIdentity = request();
        differentIdentity.setReplicaUrls(Map.of(origin, UUID.randomUUID()));
        assertThrows(DatabaseMalformedException.class, () -> service.create(container(), differentIdentity));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void receiptSurvivesFailureImmediatelyBeforeOrAfterTableDdl(boolean afterDdl) throws Exception {
        final String sql = "CREATE TABLE `" + schema + "`.samples (id INT PRIMARY KEY) ENGINE=InnoDB WITH SYSTEM VERSIONING";
        try (var connection = connect()) {
            ReplicaDdl.createDatabase(connection, schema, localId.toString());
            final Connection failed = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        try {
                            final Object result = method.invoke(connection, args);
                            if (method.getName().equals("prepareStatement") && sql.equals(args[0])) {
                                return Proxy.newProxyInstance(getClass().getClassLoader(),
                                        new Class<?>[]{PreparedStatement.class}, (p, m, a) -> {
                                            if (m.getName().equals("execute")) {
                                                if (afterDdl) {
                                                    ((PreparedStatement) result).execute();
                                                }
                                                throw new SQLException("injected lost connection/response");
                                            }
                                            try {
                                                return m.invoke(result, a);
                                            } catch (InvocationTargetException e) {
                                                throw e.getCause();
                                            }
                                        });
                            }
                            return result;
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
            assertThrows(SQLException.class, () -> ReplicaDdl.createTable(failed, schema, "samples", sql));
            connection.rollback();
        }
        try (var retry = connect(); var statement = retry.createStatement()) {
            ReplicaDdl.createTable(retry, schema, "samples", sql);
            statement.execute("INSERT INTO `" + schema + "`.samples VALUES (9)");
            retry.commit();
            ReplicaDdl.createTable(retry, schema, "samples", sql);
            assertThrows(SQLException.class, () -> ReplicaDdl.createTable(retry, schema, "samples",
                    sql.replace("id INT", "id BIGINT")));
        }
        assertEquals(1, count("`" + schema + "`.samples WHERE id = 9"));
        assertEquals(1, count("`" + schema + "`.samples FOR SYSTEM_TIME ALL"));
    }

    @Test
    void existingTableWithoutReceiptIsNotAdopted() throws Exception {
        final String sql = "CREATE TABLE `" + schema + "`.samples (id INT)";
        try (var connection = connect(); var statement = connection.createStatement()) {
            ReplicaDdl.createDatabase(connection, schema, localId.toString());
            statement.execute(sql);
            assertThrows(SQLException.class, () -> ReplicaDdl.createTable(connection, schema, "samples", sql));
        }
        assertEquals(0, count("`" + schema + "`.dbrepo_replication_ddl WHERE object_name = 'samples'"));
    }

    @Test
    void concurrentRetriesCreateOneTable() throws Exception {
        try (var connection = connect()) {
            ReplicaDdl.createDatabase(connection, schema, localId.toString());
        }
        try (var executor = Executors.newFixedThreadPool(2)) {
            final java.util.concurrent.Callable<Void> create = () -> {
                try (var connection = connect()) {
                    ReplicaDdl.createTable(connection, schema, "samples",
                            "CREATE TABLE `" + schema + "`.samples (id INT)");
                }
                return null;
            };
            final var first = executor.submit(create);
            final var second = executor.submit(create);
            first.get(45, TimeUnit.SECONDS);
            second.get(45, TimeUnit.SECONDS);
        }
        assertEquals(1, count("`" + schema + "`.dbrepo_replication_ddl WHERE object_name = 'samples'"));
    }

    private Connection connect() throws SQLException {
        final Connection connection = DriverManager.getConnection(url, "root", password);
        connection.setAutoCommit(false);
        return connection;
    }

    private long count(String relation) throws SQLException {
        try (var connection = connect(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM " + relation)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private Container container() {
        return Container.builder().id(parentId).host("127.0.0.1")
                .port(Integer.valueOf(System.getenv("DDL_SQL_TEST_PORT"))).username("root").password(password)
                .image(Image.builder().jdbcMethod("mariadb").build()).build();
    }

    private CreateDatabaseDto request() {
        return CreateDatabaseDto.builder().containerId(parentId).internalName(schema).creationLocation(origin)
                .replicaUrls(Map.of(origin, creationId)).build();
    }

    private DatabaseServiceMariaDbImpl service(MariaDbMapper mapper) {
        final var service = new DatabaseServiceMariaDbImpl(mapper);
        ReflectionTestUtils.setField(service, "baseUrl", "https://replica.example");
        return service;
    }
}
