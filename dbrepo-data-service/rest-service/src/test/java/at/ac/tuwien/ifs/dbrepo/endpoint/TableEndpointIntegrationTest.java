package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.config.PostgresContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.TableEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import at.ac.tuwien.ifs.dbrepo.utils.S3Util;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
public class TableEndpointIntegrationTest extends BaseTest {

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Config s3Config;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private KeycloakGateway keycloakGateway;

    @MockitoBean
    private HttpServletRequest httpServletRequest;

    @Container
    private static PostgresContainerConfig.CustomPostgresContainer postgresContainer = PostgresContainerConfig.getContainer();

    @Container
    private static RedisContainerConfig.CustomRedisContainer redisContainer = RedisContainerConfig.getContainer();

    @Container
    private static final MinIOContainer minIOContainer = new MinIOContainer(MINIO_IMAGE);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.spark.hadoop.fs.s3a.endpoint", minIOContainer::getS3URL);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        /* s3 */
        S3Util.cleanBucket(s3Client, s3Config);
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @Test
    public void getData_succeeds() throws Exception {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, MediaType.APPLICATION_JSON_VALUE, httpServletRequest, USER_LOCAL_ADMIN_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<Map<String, Object>> body = new LinkedList<>((Set<Map<String, Object>>) response.getBody());
        assertNotNull(body);
        assertEquals(3, body.size());
        assertEquals(Map.of("id", 1, "date", "2008-12-01", "location", "Albury", "mintemp", 13.4, "rainfall", 0.6), body.get(0));
        assertEquals(Map.of("id", 2, "date", "2008-12-02", "location", "Albury", "mintemp", 7.4, "rainfall", 0.0), body.get(1));
        assertEquals(Map.of("id", 3, "date", "2008-12-03", "location", "Albury", "mintemp", 12.9, "rainfall", 0.0), body.get(2));
    }

}
