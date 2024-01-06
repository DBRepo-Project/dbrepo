package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.DatabaseRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.Principal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude= RabbitAutoConfiguration.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class StoreServiceIntegrationReadTest extends BaseUnitTest {

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private StoreService storeService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws SQLException {
        /* insert query */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);
    }

    @Test
    public void findAll_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, ContainerNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final List<Query> queries = storeService.findAll(DATABASE_1_ID, null, USER_1_PRINCIPAL);
        assertEquals(1, queries.size());
    }

    @Test
    public void findAll_filterPersisted_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, ContainerNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final List<Query> queries = storeService.findAll(DATABASE_1_ID, true, USER_1_PRINCIPAL);
        assertEquals(0, queries.size());
    }

    @Test
    public void findOne_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void findOne_notFound_succeeds() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_notFound_fails() {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, QUERY_2_ID, principal);
        });
    }

}
