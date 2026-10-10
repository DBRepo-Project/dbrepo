package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * Data responses are written on the task executor. When it is saturated, Spring rejects the response body before it
 * runs, so the query the endpoint already opened must be closed some other way. MockMvc does not dispatch the rejection
 * (Spring only post-processes interceptors that pre-processed the task), so this test completes the request itself.
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.task.execution.pool.core-size=1",
        "spring.task.execution.pool.max-size=1"
})
@Testcontainers
public class TableEndpointSaturationIntegrationTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private KeycloakGateway keycloakGateway;

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
    public void getData_executorSaturated_closesConnection() throws Exception {
        TABLE_1_CACHE.setIsPublic(true);

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        final long before = countConnections();
        final CountDownLatch release = new CountDownLatch(1);
        /* occupy the only thread, so every response body is rejected */
        applicationTaskExecutor.execute(() -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        /* test */
        try {
            for (String accept : List.of("application/json", "text/csv")) {
                final MvcResult result = mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID + "/table/" +
                                TABLE_1_ID + "/data")
                                .accept(accept))
                        .andExpect(request().asyncStarted())
                        .andReturn();
                /* the endpoint opened the query, the rejected body never runs */
                assertEquals(before + 1, awaitConnections(before + 1));
                /* the servlet container completes the async request after the error dispatch */
                result.getRequest().getAsyncContext().complete();
                assertEquals(before, awaitConnections(before));
            }
        } finally {
            release.countDown();
        }
    }

    /* the database drops a closed connection asynchronously */
    private static long awaitConnections(long expected) throws SQLException, InterruptedException {
        long count = countConnections();
        for (int i = 0; i < 50 && count != expected; i++) {
            Thread.sleep(100);
            count = countConnections();
        }
        return count;
    }

    private static long countConnections() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:mariadb://127.0.0.1:" + CONTAINER_1_PORT + "/" +
                DATABASE_1_INTERNAL_NAME, CONTAINER_1_PRIVILEGED_USERNAME, CONTAINER_1_PRIVILEGED_PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM information_schema.PROCESSLIST " +
                     "WHERE `DB` = 'weather' AND `ID` <> CONNECTION_ID()")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

}
