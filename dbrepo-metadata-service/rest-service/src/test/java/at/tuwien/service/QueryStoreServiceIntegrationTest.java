package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import at.tuwien.service.impl.HibernateConnector;
import at.tuwien.service.impl.QueryStoreServiceImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Log4j2
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class QueryStoreServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private QueryStoreServiceImpl queryStoreService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.save(USER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, DatabaseMalformedException, SQLException {

        /* setup */
        MariaDbConfig.createDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);

        /* test */
        queryStoreService.create(DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void executeQuery_succeeds() throws SQLException {
        final ComboPooledDataSource dataSource = HibernateConnector.getPrivilegedDataSource(CONTAINER_1_IMAGE, CONTAINER_1, DATABASE_1);

        /* setup */
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);

        /* test */
        try {
            final Connection connection = dataSource.getConnection();
            queryStoreService.executeQuery(connection, "UPDATE weather_location SET lat=48.2049358, lng=16.3769348 WHERE location = ?", "Vienna");
        } finally {
            dataSource.close();
        }
    }
}
