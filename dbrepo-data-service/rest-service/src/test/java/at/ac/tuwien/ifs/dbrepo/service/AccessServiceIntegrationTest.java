package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class AccessServiceIntegrationTest extends BaseTest {

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
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNAL_NAME);
        MariaDbConfig.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
    }

    @Test
    public void create_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.READ);
        final Set<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void create_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_OWN);
        final Set<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void create_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_ALL);
        final Set<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.READ);
        final Set<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_OWN);
        final Set<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO, AccessTypeDto.WRITE_ALL);
        final Set<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
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
        final Set<String> privileges = MariaDbConfig.getPrivileges(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);
        containsInAnyOrder(new String[]{"USAGE"}, privileges.toArray());
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            accessService.delete(DATABASE_1_PRIVILEGED_DTO, USER_5_DTO);
        });
    }

}
