package at.ac.tuwien.ac.at.ifs.dbrepo.service;

import at.ac.tuwien.ac.at.ifs.dbrepo.config.MariaDbConfig;
import at.ac.tuwien.ac.at.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ac.at.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ac.at.ifs.dbrepo.service.impl.QueueServiceRabbitMqImpl;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class QueueServiceIntegrationTest extends BaseTest {

    @Autowired
    private QueueServiceRabbitMqImpl queueService;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNAL_NAME);
        MariaDbConfig.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
    }

    @Test
    public void insert_succeeds() throws SQLException, RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, DatabaseNotFoundException {
        final Map<String, Object> request = new HashMap<>() {{
            put("id", 4L);
            put("date", "2023-10-03");
            put("location", "Albury");
            put("mintemp", 15.0);
            put("rainfall", 0.2);
        }};

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        queueService.insert(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
    }

    @Test
    public void insert_onlyMandatoryFields_succeeds() throws SQLException, RemoteUnavailableException,
            TableNotFoundException, MetadataServiceException, DatabaseNotFoundException {
        final Map<String, Object> request = new HashMap<>() {{
            put("id", 5L);
            put("date", "2023-10-04");
        }};

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        queueService.insert(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
    }

}
