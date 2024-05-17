package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.impl.QueueServiceRabbitMqImpl;
import lombok.extern.log4j.Log4j2;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class QueueServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private QueueServiceRabbitMqImpl queueService;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
    }

    @Test
    public void insert_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException, ContainerNotFoundException, TableNotFoundException {
        final Map<String, Object> request = new HashMap<>() {{
            put("id", 4L);
            put("date", "2023-10-03");
            put("location", "Albury");
            put("mintemp", 15.0);
            put("rainfall", 0.2);
        }};

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        queueService.insert(TABLE_1_PRIVILEGED_DTO, request);
    }

    @Test
    public void insert_onlyMandatoryFields_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException, TableNotFoundException {
        final Map<String, Object> request = new HashMap<>() {{
            put("id", 5L);
            put("date", "2023-10-04");
        }};

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        queueService.insert(TABLE_1_PRIVILEGED_DTO, request);
    }

}
