package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@Testcontainers
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class ViewServicePersistenceIntegrationTest extends BaseUnitTest {

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private ViewService viewService;

    @Autowired
    private UserRepository userRepository;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2));
        containerRepository.saveAll(List.of(CONTAINER_1_SIMPLE, CONTAINER_2_SIMPLE));
        databaseRepository.saveAll(List.of(DATABASE_1_SIMPLE, DATABASE_2_SIMPLE));
        tableRepository.saveAll(List.of(TABLE_1_SIMPLE, TABLE_2_SIMPLE, TABLE_3_SIMPLE, TABLE_4_SIMPLE, TABLE_5_SIMPLE, TABLE_6_SIMPLE, TABLE_7_SIMPLE));
        tableColumnRepository.saveAll(TABLE_1_COLUMNS);
        tableColumnRepository.saveAll(TABLE_2_COLUMNS);
        tableColumnRepository.saveAll(TABLE_3_COLUMNS);
        tableColumnRepository.saveAll(TABLE_4_COLUMNS);
        tableColumnRepository.saveAll(TABLE_5_COLUMNS);
        tableColumnRepository.saveAll(TABLE_6_COLUMNS);
        tableColumnRepository.saveAll(TABLE_7_COLUMNS);
        viewRepository.saveAll(List.of(VIEW_1, VIEW_2, VIEW_3, VIEW_4));
    }

    @Test
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            DatabaseConnectionException, ViewMalformedException, QueryMalformedException {
        final String query = "select id from weather_aus";
        final ViewCreateDto request = ViewCreateDto.builder()
                .name("Debug")
                .query(query)
                .isPublic(true)
                .build();

        /* test */
        final View response = viewService.create(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals("Debug", response.getName());
        assertEquals("debug", response.getInternalName());
        assertEquals(query, response.getQuery());
        assertEquals(1, response.getColumns().size());
    }

    @Test
    @Transactional
    public void findById_succeeds() throws UserNotFoundException, ViewNotFoundException {

        /* mock */
        tableRepository.save(TABLE_2_SIMPLE);
        tableColumnRepository.saveAll(TABLE_2_COLUMNS);
        viewRepository.save(VIEW_1);

        /* test */
        final View response = viewService.findById(DATABASE_1_ID, VIEW_1_ID, USER_1_PRINCIPAL);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        assertEquals(VIEW_1_COLUMNS.size(), response.getColumns().size());
    }

}
