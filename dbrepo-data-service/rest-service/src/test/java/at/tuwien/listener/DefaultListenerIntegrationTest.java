package at.tuwien.listener;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.DatabaseService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import static at.tuwien.utils.RabbitMqUtils.buildMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Testcontainers
@MockOpensearch
public class DefaultListenerIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseService databaseService;

    @Autowired
    private DefaultListener defaultListener;

    @Container
    private static RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3.10");

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        DATABASE_1.setTables(List.of(TABLE_1, TABLE_2, TABLE_3));
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    public void onMessage_succeeds(CapturedOutput output) throws DatabaseNotFoundException {
        final Message request = buildMessage("dbrepo." + DATABASE_1_INTERNALNAME + "." + TABLE_1_INTERNALNAME, "{\"id\":4,\"date\":\"2023-10-03\",\"mintemp\":15.0,\"rainfall\":0.2}", new HashMap<>());

        /* mock */
        when(databaseService.findByInternalName(DATABASE_1_INTERNALNAME))
                .thenReturn(DATABASE_1);

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("successfully inserted tuple"));
    }

}
