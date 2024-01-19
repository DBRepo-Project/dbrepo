package at.tuwien.listener;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.HashMap;

import static at.tuwien.utils.RabbitMqUtils.buildMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Testcontainers
@MockOpensearch
public class DefaultListenerUnitTest extends BaseUnitTest {

    @Autowired
    private DefaultListener defaultListener;

    @Container
    private static RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3.10");

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    public void onMessage_routingKeyDatabaseAndTableMissing_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo", "{}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to map database and table"));
    }

    @Test
    public void onMessage_routingKeyTableMissing_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo.database", "{}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to map database and table"));
    }

    @Test
    public void onMessage_messageMalformed_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo.database.table", "{,}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to read object"));
    }

    @Test
    public void onMessage_databaseNotFound_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo.database.table", "{\"id\":1}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to find database"));
    }

}
