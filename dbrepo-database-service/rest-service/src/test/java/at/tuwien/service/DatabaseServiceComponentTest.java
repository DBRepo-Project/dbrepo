package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.repository.mdb.ContainerRepository;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.impl.MariaDbServiceImpl;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DatabaseServiceComponentTest extends BaseUnitTest {

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @Container
    @Autowired
    public MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
    }

    @Test
    public void create_elasticSearch_succeeds() throws Exception {

        /* mock */
        when(databaseIdxRepository.save(any(Database.class)))
                .thenReturn(DATABASE_3);

        /* test */
        generic_create(DATABASE_3_CREATE, DATABASE_3);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(DatabaseCreateDto createDto, Database database)
            throws Exception {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(containerRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.of(CONTAINER_1));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_3);

        /* test */
        final Database response = databaseService.create(createDto, USER_1_PRINCIPAL);
        assertEquals(database.getName(), response.getName());
    }

}
