package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.ViewCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.net.ServerSocket;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Native Grafana/Keycloak to real HTTP/JWT Data endpoint; authoritative Metadata remains a test seam. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DashboardCallerIntegrationTest.AuthorityFixture.class)
@Testcontainers
class DashboardCallerIntegrationTest extends BaseTest {
    @Container static MariaDBContainer<?> maria = MariaDbContainerConfig.getContainer();
    @Container static RedisContainerConfig.CustomRedisContainer redis = RedisContainerConfig.getContainer();
    private static final int KEYCLOAK_PORT = freePort(), GATEWAY_PORT = freePort();
    private static final String CONTROL_TOKEN = UUID.randomUUID().toString();
    private static final AtomicReference<String> STATE = new AtomicReference<>("private");
    @MockitoBean MetadataServiceGateway gateway;
    @Autowired DatabaseCacheRepository databases;
    @Autowired ViewCacheRepository views;
    @LocalServerPort int port;
    @TempDir Path temporary;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "https://127.0.0.1:" + GATEWAY_PORT + "/realms/dbrepo");
        properties.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + KEYCLOAK_PORT + "/realms/dbrepo/protocol/openid-connect/certs");
    }

    @Test
    void nativeCallerUsesCurrentPrivateAndPublicViewPermissions() throws Exception {
        databases.deleteAll();
        views.deleteAll();
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        try (var connection = DriverManager.getConnection("jdbc:mariadb://" + CONTAINER_1_CACHE.getHost() + ":" +
                CONTAINER_1_CACHE.getPort() + "/" + DATABASE_1_INTERNAL_NAME,
                CONTAINER_1_CACHE.getUsername(), CONTAINER_1_CACHE.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE OR REPLACE VIEW junit AS " + VIEW_1_QUERY);
        }
        STATE.set("private");
        // Keep the old grant in Redis throughout the HTTP exercise.
        DATABASE_1_CACHE.setAccesses(List.of(DatabaseAccess.builder().username(USER_2_USERNAME).type(AccessType.READ).build()));
        databases.save(DATABASE_1_CACHE);
        views.save(VIEW_1_CACHE);
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenAnswer(invocation -> currentSnapshot());
        Path repository = Path.of("").toAbsolutePath();
        while (!Files.isRegularFile(repository.resolve("dbrepo-dashboard-service/provision.py"))) {
            repository = repository.getParent();
            assertNotNull(repository, "Repository root not found");
        }
        Path log = temporary.resolve("dashboard-caller.log");
        ProcessBuilder builder = new ProcessBuilder(System.getenv().getOrDefault("DBREPO_TEST_PYTHON", "python3"),
                repository.resolve("dbrepo-dashboard-service/integration/native_oauth.py").toString());
        builder.environment().putAll(Map.of(
                "DBREPO_NATIVE_FIXTURE_DIR", temporary.toString(),
                "DBREPO_NATIVE_KEYCLOAK_PORT", String.valueOf(KEYCLOAK_PORT),
                "DBREPO_NATIVE_GATEWAY_PORT", String.valueOf(GATEWAY_PORT),
                "DBREPO_NATIVE_DATA_URL", "http://host.docker.internal:" + port,
                "DBREPO_NATIVE_DATA_PATH", "/api/v1/database/" + DATABASE_1_ID + "/view/" + VIEW_1_ID + "/data",
                "DBREPO_NATIVE_CONTROL_URL", "http://127.0.0.1:" + port + "/api/test/dashboard-authority",
                "DBREPO_NATIVE_CONTROL_TOKEN", CONTROL_TOKEN));
        Process process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(600, TimeUnit.SECONDS), "Native dashboard fixture timed out");
            assertEquals(0, process.exitValue(), () -> readLog(log));
            assertTrue(readLog(log).contains("\"current_permissions\": true"));
            verify(gateway, atLeast(5)).getDatabaseById(DATABASE_1_ID);
            assertTrue(databases.findById(DATABASE_1_ID).orElseThrow().getAccesses().stream()
                    .anyMatch(access -> USER_2_USERNAME.equals(access.getUsername())));
        } finally {
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(45, TimeUnit.SECONDS)) process.destroyForcibly();
            }
        }
    }

    private Database currentSnapshot() {
        return Database.builder().id(DATABASE_1_ID).internalName(DATABASE_1_INTERNAL_NAME)
                .container(CONTAINER_1_CACHE).ownedBy(USER_1_USERNAME).isPublic(false).isSchemaPublic(false)
                .accesses(STATE.get().equals("private") ? List.of(DatabaseAccess.builder()
                        .username(USER_2_USERNAME).type(AccessType.READ).build()) : List.of())
                .views(List.of(View.builder().id(VIEW_1_ID).databaseId(DATABASE_1_ID)
                        .internalName(VIEW_1_INTERNAL_NAME).query(VIEW_1_QUERY)
                        .isPublic(STATE.get().equals("public")).build())).build();
    }

    private static int freePort() {
        try (var socket = new ServerSocket(0)) { return socket.getLocalPort(); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    private static String readLog(Path log) {
        try { return Files.readString(log); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    @org.springframework.boot.test.context.TestComponent
    @RestController
    static class AuthorityFixture {
        @GetMapping("/api/test/dashboard-authority")
        String state(@RequestHeader("X-Fixture-Control") String token, @RequestParam String state) {
            if (!CONTROL_TOKEN.equals(token)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (!List.of("private", "public", "revoked").contains(state))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            STATE.set(state);
            return "ok";
        }
    }
}
