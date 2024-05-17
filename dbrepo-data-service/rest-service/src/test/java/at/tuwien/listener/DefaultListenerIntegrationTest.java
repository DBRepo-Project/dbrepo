package at.tuwien.listener;

import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.HashMap;

import static at.tuwien.utils.RabbitMqUtils.buildMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Testcontainers
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DefaultListenerIntegrationTest extends AbstractUnitTest {

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @Autowired
    private DefaultListener defaultListener;

    @Container
    private static RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3.10");

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* database */
        MariaDbConfig.dropAllDatabases(CONTAINER_1_PRIVILEGED_DTO);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
    }

    @Test
    public void onMessage_succeeds(CapturedOutput output) throws TableNotFoundException, RemoteUnavailableException {
        final Message request = buildMessage("dbrepo." + DATABASE_1_ID + "." + TABLE_1_ID, "{\"id\":4,\"date\":\"2023-10-03\",\"mintemp\":15.0,\"rainfall\":0.2}", new HashMap<>());

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("successfully inserted tuple"));
    }

    @Test
    @Disabled
    public void onMessage_tableNotFound_fails(CapturedOutput output) throws TableNotFoundException, RemoteUnavailableException {
        final Message request = buildMessage("dbrepo." + DATABASE_1_ID + "." + TABLE_1_ID, "{\"id\":4,\"date\":\"2023-10-03\",\"mintemp\":15.0,\"rainfall\":0.2}", new HashMap<>());

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_1_ID, TABLE_1_ID);

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to insert tuple"));
    }

}
