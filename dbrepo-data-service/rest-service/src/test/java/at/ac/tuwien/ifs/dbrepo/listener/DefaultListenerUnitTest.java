package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;

import static at.ac.tuwien.ifs.dbrepo.utils.RabbitMqUtils.buildMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

@Log4j2
@SpringBootTest
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Testcontainers
public class DefaultListenerUnitTest {

    @MockitoBean
    private CacheService credentialService;

    @Autowired
    private DefaultListener defaultListener;

    @Container
    private static RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3.10");

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

//    @BeforeEach
//    public void beforeEach() throws SQLException {
//        /* metadata database */
//        MariaDbConfig.dropAllDatabases(CONTAINER_1_PRIVILEGED_DTO);
//        MariaDbConfig.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
//    }

    @Test
    public void onMessage_routingKeyDatabaseAndTableMissing_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo", "{}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to map database and table"));
    }

    @Test
    public void onMessage_routingKeyTableMissing_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo.", "{}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to map database and table"));
    }

//    @Test
//    public void onMessage_messageMalformed_fails(CapturedOutput output) throws TableNotFoundException,
//            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException {
//        final Message request = buildMessage(TABLE_1_ROUTING_KEY, "{,}", new HashMap<>());
//
//        /* mock */
//        when(credentialService.getTable(DATABASE_1_ID, TABLE_1_ID))
//                .thenReturn(TABLE_1_DTO);
//        when(credentialService.getDatabase(DATABASE_1_ID))
//                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
//
//        /* test */
//        defaultListener.onMessage(request);
//        assertTrue(output.getAll().contains("Failed to read object"));
//    }
//
//    @Test
//    public void onMessage_tableNotFound_fails(CapturedOutput output) throws TableNotFoundException,
//            RemoteUnavailableException, MetadataServiceException {
//        final Message request = buildMessage(TABLE_1_ROUTING_KEY, "{\"id\": 1}", new HashMap<>());
//
//        /* mock */
//        doThrow(TableNotFoundException.class)
//                .when(credentialService)
//                .getTable(DATABASE_1_ID, TABLE_1_ID);
//
//        /* test */
//        defaultListener.onMessage(request);
//        assertTrue(output.getAll().contains("Failed to find table"));
//    }

}
