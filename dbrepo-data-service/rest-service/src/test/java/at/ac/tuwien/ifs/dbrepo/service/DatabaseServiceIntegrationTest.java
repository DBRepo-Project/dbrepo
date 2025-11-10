package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreCreateException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class DatabaseServiceIntegrationTest extends BaseTest {

    @Autowired
    private DatabaseService databaseService;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException, InterruptedException {
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_2_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_2_CACHE);
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @Test
    public void createQueryStore_succeeds() throws SQLException, QueryStoreCreateException {

        /* mock */
        MariaDbUtil.dropQueryStore(DATABASE_1_CACHE);

        /* test */
        databaseService.createQueryStore(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        final List<Map<String, Object>> queryStore = MariaDbUtil.listQueryStore(DATABASE_1_CACHE);
        assertEquals(0, queryStore.size());
    }

    @Test
    public void create_succeeds() throws SQLException, DatabaseMalformedException {

        /* mock */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);

        /* test */
        databaseService.create(CONTAINER_1_CACHE, DATABASE_1_CREATE_INTERNAL);
    }

    @Test
    public void create_exists_fails() {

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseService.create(CONTAINER_1_CACHE, DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void update_succeeds() throws SQLException, DatabaseMalformedException {
        final UpdateUserPasswordDto request = UpdateUserPasswordDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        MariaDbUtil.grantAccess(DATABASE_1_CACHE, grantDefaultWrite, USER_1_USERNAME);

        /* test */
        databaseService.update(DATABASE_1_CACHE, request);
        MariaDbUtil.mockQuery(CONTAINER_1_HOST, CONTAINER_1_PORT, DATABASE_1_INTERNAL_NAME, "CREATE SEQUENCE debug2 NOCACHE", USER_1_USERNAME, USER_2_PASSWORD);
    }

    @Test
    public void update_notExists_fails() {
        final UpdateUserPasswordDto request = UpdateUserPasswordDto.builder()
                .username("i_do_not_exist")
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseService.update(DATABASE_1_CACHE, request);
        });
    }

}
