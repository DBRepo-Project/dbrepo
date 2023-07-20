package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class DatabaseServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

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
    private DatabaseService databaseService;

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException {

        /* test */
        final List<DatabaseBriefDto> databases = databaseService.findAll(CONTAINER_1);
        assertEquals(1, databases.size());
        final DatabaseBriefDto database0 = databases.get(0);
        assertEquals(DATABASE_1_INTERNALNAME, database0.getInternalName());
    }

    @Test
    public void findAll_multipleOutOfSync_succeeds() throws DatabaseNotFoundException, SQLException {

        /* mock */
        MariaDbConfig.execute(CONTAINER_1, "CREATE DATABASE junit1;");
        MariaDbConfig.execute(CONTAINER_1, "CREATE DATABASE junit2;");
        MariaDbConfig.execute(CONTAINER_1, "CREATE DATABASE junit3;");

        /* test */
        final List<DatabaseBriefDto> databases = databaseService.findAll(CONTAINER_1);
        assertEquals(4, databases.size());
        final DatabaseBriefDto database0 = databases.get(0);
        assertEquals("junit1", database0.getInternalName());
        final DatabaseBriefDto database1 = databases.get(1);
        assertEquals("junit2", database1.getInternalName());
        final DatabaseBriefDto database2 = databases.get(2);
        assertEquals("junit3", database2.getInternalName());
        final DatabaseBriefDto database3 = databases.get(3);
        assertEquals(DATABASE_1_INTERNALNAME, database3.getInternalName());
    }

    @Test
    public void save_containerMissing_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            databaseService.save(DATABASE_2_DTO_BRIEF);
        });
    }

    @Test
    public void save_succeeds() throws ContainerNotFoundException {

        /* mock */
        containerRepository.save(CONTAINER_2);

        /* test */
        final Database response = databaseService.save(DATABASE_2_DTO_BRIEF);
        assertEquals(DATABASE_2_ID, response.getId());
        assertEquals(DATABASE_2_NAME, response.getName());
        assertEquals(DATABASE_2_INTERNALNAME, response.getInternalName());
        assertEquals(DATABASE_2_EXCHANGE, response.getExchangeName());
    }
}
