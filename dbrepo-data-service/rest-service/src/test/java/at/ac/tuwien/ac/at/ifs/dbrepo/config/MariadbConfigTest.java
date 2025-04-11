package at.ac.tuwien.ac.at.ifs.dbrepo.config;

import at.ac.tuwien.ac.at.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
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
import java.sql.SQLTimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class MariadbConfigTest extends BaseTest {

    @Autowired
    private DatabaseService databaseService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException, InterruptedException {
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNAL_NAME);
        MariaDbConfig.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @Test
    public void longRunningQuery_succeeds() throws SQLException {

        /* test */
        MariaDbConfig.execute(DATABASE_1_PRIVILEGED_DTO, "SELECT SLEEP(8);"); // -2
    }

    @Test
    public void tooLongRunningQuery_succeeds() {

        /* test */
        assertThrows(SQLTimeoutException.class, () -> {
            MariaDbConfig.execute(DATABASE_1_PRIVILEGED_DTO, "SELECT SLEEP(12);"); // +2
        });
    }

}
