package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.exception.*;
import at.tuwien.repository.sdb.ConceptIdxRepository;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.UnitIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
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
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;

@Log4j2
@Testcontainers
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private Channel channel;

    @MockBean
    private UnitIdxRepository unitIdxRepository;

    @MockBean
    private ConceptIdxRepository conceptIdxRepository;

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

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        imageRepository.save(IMAGE_1);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_hasRoleHasAccess_succeeds() throws UserNotFoundException, TableMalformedException, NotAllowedException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, AmqpException,
            TableNameExistsException, ContainerNotFoundException {

        /* mock */
        accessRepository.save(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        tableEndpoint.create(DATABASE_1_ID, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
    }
}
