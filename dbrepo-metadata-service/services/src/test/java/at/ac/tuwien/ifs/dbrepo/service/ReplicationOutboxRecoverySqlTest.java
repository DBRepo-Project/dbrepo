package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.metadata.ReplicationNotificationOutboxRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationStatus;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationNotificationOutboxServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.Instant;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplicationOutboxRecoverySqlTest {

    @Test
    void durableDueQueryRecoversExhaustedOutageWithoutLosingFailedState() throws Exception {
        final String url = System.getenv("DBREPO_RECOVERY_TEST_JDBC_URL");
        if (url != null) {
            assertEquals("jdbc:mariadb://127.0.0.1:13366/recovery_replication_test", url,
                    "This test recreates its outbox table; use only the isolated recovery test database");
            try (var connection = DriverManager.getConnection("jdbc:mariadb://127.0.0.1:13366/", "root",
                    System.getenv("DBREPO_RECOVERY_TEST_PASSWORD")); var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS recovery_replication_test");
            }
        }
        final var configuration = new Configuration()
                .addAnnotatedClass(ReplicationNotificationOutbox.class)
                .setProperty("hibernate.connection.provider_class",
                        "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl")
                .setProperty("hibernate.connection.url", url == null ? "jdbc:h2:mem:recovery;DB_CLOSE_DELAY=-1" : url)
                .setProperty("hibernate.connection.username", url == null ? "sa" : "root")
                .setProperty("hibernate.connection.password", url == null ? "" : System.getenv("DBREPO_RECOVERY_TEST_PASSWORD"))
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = configuration.buildSessionFactory(); var entityManager = factory.createEntityManager()) {
            final var repository = new JpaRepositoryFactory(entityManager)
                    .getRepository(ReplicationNotificationOutboxRepository.class);
            final var service = new ReplicationNotificationOutboxServiceImpl(repository, new ObjectMapper());
            entityManager.getTransaction().begin();
            final var offline = service.enqueue(ReplicationNotificationType.TABLE_CREATE, HttpMethod.POST,
                    "/api/replication/table", Map.of("id", "retained"), UUID.randomUUID());
            final var invalid = service.enqueue(ReplicationNotificationType.TABLE_CREATE, HttpMethod.POST,
                    "/api/replication/table", Map.of("id", "invalid"), UUID.randomUUID());
            for (int i = 0; i < 25; i++) {
                service.markFailed(offline.getId(), "offline", Duration.ofMinutes(15), 20, true);
            }
            service.markFailed(invalid.getId(), "404", Duration.ofMinutes(15), 1, false);
            entityManager.getTransaction().commit();
            entityManager.clear();

            final var persisted = service.findById(offline.getId()).orElseThrow();
            assertEquals(ReplicationNotificationStatus.FAILED, persisted.getStatus());
            assertEquals(25, persisted.getAttempts());
            assertTrue(service.findDue(Instant.now(), 25).isEmpty());
            assertEquals(offline.getId(), service.findDue(Instant.now().plusSeconds(901), 1).getFirst().getId());
            assertNull(service.findById(invalid.getId()).orElseThrow().getNextAttemptAt());

            entityManager.getTransaction().begin();
            service.markSucceeded(offline.getId());
            service.markFailed(offline.getId(), "late error", Duration.ofMinutes(15), 20, true);
            entityManager.getTransaction().commit();
            entityManager.clear();
            assertTrue(service.findDue(Instant.now().plusSeconds(901), 25).isEmpty());
            assertEquals(2, service.findAll().size());
            assertEquals(ReplicationNotificationStatus.SUCCEEDED,
                    service.findById(offline.getId()).orElseThrow().getStatus());
            assertEquals("{\"id\":\"retained\"}", service.findById(offline.getId()).orElseThrow().getPayload());
        }
    }
}
