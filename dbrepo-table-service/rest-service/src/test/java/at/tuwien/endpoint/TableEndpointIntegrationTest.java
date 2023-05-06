package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.*;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableEndpointIntegrationTest extends BaseUnitTest {

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
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private DatabaseAccessRepository accessRepository;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private H2Utils h2Utils;

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* create network */
        DockerConfig.createAllNetworks();
        /* metadata database */
        h2Utils.runScript("schema.sql");
        imageRepository.save(IMAGE_1);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_hasRoleHasAccess_succeeds() throws UserNotFoundException, TableMalformedException, NotAllowedException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, AmqpException,
            TableNameExistsException, ContainerNotFoundException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        accessRepository.save(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        tableEndpoint.create(CONTAINER_1_ID, DATABASE_1_ID, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
    }
}
