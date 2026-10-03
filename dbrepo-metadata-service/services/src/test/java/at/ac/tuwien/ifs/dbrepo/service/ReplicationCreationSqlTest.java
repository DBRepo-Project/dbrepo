package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationCreation;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.annotations.JdbcTypeCode;
import org.junit.jupiter.api.*;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ReplicationCreationSqlTest {
    private final UUID parent = UUID.randomUUID();
    private final UUID remoteId = UUID.randomUUID();
    private final String origin = "https://origin.example";
    private final String schema = "ddl_retry_test_" + UUID.randomUUID().toString().replace("-", "");
    private EntityManagerFactory factory;
    private EntityManager entityManager;
    private TransactionTemplate transaction;
    private ReplicationServiceImpl service;
    private String rootUrl;

    @BeforeEach
    void setup() throws Exception {
        final String port = System.getenv("DDL_SQL_TEST_PORT");
        final String password = System.getenv("DDL_SQL_TEST_PASSWORD");
        rootUrl = port == null ? null : "jdbc:mariadb://127.0.0.1:" + port + "/";
        if (rootUrl != null) {
            try (var connection = DriverManager.getConnection(rootUrl, "root", password);
                 var statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE `" + schema + "`");
            }
        }
        factory = new Configuration().addAnnotatedClass(ReplicationCreation.class).addAnnotatedClass(Resource.class)
                .setProperty("hibernate.connection.provider_class",
                        "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl")
                .setProperty("hibernate.connection.url", rootUrl == null ? "jdbc:h2:mem:" + schema : rootUrl + schema)
                .setProperty("hibernate.connection.username", rootUrl == null ? "sa" : "root")
                .setProperty("hibernate.connection.password", rootUrl == null ? "" : password)
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory();
        entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        final var manager = new JpaTransactionManager(factory);
        transaction = new TransactionTemplate(manager);
        service = new ReplicationServiceImpl(null, null, null);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        ReflectionTestUtils.setField(service, "transactionManager", manager);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    @AfterEach
    void cleanup() throws Exception {
        if (factory != null) {
            factory.close();
        }
        if (rootUrl != null) {
            try (var connection = DriverManager.getConnection(rootUrl, "root", System.getenv("DDL_SQL_TEST_PASSWORD"));
                 var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE `" + schema + "`");
            }
        }
    }

    @Test
    void intentSurvivesMetadataRollbackAndResponseRetryKeepsTheSameIdentity() {
        final UUID expected = ReplicationCreation.localId("DATABASE", parent, origin, remoteId);
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            assertEquals(expected, reserve("DATABASE", null, Map.of("name", "measurements")));
            entityManager.persist(new Resource(expected));
            entityManager.flush();
            throw new IllegalStateException("injected metadata failure after successful DDL");
        }));
        transaction.executeWithoutResult(status -> {
            assertNotNull(entityManager.find(ReplicationCreation.class, expected));
            assertNull(service.findCreated(Resource.class, expected));
            assertEquals(expected, reserve("DATABASE", null, Map.of("name", "measurements")));
            entityManager.persist(new Resource(expected));
        });
        transaction.executeWithoutResult(status -> {
            assertEquals(expected, reserve("DATABASE", null, Map.of("name", "measurements")));
            assertNotNull(service.findCreated(Resource.class, expected));
        });
    }

    @Test
    void changedPayloadOrASecondIdentityCannotStealTheReservedTableName() {
        transaction.executeWithoutResult(status -> reserve("TABLE", "samples", Map.of("type", "INT")));
        assertThrows(IllegalArgumentException.class, () -> transaction.executeWithoutResult(status ->
                reserve("TABLE", "samples", Map.of("type", "BIGINT"))));
        assertThrows(RuntimeException.class, () -> transaction.executeWithoutResult(status ->
                service.reserveCreation("TABLE", parent, origin, UUID.randomUUID(), "samples", Map.of("type", "INT"))));
        assertEquals(ReplicationCreation.localId("TABLE", parent, origin, remoteId),
                transaction.execute(status -> reserve("TABLE", "samples", Map.of("type", "INT"))));
    }

    @Test
    void differentOriginsHaveDifferentIdentitiesAndDatabaseNames() {
        final UUID first = ReplicationCreation.localId("DATABASE", parent, origin, remoteId);
        final UUID second = ReplicationCreation.localId("DATABASE", parent, "https://other.example", remoteId);
        assertNotEquals(first, second);
        assertNotEquals(ReplicationCreation.databaseName(first), ReplicationCreation.databaseName(second));
        assertEquals(first, ReplicationCreation.localId("DATABASE", parent, origin + "/", remoteId));
    }

    @Test
    void duplicateCreationWaitsForTheFirstMetadataTransactionToFinish() throws Exception {
        final CountDownLatch locked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> transaction.execute(status -> {
                final UUID id = reserve("TABLE", "samples", Map.of("type", "INT"));
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test timed out");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                entityManager.persist(new Resource(id));
                return id;
            }));
            assertTrue(locked.await(10, TimeUnit.SECONDS));
            final var second = executor.submit(() -> transaction.execute(status -> {
                final UUID id = reserve("TABLE", "samples", Map.of("type", "INT"));
                assertNotNull(service.findCreated(Resource.class, id));
                return id;
            }));
            try {
                assertThrows(java.util.concurrent.TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
            } finally {
                release.countDown();
            }
            assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
    }

    private UUID reserve(String kind, String name, Object payload) {
        return service.reserveCreation(kind, parent, origin, remoteId, name, payload);
    }

    @Entity(name = "CreatedResource")
    @Table(name = "creation_test_resource")
    public static class Resource {
        @Id
        @JdbcTypeCode(java.sql.Types.VARCHAR)
        @Column(length = 36)
        UUID id;

        public Resource() {
        }

        Resource(UUID id) {
            this.id = id;
        }
    }
}
