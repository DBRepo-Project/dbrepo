package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@MockOpensearch
public class QueueServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private QueueService queueService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4, USER_5));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2));
        tableRepository.saveAll(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_4, TABLE_5, TABLE_6, TABLE_7));
        tableColumnRepository.saveAll(Stream.of(TABLE_1_COLUMNS, TABLE_2_COLUMNS, TABLE_3_COLUMNS, TABLE_4_COLUMNS, TABLE_5_COLUMNS, TABLE_6_COLUMNS, TABLE_7_COLUMNS).flatMap(List::stream).toList());
    }

    @Test
    public void insert_succeeds() throws TableNotFoundException, QueryMalformedException, DatabaseNotFoundException,
            InterruptedException {
        final Map<String, Object> request = new HashMap<>() {{
            put("id", 4L);
            put("date", "2023-10-03");
            put("location", "Albury");
            put("mintemp", 15.0);
            put("rainfall", 0.2);
        }};

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        queueService.insert(DATABASE_1_INTERNALNAME, TABLE_1_INTERNALNAME, request);
    }

    @Test
    public void insert_onlyMandatoryFields_succeeds() throws TableNotFoundException, QueryMalformedException,
            DatabaseNotFoundException, InterruptedException {
        final Map<String, Object> request = new HashMap<>() {{
            put("id", 5L);
            put("date", "2023-10-04");
        }};

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        queueService.insert(DATABASE_1_INTERNALNAME, TABLE_1_INTERNALNAME, request);
    }

}
