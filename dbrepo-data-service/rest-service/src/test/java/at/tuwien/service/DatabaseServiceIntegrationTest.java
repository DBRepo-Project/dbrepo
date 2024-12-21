package at.tuwien.service;

import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.DatabaseMalformedException;
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

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class DatabaseServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private DatabaseService databaseService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
    }

    @Test
    public void create_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        final PrivilegedDatabaseDto response = databaseService.create(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL);
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
    public void create_exists_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseService.create(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void update_succeeds() throws SQLException, DatabaseMalformedException {
        final UpdateUserPasswordDto request = UpdateUserPasswordDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
        MariaDbConfig.grantWriteAccess(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);

        /* pre-condition */
        MariaDbConfig.mockQuery(CONTAINER_1_HOST, CONTAINER_1_PORT, DATABASE_1_INTERNALNAME, "CREATE SEQUENCE debug NOCACHE", USER_1_USERNAME, USER_1_PASSWORD);
        try {
            MariaDbConfig.mockQuery(CONTAINER_1_HOST, CONTAINER_1_PORT, DATABASE_1_INTERNALNAME, "CREATE SEQUENCE debug NOCACHE", USER_1_USERNAME, USER_2_PASSWORD);
            fail();
        } catch (SQLException e) {
            /* ignore */
        }

        /* test */
        databaseService.update(DATABASE_1_PRIVILEGED_DTO, request);
        MariaDbConfig.mockQuery(CONTAINER_1_HOST, CONTAINER_1_PORT, DATABASE_1_INTERNALNAME, "CREATE SEQUENCE debug2 NOCACHE", USER_1_USERNAME, USER_2_PASSWORD);
    }

    @Test
    public void update_notExists_fails() throws SQLException {
        final UpdateUserPasswordDto request = UpdateUserPasswordDto.builder()
                .username("i_do_not_exist")
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseService.update(DATABASE_1_PRIVILEGED_DTO, request);
        });
    }

}
