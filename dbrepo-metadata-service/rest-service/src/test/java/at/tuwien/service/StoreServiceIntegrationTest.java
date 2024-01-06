package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.*;
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

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class StoreServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreService storeService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setAccesses(List.of());
        databaseRepository.save(DATABASE_1);
        /* data stuff */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_3, USER_1_USERNAME);
    }

    @Test
    public void findAll_succeeds() throws ContainerNotFoundException, UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException {

        /* test */
        final List<Query> response = storeService.findAll(DATABASE_1_ID, null, USER_1_PRINCIPAL);
        assertEquals(2, response.size());
    }

    @Test
    public void findAll_onlyPersisted_succeeds() throws ContainerNotFoundException, UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException {

        /* test */
        final List<Query> response = storeService.findAll(DATABASE_1_ID, true, USER_1_PRINCIPAL);
        assertEquals(1, response.size());
    }

    @Test
    public void findAll_onlyNotPersisted_succeeds() throws ContainerNotFoundException, UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException {

        /* test */
        final List<Query> response = storeService.findAll(DATABASE_1_ID, false, USER_1_PRINCIPAL);
        assertEquals(1, response.size());
    }

    @Test
    public void findOne_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException {

        /* test */
        final Query response = storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
        assertNotNull(response);
    }

    @Test
    public void findOne_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void persist_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* precondition */
        final Query query3 = storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
        assertFalse(query3.getIsPersisted());

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, QUERY_1_ID, request);
        assertNotNull(response);
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unchanged_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(false) // <<<<<<<
                .build();

        /* precondition */
        final Query query3 = storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
        assertFalse(query3.getIsPersisted());

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, QUERY_1_ID, request);
        assertNotNull(response);
        assertFalse(response.getIsPersisted());
    }
}
