package at.tuwien.service;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class AccessServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private AccessService accessService;

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
    }

    @Test
    public void create_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.READ);
        final List<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        for (String privilege : grantDefaultRead.split(",")) {
            assertTrue(privileges.stream().anyMatch(p -> p.trim().equals(privilege.trim())));
        }
    }

    @Test
    public void create_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_OWN);
        final List<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        for (String privilege : grantDefaultWrite.split(",")) {
            assertTrue(privileges.stream().anyMatch(p -> p.trim().equals(privilege.trim())));
        }
    }

    @Test
    public void create_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_ALL);
        final List<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        for (String privilege : grantDefaultWrite.split(",")) {
            assertTrue(privileges.stream().anyMatch(p -> p.trim().equals(privilege.trim())));
        }
    }

    @Test
    public void update_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.READ);
        final List<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        for (String privilege : grantDefaultRead.split(",")) {
            assertTrue(privileges.stream().anyMatch(p -> p.trim().equals(privilege.trim())));
        }
    }

    @Test
    public void update_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_OWN);
        final List<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        for (String privilege : grantDefaultWrite.split(",")) {
            assertTrue(privileges.stream().anyMatch(p -> p.trim().equals(privilege.trim())));
        }
    }

    @Test
    public void update_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_ALL);
        final List<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        for (String privilege : grantDefaultWrite.split(",")) {
            assertTrue(privileges.stream().anyMatch(p -> p.trim().equals(privilege.trim())));
        }
    }

    @Test
    public void update_notFound_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            accessService.update(DATABASE_1_PRIVILEGED_DTO, USER_5_DTO, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void delete_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.delete(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO);
        final List<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        assertEquals(1, privileges.size());
        assertEquals("USAGE", privileges.get(0));
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            accessService.delete(DATABASE_1_PRIVILEGED_DTO, USER_5_DTO);
        });
    }

}
