package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.impl.HibernateConnector;
import at.tuwien.service.impl.QueryStoreServiceImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class QueryStoreServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private QueryStoreServiceImpl queryStoreService;

    @Autowired
    private DatabaseMapper databaseMapper;

    private final static String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* create network */
        DockerConfig.createAllNetworks();
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, DatabaseMalformedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);

        /* test */
        queryStoreService.create(CONTAINER_1_ID, DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void executeQuery_succeeds() throws SQLException, InterruptedException {
        final User root = databaseMapper.containerToPrivilegedUser(CONTAINER_1);
        final ComboPooledDataSource dataSource = HibernateConnector.getDataSource(CONTAINER_1_IMAGE, CONTAINER_1, DATABASE_1, root);

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);

        /* test */
        try {
            final Connection connection = dataSource.getConnection();
            queryStoreService.executeQuery(connection, "UPDATE weather_location SET lat=48.2049358, lng=16.3769348 WHERE location = ?", "Vienna");
        } finally {
            dataSource.close();
        }
    }
}
