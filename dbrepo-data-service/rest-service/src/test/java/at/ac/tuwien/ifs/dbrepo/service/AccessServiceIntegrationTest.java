package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.PostgresContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class AccessServiceIntegrationTest extends BaseTest {

    @Autowired
    private AccessService accessService;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    @Container
    private static PostgresContainerConfig.CustomPostgresContainer postgresContainer = PostgresContainerConfig.getContainer();

    @Container
    private static RedisContainerConfig.CustomRedisContainer redisContainer = RedisContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
    }

    @Test
    public void create_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_CACHE, AccessTypeDto.READ, USER_1_USERNAME, USER_1_PASSWORD);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void create_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_CACHE, AccessTypeDto.WRITE_OWN, USER_1_USERNAME, USER_1_PASSWORD);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void create_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.create(DATABASE_1_CACHE, AccessTypeDto.WRITE_ALL, USER_1_USERNAME, USER_1_PASSWORD);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_read_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_CACHE, AccessTypeDto.READ, USER_1_USERNAME);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_writeOwn_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_CACHE, AccessTypeDto.WRITE_OWN, USER_1_USERNAME);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_writeAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.update(DATABASE_1_CACHE, AccessTypeDto.WRITE_ALL, USER_1_USERNAME);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).toArray(), privileges.toArray());
    }

    @Test
    public void update_notFound_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            accessService.update(DATABASE_1_CACHE, AccessTypeDto.WRITE_ALL, USER_5_USERNAME);
        });
    }

    @Test
    public void delete_succeeds() throws SQLException, DatabaseMalformedException {

        /* test */
        accessService.delete(DATABASE_1_CACHE, USER_1_USERNAME);
        final Set<String> privileges = MariaDbUtil.getPrivileges(DATABASE_1_CACHE, USER_1_USERNAME);
        containsInAnyOrder(new String[]{"USAGE"}, privileges.toArray());
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            accessService.delete(DATABASE_1_CACHE, USER_5_USERNAME);
        });
    }

}
