package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.repository.mdb.*;
import at.tuwien.service.impl.QueueServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
@MockOpensearch
public class QueueServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private QueueServiceImpl queueService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    public void insert_succeeds() throws TableNotFoundException, DatabaseNotFoundException, InterruptedException,
            SQLException {
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
    public void insert_onlyMandatoryFields_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
            InterruptedException, SQLException {
        final Map<String, Object> request = new HashMap<>() {{
            put("id", 5L);
            put("date", "2023-10-04");
        }};

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        queueService.insert(DATABASE_1_INTERNALNAME, TABLE_1_INTERNALNAME, request);
    }

    @Test
    public void insert_databaseNotExists_fails() throws InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            queueService.insert("not_exists", TABLE_1_INTERNALNAME, new HashMap<>());
        });
    }

    @Test
    public void insert_tableNotExists_fails() throws InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            queueService.insert(DATABASE_1_INTERNALNAME, "not_exists", new HashMap<>());
        });
    }

}
