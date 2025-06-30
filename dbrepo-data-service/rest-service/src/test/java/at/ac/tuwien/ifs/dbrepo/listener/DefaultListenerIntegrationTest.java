package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

import static at.ac.tuwien.ifs.dbrepo.utils.RabbitMqUtils.buildMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Testcontainers
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DefaultListenerIntegrationTest extends BaseTest {

    @MockitoBean
    private CacheService credentialService;

    @Autowired
    private DefaultListener defaultListener;

    @Container
    private static RabbitMQContainer rabbitContainer = new RabbitMQContainer(RABBITMQ_IMAGE);

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* database */
        MariaDbUtil.dropAllDatabases(CONTAINER_1_PRIVILEGED_DTO);
        MariaDbUtil.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
    }

    @Test
    public void onMessage_succeeds(CapturedOutput output) throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final Message request = buildMessage("dbrepo." + DATABASE_1_ID + "." + TABLE_1_ID, "{\"id\":4,\"date\":\"2023-10-03\",\"mintemp\":15.0,\"rainfall\":0.2}", new HashMap<>());

        /* mock */
        when(credentialService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("successfully inserted tuple"));
    }

}
