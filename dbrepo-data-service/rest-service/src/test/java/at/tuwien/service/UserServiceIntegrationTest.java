package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class UserServiceIntegrationTest extends BaseUnitTest {

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
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private UserService userService;

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
        tableRepository.save(TABLE_1);
        tableRepository.save(TABLE_2);
        tableColumnRepository.saveAll(TABLE_1_COLUMNS);
        tableColumnRepository.saveAll(TABLE_2_COLUMNS);
    }

    @Test
    public void findAll_succeeds() throws UserNotFoundException {

        /* test */
        final List<UserBriefDto> response = userService.findAll(DATABASE_1);
        final List<String> expected = List.of("healthcheck", "junit1", "junit2", "junit3", "junit4", "root", "test");
        assertEquals(expected.size(), response.size());
        for (int i = 0; i < response.size(); i++) {
            final UserBriefDto user = response.get(i);
            assertEquals(expected.get(i), user.getUsername());
            assertNull(user.getEmailVerified());
        }
    }

    @Test
    public void save_succeeds() {

        /* test */
        final User response = userService.save(USER_1_BRIEF_DTO);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertNull(response.getEmail());
        assertFalse(response.getEnabled());
        assertFalse(response.getEmailVerified());
    }
}
