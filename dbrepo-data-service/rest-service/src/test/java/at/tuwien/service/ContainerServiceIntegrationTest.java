package at.tuwien.service;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.QueryStoreCreateException;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class ContainerServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private ContainerService containerService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException, InterruptedException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @Test
    public void create_succeeds() throws SQLException, DatabaseMalformedException {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);

        /* test */
        final DatabaseDto response = containerService.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL);
        assertNull(response.getName());
        assertEquals(DATABASE_1_INTERNALNAME, response.getInternalName());
        assertEquals(EXCHANGE_DBREPO_NAME, response.getExchangeName());
        assertNotNull(response.getOwner());
        assertEquals(USER_1_ID, response.getOwner().getId());
        assertNotNull(response.getContact());
        assertEquals(USER_1_ID, response.getContact().getId());
        assertNotNull(response.getContainer());
        assertEquals(CONTAINER_1_ID, response.getContainer().getId());
    }

    @Test
    public void create_exists_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            containerService.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void createQueryStore_succeeds() throws SQLException, QueryStoreCreateException, InterruptedException {

        /* test */
        createQueryStore_generic(DATABASE_1_INTERNALNAME);
    }

    protected void createQueryStore_generic(String databaseName) throws SQLException, QueryStoreCreateException {

        /* test */
        containerService.createQueryStore(CONTAINER_1_PRIVILEGED_DTO, databaseName);
        final List<Map<String, Object>> response = MariaDbConfig.listQueryStore(DATABASE_1_PRIVILEGED_DTO);
        assertEquals(0, response.size());
    }
}
