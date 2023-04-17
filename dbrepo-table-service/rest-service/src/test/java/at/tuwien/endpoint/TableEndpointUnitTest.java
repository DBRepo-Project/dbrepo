package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.DatabaseService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TableEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private TableIdxRepository tableidxRepository;

    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private TableEndpoint tableEndpoint;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        imageRepository.save(IMAGE_1_SIMPLE);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
    }

    @Test
    public void list_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableEndpoint.list(CONTAINER_1_ID, DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }
}
