package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.service.DatabaseService;
import com.rabbitmq.client.Channel;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.ws.rs.NotAllowedException;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TableEndpointUnitTest extends BaseUnitTest {

    /**
     * RabbitMQ not required in this test
     */
    @MockBean
    private ReadyConfig readyConfig;

    /**
     * RabbitMQ not required in this test
     */
    @MockBean
    private Channel channel;

    /**
     * ElasticSearch not required in this test
     */
    @MockBean
    private IndexInitializer indexInitializer;

    /**
     * ElasticSearch not required in this test
     */
    @MockBean
    private TableIdxRepository tableidxRepository;

    /**
     * ElasticSearch not required in this test
     */
    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @MockBean
    private DatabaseService databaseService;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Test
    @Disabled
    public void list_databaseNotFound_fails() throws DatabaseNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseService.find(CONTAINER_1_ID, DATABASE_1_ID))
                .thenThrow(DatabaseNotFoundException.class);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.list(CONTAINER_1_ID, DATABASE_1_ID, principal);
        });
    }
}
